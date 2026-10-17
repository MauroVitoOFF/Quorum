package dev.qrm.police.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.data.ModDataService;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.permission.PermissionService;
import dev.qrm.police.logic.CallType;
import dev.qrm.police.logic.Citizen;
import dev.qrm.police.logic.DispatchCall;
import dev.qrm.police.logic.PoliceDatabase;
import dev.qrm.police.net.PolicePayloads;
import java.util.ArrayList;
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

/** Il database di polizia sul server e tutto ciò che lo cambia. Ogni azione ricontrolla accesso e argomenti. */
public final class PoliceServer {
    public static final String NODE_USE = "police.mdt.use";
    private static final String NAMESPACE = "qrm_police";
    private static final String KEY = "database";
    private static final int SAVE_EVERY = 100;
    private static final int MAX_RECORDS_SENT = 40;
    private static final int MAX_CALLS = 50;
    private static final int BEEP_GAP = 5;

    private record Beep(UUID player, int atTick) {}

    private static PoliceDatabase db;
    private static boolean dirty;
    private static final Map<UUID, Integer> SUPPORT_AT = new HashMap<>();
    private static final int SUPPORT_COOLDOWN_TICKS = 200;
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
        VIEWERS.clear();
        BEEPS.clear();
    }

    public static void forget(UUID player) {
        SUPPORT_AT.remove(player);
        VIEWERS.remove(player);
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
            PlayerCharacter pc = c.get();
            return db().ensureCitizen(pc.id().value(), pc.firstName(), pc.lastName(), pc.birthDate().toString());
        }
        String name = p.getGameProfile().name();
        return db().ensureCitizen(p.getUUID(), name, "(giocatore)", "—");
    }

    // ---- apertura e stato ----

    public static void open(ServerPlayer p) {
        if (!mayUse(p)) {
            p.sendSystemMessage(Component.translatable("qrm_police.denied"), true);
            return;
        }
        VIEWERS.add(p.getUUID());
        sendSnapshot(p, true);
    }

    private static void sendSnapshot(ServerPlayer p, boolean open) {
        PacketDistributor.sendToPlayer(p, new PolicePayloads.Snapshot(open, officerName(p), db().toJson(MAX_RECORDS_SENT)));
    }

    // ---- azioni dal client ----

    private static String arg(List<String> args, int i, int max) {
        if (i >= args.size()) return "";
        String s = args.get(i).strip();
        return s.length() > max ? s.substring(0, max) : s;
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
                sendSnapshot(p, false);
                return;
            }
            case "support" -> {
                Integer last = SUPPORT_AT.get(p.getUUID());
                int tick = server.getTickCount();
                if (last != null && tick - last < SUPPORT_COOLDOWN_TICKS) {
                    p.sendSystemMessage(Component.translatable("qrm_police.support.cooldown"), true);
                    return;
                }
                SUPPORT_AT.put(p.getUUID(), tick);
                BlockPos pos = p.blockPosition();
                raiseCall(server, CallType.POLICE, "Richiesta supporto operativo da " + officer,
                        p.level().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), officer);
                return;
            }
            case "bolo_add" -> {
                String title = arg(args, 0, 60);
                if (title.isEmpty()) return;
                d.addBolo(title, arg(args, 1, 200), (int) number(args, 2, 3), officer, now);
            }
            case "bolo_remove" -> {
                if (!d.removeBolo(arg(args, 0, 16))) return;
            }
            case "wanted" -> {
                Citizen c = citizenArg(d, args, 0);
                if (c == null) return;
                c.wanted = number(args, 1, 1) == 1;
                c.wantedReason = c.wanted ? arg(args, 2, 120) : "";
            }
            case "record_add" -> {
                Citizen c = citizenArg(d, args, 0);
                String offense = arg(args, 1, 80);
                if (c == null || offense.isEmpty()) return;
                d.addRecord(c, officer, offense, number(args, 2, 10_000_000), (int) number(args, 3, 1200),
                        arg(args, 4, 120), now);
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
                if (!d.takeCall(arg(args, 0, 16), officer)) return;
            }
            case "call_close" -> {
                if (!d.closeCall(arg(args, 0, 16))) return;
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
        PoliceDatabase d = db();
        DispatchCall call = d.addCall(type, description, dimension, x, y, z, System.currentTimeMillis(), caller);
        while (d.calls().size() > MAX_CALLS) d.closeCall(d.calls().get(d.calls().size() - 1).id);
        Component msg = Component.translatable("qrm_police.call.new", Component.translatable(
                "qrm_police.call.type." + type.name().toLowerCase(java.util.Locale.ROOT)), description, x, y, z);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!mayUse(p)) continue;
            p.sendSystemMessage(msg);
            beep(p);
            BEEPS.add(new Beep(p.getUUID(), server.getTickCount() + BEEP_GAP));
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
        if (now % SAVE_EVERY == 0) flush();
    }
}
