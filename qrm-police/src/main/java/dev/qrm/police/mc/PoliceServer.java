package dev.qrm.police.mc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.data.ModDataService;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.permission.PermissionService;
import dev.qrm.police.logic.Agente;
import dev.qrm.police.logic.CallType;
import dev.qrm.police.logic.Citizen;
import dev.qrm.police.logic.DispatchCall;
import dev.qrm.police.logic.FiltroStorico;
import dev.qrm.police.logic.FineCatalog;
import dev.qrm.police.logic.FineStatus;
import dev.qrm.police.logic.PoliceDatabase;
import dev.qrm.police.logic.RecordKind;
import dev.qrm.police.logic.StatoAgente;
import dev.qrm.police.logic.StatoChiamata;
import dev.qrm.police.logic.Unita;
import dev.qrm.police.net.PolicePayloads;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Il database di polizia sul server e tutto ciò che lo cambia. Ogni azione ricontrolla accesso e argomenti. */
public final class PoliceServer {
    public static final String NODE_USE = "police.mdt.use";
    /** Chi può scrivere la bacheca del dipartimento (oltre agli operatori). */
    public static final String NODE_BOARD = "police.mdt.board";
    /** Chi coordina il dispatch: crea e assegna chiamate, cambia le priorità, chiude e annulla quelle degli altri. */
    public static final String NODE_OPERATE = "police.dispatch.operate";
    private static final String NAMESPACE = "qrm_police";
    private static final String KEY = "database";
    private static final int SAVE_EVERY = 100;
    private static final int MAX_RECORDS_SENT = 40;
    private static final int MAX_CALLS = 50;
    private static final int BEEP_GAP = 5;
    private static final int UNITS_CHECK_EVERY = 40;
    private static final int PANIC_COOLDOWN_TICKS = 200;
    private static final int MAX_BOARD = 120;
    private static final int MAX_DESC = 120;
    private static final int MAX_HISTORY = 40;
    private static final int REPLY_GAP_TICKS = 10;

    private record Beep(UUID player, int atTick) {}

    private static PoliceDatabase db;
    private static boolean dirty;
    private static final Map<UUID, Integer> SUPPORT_AT = new HashMap<>();
    private static final int SUPPORT_COOLDOWN_TICKS = 200;
    private static final Map<UUID, StatoAgente> STATUS = new HashMap<>();
    private static final Map<UUID, Integer> PANIC_AT = new HashMap<>();
    private static final Map<String, Integer> REPLY_AT = new HashMap<>();
    private static String unitsSeen = "";
    private static final Set<UUID> VIEWERS = new HashSet<>();
    private static final List<Beep> BEEPS = new ArrayList<>();
    private static final Random RANDOM = new Random();

    private PoliceServer() {}

    // ---- database ----

    public static synchronized PoliceDatabase db() {
        if (db == null) {
            db = QRM.get(ModDataService.class).get(NAMESPACE, OwnerRef.system(), KEY)
                    .map(PoliceDatabase::fromJson).orElseGet(PoliceDatabase::new);
        }
        return db;
    }

    /** Segna i dati da salvare e avvisa chi ha l'MDT aperto. */
    public static void changed(MinecraftServer server) {
        dirty = true;
        for (UUID id : new ArrayList<>(VIEWERS)) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null || !mayUse(p)) VIEWERS.remove(id);
            else sendSnapshot(p, false);
        }
    }

    public static synchronized void flush() {
        if (db == null || !dirty) return;
        QRM.get(ModDataService.class).put(NAMESPACE, OwnerRef.system(), KEY, db.toJson());
        dirty = false;
    }

    public static synchronized void reset() {
        flush();
        db = null;
        dirty = false;
        SUPPORT_AT.clear();
        STATUS.clear();
        PANIC_AT.clear();
        REPLY_AT.clear();
        unitsSeen = "";
        VIEWERS.clear();
        BEEPS.clear();
    }

    public static void forget(UUID player) {
        SUPPORT_AT.remove(player);
        PANIC_AT.remove(player);
        REPLY_AT.remove(player + "units");
        REPLY_AT.remove(player + "history");
        STATUS.remove(player);
        VIEWERS.remove(player);
        releaseOnDisconnect(player);
    }

    /** Chi cade mentre ha una chiamata in carico la lascia: torna in attesa, con una riga in cronologia. */
    private static void releaseOnDisconnect(UUID player) {
        if (db == null) return;
        var open = db.activeCallOf(player.toString());
        if (open.isEmpty()) return;
        DispatchCall call = open.get();
        db.releaseCall(call.id, Agente.SYSTEM, call.assignee + " si è disconnesso: la chiamata torna in attesa",
                System.currentTimeMillis());
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) changed(server);
    }

    // ---- accesso e identità ----

    /** Chi può usare l'MDT: gli operatori, chi ha il nodo del personaggio, e chiunque in singleplayer. */
    public static boolean mayUse(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        if (server != null && server.isSingleplayer()) return true;
        if (p.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return true;
        return QRM.get(CharacterService.class).activeOf(new PlayerId(p.getUUID()))
                .map(c -> QRM.get(PermissionService.class).has(c.id(), NODE_USE)).orElse(false);
    }

    public static String officerName(ServerPlayer p) {
        return QRM.get(CharacterService.class).activeOf(new PlayerId(p.getUUID()))
                .map(c -> c.firstName() + " " + c.lastName()).orElse(p.getGameProfile().name());
    }

    /** Il profilo del giocatore nel database: della Character attiva se c'è, altrimenti del giocatore. */
    public static Citizen selfCitizen(ServerPlayer p) {
        Optional<PlayerCharacter> c = QRM.get(CharacterService.class).activeOf(new PlayerId(p.getUUID()));
        if (c.isPresent()) {
            return register(c.get());
        }
        String name = p.getGameProfile().name();
        Citizen self = db().ensureCitizen(p.getUUID(), name, "(giocatore)", "—");
        self.playerId = p.getUUID().toString();
        return self;
    }

    /** La scheda del personaggio, creata se manca; ricorda il giocatore proprietario per mostrarne la skin. */
    public static synchronized Citizen register(PlayerCharacter pc) {
        Citizen c = db().ensureCitizen(pc.id().value(), pc.firstName(), pc.lastName(), pc.birthDate().toString());
        String owner = pc.playerId().value().toString();
        if (!owner.equals(c.playerId)) {
            c.playerId = owner;
            dirty = true;
        }
        return c;
    }

    // ---- apertura e stato ----

    /** Allinea l'anagrafe ai personaggi del server: chi manca viene aggiunto, le schede esistenti non cambiano. */
    public static void syncCitizens() {
        PoliceDatabase d = db();
        for (PlayerCharacter pc : QRM.get(CharacterService.class).listAll()) {
            if (d.citizen(pc.id().value()).isEmpty()) dirty = true;
            register(pc);
        }
    }

    public static void open(ServerPlayer p) {
        if (!mayUse(p)) {
            p.sendSystemMessage(Component.translatable("qrm_police.denied"), true);
            return;
        }
        syncCitizens();
        VIEWERS.add(p.getUUID());
        sendSnapshot(p, true);
    }

    private static void sendSnapshot(ServerPlayer p, boolean open) {
        MinecraftServer server = p.level().getServer();
        JsonArray units = unitsJson(server);
        String json = db().toJson(MAX_RECORDS_SENT, o -> {
            o.add("units", units);
            o.addProperty("me", statusOf(p).name());
            o.addProperty("canBoard", mayEditBoard(p));
            o.addProperty("canOperate", mayOperate(p));
        });
        PacketDistributor.sendToPlayer(p, new PolicePayloads.Snapshot(open, officerName(p), json));
    }

    // ---- unità in servizio ----

    /** Chi non ha mai scelto uno stato è fuori servizio: va dichiarato di essere in pattuglia. */
    public static StatoAgente statusOf(ServerPlayer p) {
        return STATUS.getOrDefault(p.getUUID(), StatoAgente.OFF);
    }

    /** Gli agenti collegati che possono usare l'MDT, con il loro stato; in servizio per primi. */
    private static List<Unita> unitsOnline(MinecraftServer server) {
        List<Unita> out = new ArrayList<>();
        if (server == null) return out;
        for (ServerPlayer q : server.getPlayerList().getPlayers()) {
            if (!mayUse(q)) continue;
            BlockPos pos = q.blockPosition();
            out.add(new Unita(officerName(q), effectiveStatus(q), q.getUUID().toString(), matricolaOf(q), rankOf(q),
                    pos.getX(), pos.getY(), pos.getZ(), q.level().dimension().identifier().toString()));
        }
        out.sort(Comparator.comparing((Unita u) -> u.status().ordinal()).thenComparing(Unita::name));
        return out;
    }

    /**
     * Lo stato mostrato: quello scelto dall'agente, ma con una chiamata in carico segue la chiamata (in viaggio, sul
     * posto, in intervento) e, appena assegnata, risulta occupato. Fuori servizio resta tale.
     */
    private static StatoAgente effectiveStatus(ServerPlayer p) {
        StatoAgente chosen = statusOf(p);
        if (chosen == StatoAgente.OFF) return chosen;
        return db().activeCallOf(p.getUUID().toString()).map(c -> switch (c.state) {
            case EN_ROUTE -> StatoAgente.EN_ROUTE;
            case ON_SCENE -> StatoAgente.ON_SCENE;
            case INTERVENING, BACKUP_REQUESTED -> StatoAgente.INTERVENING;
            case ASSIGNED -> chosen == StatoAgente.PATROL ? StatoAgente.BUSY : chosen;
            default -> chosen;
        }).orElse(chosen);
    }

    /** Il grado nel lavoro del personaggio attivo, se ne ha uno. */
    private static String rankOf(ServerPlayer p) {
        try {
            return QRM.get(CharacterService.class).activeOf(new PlayerId(p.getUUID()))
                    .flatMap(c -> QRM.get(dev.qrm.api.job.JobService.class).assignmentOf(c.id()))
                    .flatMap(a -> QRM.get(dev.qrm.api.job.JobRegistry.class).find(a.jobId())
                            .flatMap(j -> j.grade(a.gradeId())))
                    .map(dev.qrm.api.job.GradeDefinition::label).orElse("");
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static JsonArray unitsJson(MinecraftServer server) {
        JsonArray us = new JsonArray();
        for (Unita u : unitsOnline(server)) {
            JsonObject j = new JsonObject();
            j.addProperty("name", u.name());
            j.addProperty("status", u.status().name());
            j.addProperty("id", u.id());
            j.addProperty("no", u.no());
            j.addProperty("rank", u.rank());
            j.addProperty("x", u.x());
            j.addProperty("y", u.y());
            j.addProperty("z", u.z());
            j.addProperty("dim", u.dim());
            us.add(j);
        }
        return us;
    }

    /** La matricola dell'agente: il numero ID del personaggio attivo, se ha già una scheda. */
    private static String matricolaOf(ServerPlayer p) {
        return QRM.get(CharacterService.class).activeOf(new PlayerId(p.getUUID()))
                .flatMap(c -> db().citizen(c.id().value())).map(c -> c.idNumber).orElse("");
    }

    private static Agente agentOf(ServerPlayer p) {
        return new Agente(officerName(p), selfCitizen(p).idNumber, p.getUUID().toString());
    }

    /** L'operatore di centrale: gli operatori del server e chi ha il nodo del dispatch (in singleplayer chiunque). */
    public static boolean mayOperate(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        if (server != null && server.isSingleplayer()) return true;
        if (p.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return true;
        return QRM.get(CharacterService.class).activeOf(new PlayerId(p.getUUID()))
                .map(c -> QRM.get(PermissionService.class).has(c.id(), NODE_OPERATE)).orElse(false);
    }

    /** L'incaricato della chiamata, o un operatore di centrale. */
    private static boolean mayManage(ServerPlayer p, DispatchCall c) {
        return c.assignee.length() > 0 && c.assigneeId.equals(p.getUUID().toString()) || mayOperate(p);
    }

    private static void deny(ServerPlayer p, String key) {
        p.sendSystemMessage(Component.translatable("qrm_police.dispatch." + key), true);
    }

    private static ServerPlayer playerArg(MinecraftServer server, List<String> args, int i) {
        try {
            return server.getPlayerList().getPlayer(UUID.fromString(arg(args, i, 40)));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean mayEditBoard(ServerPlayer p) {
        if (p.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return true;
        return QRM.get(CharacterService.class).activeOf(new PlayerId(p.getUUID()))
                .map(c -> QRM.get(PermissionService.class).has(c.id(), NODE_BOARD)).orElse(false);
    }

    /** Rimanda lo stato a chi ha l'MDT aperto, senza toccare il salvataggio. */
    private static void pushSnapshots(MinecraftServer server) {
        for (UUID id : new ArrayList<>(VIEWERS)) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null || !mayUse(p)) VIEWERS.remove(id);
            else sendSnapshot(p, false);
        }
    }

    // ---- azioni dal client ----

    private static String arg(List<String> args, int i, int max) {
        if (i >= args.size()) return "";
        String s = args.get(i).strip();
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static boolean replyTooSoon(ServerPlayer p, MinecraftServer server, String kind) {
        int tick = server.getTickCount();
        String key = p.getUUID() + kind;
        Integer last = REPLY_AT.get(key);
        if (last != null && tick - last < REPLY_GAP_TICKS) return true;
        REPLY_AT.put(key, tick);
        return false;
    }

    /** args: testo, categoria, priorità (0 = tutte), stato (vuoto = tutti), data (OGGI, IERI, SETTIMANA o vuoto). */
    private static void sendHistory(ServerPlayer p, List<String> args) {
        StatoChiamata state = StatoChiamata.parse(arg(args, 3, 12), null);
        if (state != null && !state.terminal()) state = null;
        long startToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long from = 0;
        long to = Long.MAX_VALUE;
        switch (arg(args, 4, 12)) {
            case "TODAY" -> from = startToday;
            case "YESTERDAY" -> {
                from = startToday - 86_400_000L;
                to = startToday;
            }
            case "WEEK" -> from = System.currentTimeMillis() - 7 * 86_400_000L;
            default -> { }
        }
        boolean all = mayOperate(p);
        FiltroStorico filter = new FiltroStorico(arg(args, 0, 40), arg(args, 1, 20), (int) number(args, 2, 4), state, from,
                to, all, p.getUUID().toString(), all ? "" : matricolaOf(p));
        JsonArray out = new JsonArray();
        for (DispatchCall c : db().history(filter, MAX_HISTORY)) out.add(PoliceDatabase.callJson(c));
        PacketDistributor.sendToPlayer(p, new PolicePayloads.Reply("history", out.toString()));
    }

    private static long signed(List<String> args, int i) {
        try {
            return Long.parseLong(arg(args, i, 12));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long number(List<String> args, int i, long max) {
        try {
            return Math.max(0, Math.min(max, Long.parseLong(arg(args, i, 12))));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void onAction(ServerPlayer p, String kind, List<String> args) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        if (kind.equals("close")) {
            VIEWERS.remove(p.getUUID());
            return;
        }
        if (!mayUse(p)) return;
        PoliceDatabase d = db();
        String officer = officerName(p);
        long now = System.currentTimeMillis();
        switch (kind) {
            case "refresh" -> {
                syncCitizens();
                        sendSnapshot(p, false);
                return;
            }
            case "units_poll" -> {
                if (replyTooSoon(p, server, "units")) return;
                PacketDistributor.sendToPlayer(p, new PolicePayloads.Reply("units", unitsJson(server).toString()));
                return;
            }
            case "history" -> {
                if (replyTooSoon(p, server, "history")) return;
                sendHistory(p, args);
                return;
            }
            case "support" -> {
                Integer last = SUPPORT_AT.get(p.getUUID());
                int tick = server.getTickCount();
                if (last != null && tick - last < SUPPORT_COOLDOWN_TICKS) {
                    p.sendSystemMessage(Component.translatable("qrm_police.support.cooldown"), true);
                    return;
                }
                String uid = p.getUUID().toString();
                // Una richiesta già aperta dello stesso agente basta: niente doppioni.
                if (d.calls().stream().anyMatch(c -> c.active() && c.type == CallType.BACKUP && c.callerId.equals(uid))) {
                    deny(p, "support_dup");
                    return;
                }
                SUPPORT_AT.put(p.getUUID(), tick);
                BlockPos pos = p.blockPosition();
                var own = d.activeCallOf(uid);
                raiseCall(server, CallType.BACKUP, "Richiesta supporto da " + officer
                        + own.map(c -> " (intervento " + c.id + ")").orElse(""),
                        p.level().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), officer, uid,
                        own.map(c -> c.id).orElse(""));
                return;
            }
            case "status" -> {
                StatoAgente chosen = StatoAgente.parse(arg(args, 0, 12), StatoAgente.OFF);
                if (!chosen.selectable()) return;
                STATUS.put(p.getUUID(), chosen);
                // Chi va fuori servizio non può tenersi una chiamata: torna in attesa per gli altri.
                if (chosen == StatoAgente.OFF) {
                    d.activeCallOf(p.getUUID().toString()).ifPresent(c -> d.releaseCall(c.id, agentOf(p), "L'agente è andato fuori servizio", now));
                }
                pushSnapshots(server);
                return;
            }
            case "panic" -> {
                Integer last = PANIC_AT.get(p.getUUID());
                int tick = server.getTickCount();
                if (last != null && tick - last < PANIC_COOLDOWN_TICKS) {
                    p.sendSystemMessage(Component.translatable("qrm_police.panic.cooldown"), true);
                    return;
                }
                String uid = p.getUUID().toString();
                if (d.calls().stream().anyMatch(c -> c.active() && c.type == CallType.PANIC && c.callerId.equals(uid))) {
                    deny(p, "panic_dup");
                    return;
                }
                PANIC_AT.put(p.getUUID(), tick);
                BlockPos pos = p.blockPosition();
                String idn = selfCitizen(p).idNumber;
                raiseCall(server, CallType.PANIC, "PANICO - agente " + officer + " (" + idn + ")",
                        p.level().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), officer, uid,
                        "");
                return;
            }
            case "board_set" -> {
                if (!mayEditBoard(p)) return;
                d.setBoard(arg(args, 0, MAX_BOARD));
            }
            case "wanted" -> {
                Citizen c = citizenArg(d, args, 0);
                if (c == null) return;
                boolean was = c.wanted;
                c.wanted = number(args, 1, 1) == 1;
                c.wantedReason = c.wanted ? arg(args, 2, 120) : "";
                if (was != c.wanted) {
                    d.addRecord(c, RecordKind.WARRANT, officer,
                            c.wanted ? "Mandato di cattura emesso" : "Mandato di cattura revocato", 0, 0,
                            c.wanted ? c.wantedReason : "", now);
                }
            }
            case "record_add" -> {
                // args: cittadino, tipo, titolo, importo, mesi, note
                Citizen c = citizenArg(d, args, 0);
                RecordKind rk = RecordKind.parse(arg(args, 1, 12), null);
                String offense = arg(args, 2, 80);
                // Multe, arresti e mandati hanno i loro percorsi: qui solo rapporti e note.
                if (c == null || rk == null || rk == RecordKind.WARRANT || rk == RecordKind.FINE
                        || rk == RecordKind.ARREST || offense.isEmpty()) {
                    return;
                }
                long fine = rk == RecordKind.FINE || rk == RecordKind.ARREST ? number(args, 3, 10_000_000) : 0;
                int months = rk == RecordKind.ARREST ? (int) number(args, 4, 1200) : 0;
                d.addRecord(c, rk, officer, offense, fine, months, arg(args, 5, 200), now);
            }
            case "fine_issue" -> {
                // args: cittadino, codice infrazione, motivazione. L'importo lo decide il catalogo.
                Citizen c = citizenArg(d, args, 0);
                var inf = FineCatalog.find(arg(args, 1, 8));
                if (c == null || inf.isEmpty()) return;
                d.issueFine(c, inf.get(), arg(args, 2, 200), officer, now);
            }
            case "arrest_register" -> {
                // args: cittadino, nomi dei reati separati da "|", note. Multa e pena le calcola il codice penale.
                Citizen c = citizenArg(d, args, 0);
                if (c == null) return;
                List<String> names = java.util.Arrays.stream(arg(args, 1, 600).split("\\|")).map(String::strip)
                        .filter(s -> !s.isEmpty()).toList();
                if (d.registerArrest(c, names, arg(args, 2, 200), officer, now) == null) return;
            }
            case "record_archive" -> {
                // args: cittadino, id voce, 1 archivia / 0 ripristina
                Citizen c = citizenArg(d, args, 0);
                if (c == null || !d.archiveRecord(c, arg(args, 1, 16), number(args, 2, 1) == 1, officer)) return;
            }
            case "fine_status" -> {
                // args: id multa, PAID o CANCELED
                FineStatus st = FineStatus.parse(arg(args, 1, 10), FineStatus.UNPAID);
                if (!d.setFineStatus(arg(args, 0, 16), st)) return;
            }
            case "gun_license" -> {
                Citizen c = citizenArg(d, args, 0);
                if (c == null) return;
                c.weaponLicense = !c.weaponLicense;
            }
            case "drive_license" -> {
                Citizen c = citizenArg(d, args, 0);
                if (c == null) return;
                c.drivingLicenseValid = !c.drivingLicenseValid;
            }
            case "danger" -> {
                Citizen c = citizenArg(d, args, 0);
                if (c == null) return;
                c.danger = c.danger.next();
            }
            case "call_take" -> {
                if (statusOf(p) == StatoAgente.OFF) {
                    deny(p, "off_duty");
                    return;
                }
                if (!d.takeCall(arg(args, 0, 16), agentOf(p), now)) {
                    deny(p, "unavailable");
                    return;
                }
            }
            case "call_assign" -> {
                if (!mayOperate(p)) {
                    deny(p, "forbidden");
                    return;
                }
                ServerPlayer target = playerArg(server, args, 1);
                if (target == null || !mayUse(target) || statusOf(target) == StatoAgente.OFF) {
                    deny(p, "target");
                    return;
                }
                String id = arg(args, 0, 16);
                if (!d.assignCall(id, agentOf(target), agentOf(p), now)) {
                    deny(p, "unavailable");
                    return;
                }
                if (target != p) {
                    target.sendSystemMessage(Component.translatable("qrm_police.dispatch.assigned", id));
                    beep(target);
                }
            }
            case "call_release" -> {
                DispatchCall c = d.call(arg(args, 0, 16)).orElse(null);
                if (c == null || !mayManage(p, c) || !d.releaseCall(c.id, agentOf(p), "", now)) {
                    deny(p, "forbidden");
                    return;
                }
            }
            case "call_state" -> {
                DispatchCall c = d.call(arg(args, 0, 16)).orElse(null);
                StatoChiamata target = StatoChiamata.parse(arg(args, 1, 20), null);
                if (c == null || target == null || !mayManage(p, c) || !d.advanceCall(c.id, target, agentOf(p), now)) {
                    deny(p, "forbidden");
                    return;
                }
            }
            case "call_backup" -> {
                DispatchCall c = d.call(arg(args, 0, 16)).orElse(null);
                if (c == null || !mayManage(p, c) || !d.requestBackup(c.id, agentOf(p), now)) {
                    deny(p, "forbidden");
                    return;
                }
                Component msg = Component.translatable("qrm_police.dispatch.backup", c.id, officer, c.x, c.y, c.z);
                for (ServerPlayer q : server.getPlayerList().getPlayers()) {
                    if (!mayUse(q)) continue;
                    q.sendSystemMessage(msg);
                    beep(q);
                }
            }
            case "call_priority" -> {
                if (!mayOperate(p) || !d.setPriority(arg(args, 0, 16), (int) number(args, 1, 4), agentOf(p), now)) {
                    deny(p, "forbidden");
                    return;
                }
            }
            case "call_close" -> {
                DispatchCall c = d.call(arg(args, 0, 16)).orElse(null);
                if (c == null || !mayManage(p, c) || !d.closeCall(c.id, arg(args, 1, MAX_DESC), agentOf(p), now)) {
                    deny(p, "forbidden");
                    return;
                }
            }
            case "call_cancel" -> {
                if (!mayOperate(p) || !d.cancelCall(arg(args, 0, 16), arg(args, 1, MAX_DESC), agentOf(p), now)) {
                    deny(p, "forbidden");
                    return;
                }
            }
            case "call_create" -> {
                // args: tipo, priorità (0 = quella del tipo), descrizione, X, Y, Z
                CallType type = CallType.parse(arg(args, 0, 20));
                String desc = arg(args, 2, MAX_DESC);
                if (!mayOperate(p) || type == CallType.PANIC || type == CallType.POLICE || desc.isEmpty()) {
                    deny(p, "forbidden");
                    return;
                }
                int cx = (int) Math.max(-30_000_000, Math.min(30_000_000, signed(args, 3)));
                int cy = (int) Math.max(-2048, Math.min(2048, signed(args, 4)));
                int cz = (int) Math.max(-30_000_000, Math.min(30_000_000, signed(args, 5)));
                DispatchCall created = raiseCall(server, type, desc, p.level().dimension().identifier().toString(), cx,
                        cy, cz, officer, p.getUUID().toString(), "");
                int prio = (int) number(args, 1, 4);
                if (prio >= 1) d.setPriority(created.id, prio, agentOf(p), now);
            }
            default -> {
                return;
            }
        }
        changed(server);
    }

    private static Citizen citizenArg(PoliceDatabase d, List<String> args, int i) {
        try {
            return d.citizen(UUID.fromString(arg(args, i, 40))).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---- chiamate ----

    /** Crea una chiamata, la notifica a chi ha accesso all'MDT e restituisce la chiamata. */
    public static DispatchCall raiseCall(MinecraftServer server, CallType type, String description, String dimension,
            int x, int y, int z, String caller) {
        return raiseCall(server, type, description, dimension, x, y, z, caller, "", "");
    }

    public static DispatchCall raiseCall(MinecraftServer server, CallType type, String description, String dimension,
            int x, int y, int z, String caller, String callerId, String parent) {
        PoliceDatabase d = db();
        long now = System.currentTimeMillis();
        DispatchCall call = d.addCall(type, description, dimension, x, y, z, now, caller, callerId);
        call.parent = parent;
        d.trimActive(MAX_CALLS, now);
        boolean panic = type == CallType.PANIC;
        Component msg = panic ? Component.translatable("qrm_police.call.panic", description, x, y, z)
                : Component.translatable("qrm_police.call.new", Component.literal(type.label() + " P" + call.priority),
                        description, x, y, z);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!mayUse(p)) continue;
            p.sendSystemMessage(msg);
            beep(p);
            BEEPS.add(new Beep(p.getUUID(), server.getTickCount() + BEEP_GAP));
            // Il panico suona tre volte, così non passa inosservato.
            if (panic) BEEPS.add(new Beep(p.getUUID(), server.getTickCount() + 2 * BEEP_GAP));
        }
        changed(server);
        return call;
    }

    private static void beep(ServerPlayer p) {
        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.8F,
                1.9F);
    }

    /** Un punto casuale fra {@code min} e {@code max} blocchi dal giocatore, alla sua altezza. */
    public static BlockPos randomNear(ServerPlayer p, int min, int max) {
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double dist = min + RANDOM.nextDouble() * (max - min);
        BlockPos base = p.blockPosition();
        return base.offset((int) Math.round(Math.cos(angle) * dist), 0, (int) Math.round(Math.sin(angle) * dist));
    }

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        for (Iterator<Beep> it = BEEPS.iterator(); it.hasNext();) {
            Beep b = it.next();
            if (now < b.atTick()) continue;
            it.remove();
            ServerPlayer p = server.getPlayerList().getPlayer(b.player());
            if (p != null) beep(p);
        }
        if (now % UNITS_CHECK_EVERY == 0 && !VIEWERS.isEmpty()) {
            // Colleghi che entrano o escono: l'elenco delle unità si aggiorna senza che nessuno prema nulla.
            // Solo chi c'è e in che stato: le posizioni cambiano di continuo e non devono far ripartire gli snapshot.
            StringBuilder sigBuilder = new StringBuilder();
            for (Unita u : unitsOnline(server)) sigBuilder.append(u.id()).append(u.status()).append(u.name()).append(';');
            String sig = sigBuilder.toString();
            if (!sig.equals(unitsSeen)) {
                unitsSeen = sig;
                pushSnapshots(server);
            }
        }
        if (now % SAVE_EVERY == 0) flush();
    }
}
