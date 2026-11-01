package dev.qrm.police.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Il database di polizia: anagrafe, multe, armi e chiamate. Solo dati e regole, nessun Minecraft; si salva come un
 * unico JSON. Non è thread-safe: lo usa il thread del server.
 */
public final class PoliceDatabase {
    private final Map<UUID, Citizen> citizens = new LinkedHashMap<>();
    private final Map<String, WeaponEntry> weapons = new LinkedHashMap<>();
    private final List<DispatchCall> calls = new ArrayList<>();
    private final List<Fine> fines = new ArrayList<>();
    /** La bacheca del dipartimento: comunicazioni di turno, uguali per tutti. */
    private String board = "";
    private int nextId = 1;
    private int nextCitizenNo = 1;
    private int nextCallNo = 1;
    private static final int MAX_FINES_SENT = 200;

    public String newId(String prefix) {
        return prefix + (nextId++);
    }

    // ---- anagrafe ----

    public Collection<Citizen> citizens() {
        return Collections.unmodifiableCollection(citizens.values());
    }

    public Optional<Citizen> citizen(UUID id) {
        return Optional.ofNullable(citizens.get(id));
    }

    public Citizen addCitizen(Citizen c) {
        if (c.idNumber.isEmpty()) c.idNumber = String.format(Locale.ROOT, "QRM-%06d", nextCitizenNo++);
        citizens.put(c.id, c);
        return c;
    }

    /** Il cittadino con questo id, creato con i dati dati se manca. */
    public Citizen ensureCitizen(UUID id, String first, String last, String birth) {
        Citizen c = citizens.get(id);
        return c != null ? c : addCitizen(new Citizen(id, first, last, birth));
    }

    public List<Citizen> search(String query) {
        return citizens.values().stream().filter(c -> c.matches(query)).toList();
    }

    public int wantedCount() {
        return (int) citizens.values().stream().filter(c -> c.wanted).count();
    }

    public CriminalRecord addRecord(Citizen c, String officer, String offense, long fine, int jailMonths, String notes,
            long now) {
        return addRecord(c, CriminalRecord.legacyKind(jailMonths), officer, offense, fine, jailMonths, notes, now);
    }

    public CriminalRecord addRecord(Citizen c, RecordKind kind, String officer, String offense, long fine,
            int jailMonths, String notes, long now) {
        CriminalRecord r = new CriminalRecord(newId("R"), now, officer, offense, Math.max(0, fine),
                Math.max(0, jailMonths), notes, kind, false, "");
        c.records.add(r);
        return r;
    }

    /**
     * Registra un arresto per uno o più reati del codice penale (per nome). Multa e detenzione sono la somma delle
     * sanzioni del codice. @return null se non ci sono reati o un nome non esiste.
     */
    public CriminalRecord registerArrest(Citizen c, List<String> offenseNames, String notes, String officer,
            long now) {
        List<String> names = offenseNames.stream().distinct().toList();
        if (names.isEmpty()) return null;
        long fine = 0;
        int jail = 0;
        for (String name : names) {
            PenalCode.Offense o = PenalCode.OFFENSES.stream().filter(x -> x.name().equals(name)).findFirst()
                    .orElse(null);
            if (o == null) return null;
            fine += o.fine();
            jail += o.jailMonths();
        }
        String joined = String.join(", ", names);
        if (joined.length() > 80) joined = joined.substring(0, 77) + "...";
        return addRecord(c, RecordKind.ARREST, officer, joined, fine, jail, notes, now);
    }

    /**
     * Archivia (o ripristina) una voce: resta consultabile ma esce dalla fedina. Una multa non ancora pagata non si
     * archivia. @return false se la voce non c'è, è già nello stato chiesto o non si può archiviare.
     */
    public boolean archiveRecord(Citizen c, String recordId, boolean archive, String by) {
        for (int i = 0; i < c.records.size(); i++) {
            CriminalRecord r = c.records.get(i);
            if (!r.id().equals(recordId)) continue;
            if (r.archived() == archive) return false;
            if (archive && fines.stream().anyMatch(f -> recordId.equals(f.recordId) && f.status == FineStatus.UNPAID)) {
                return false;
            }
            c.records.set(i, r.withArchived(archive, by));
            return true;
        }
        return false;
    }

    // ---- multe ----

    /** Dalla più vecchia alla più recente. */
    public List<Fine> fines() {
        return Collections.unmodifiableList(fines);
    }

    public Optional<Fine> fine(String id) {
        return fines.stream().filter(f -> f.id.equals(id)).findFirst();
    }

    /** Emette la multa e ne lascia traccia anche nello storico del cittadino. L'importo è quello del catalogo. */
    public Fine issueFine(Citizen c, FineCatalog.Infraction inf, String reason, String officer, long now) {
        return issueFine(c, inf.code(), inf.name(), inf.amount(), reason, officer, now);
    }

    private Fine issueFine(Citizen c, String code, String offense, long amount, String reason, String officer,
            long now) {
        CriminalRecord r = addRecord(c, RecordKind.FINE, officer, offense, amount, 0, reason, now);
        Fine f = new Fine(newId("F"), c.id.toString(), c.fullName(), code, offense, amount, reason, officer, now,
                FineStatus.UNPAID, r.id());
        fines.add(f);
        return f;
    }

    /**
     * Da non pagata a pagata o annullata. Annullare toglie la voce dallo storico del cittadino: la multa resta
     * nell'archivio come annullata. @return false se non esiste o non è più non pagata.
     */
    public boolean setFineStatus(String id, FineStatus status) {
        Optional<Fine> found = fine(id);
        if (found.isEmpty() || status == FineStatus.UNPAID || found.get().status != FineStatus.UNPAID) return false;
        Fine f = found.get();
        f.status = status;
        if (status == FineStatus.CANCELED) {
            try {
                citizen(UUID.fromString(f.citizenId)).ifPresent(c -> c.records.removeIf(r -> r.id().equals(f.recordId)));
            } catch (IllegalArgumentException e) {
                // id non valido: niente da togliere
            }
            f.recordId = "";
        }
        return true;
    }

    // ---- armi ----

    public Collection<WeaponEntry> weapons() {
        return Collections.unmodifiableCollection(weapons.values());
    }

    public Optional<WeaponEntry> weapon(String serial) {
        return Optional.ofNullable(weapons.get(serial.toUpperCase(Locale.ROOT)));
    }

    public void putWeapon(WeaponEntry w) {
        weapons.put(w.serial().toUpperCase(Locale.ROOT), w);
    }

    /** Un numero di matricola nuovo, es. {@code QRM-91827}. */
    public String newSerial(Random rnd) {
        String s;
        do {
            s = "QRM-" + (10000 + rnd.nextInt(90000));
        } while (weapons.containsKey(s));
        return s;
    }

    /** Cerca per matricola (anche parziale) o per nome del proprietario. */
    public List<WeaponEntry> searchWeapons(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        return weapons.values().stream().filter(w -> q.isEmpty() || w.serial().toLowerCase(Locale.ROOT).contains(q)
                || w.ownerName().toLowerCase(Locale.ROOT).contains(q)).toList();
    }

    public boolean setWeaponStatus(String serial, WeaponStatus status) {
        String key = serial.toUpperCase(Locale.ROOT);
        WeaponEntry w = weapons.get(key);
        if (w == null) return false;
        weapons.put(key, w.withStatus(status));
        return true;
    }

    public String board() {
        return board;
    }

    public void setBoard(String text) {
        board = text;
    }

    // ---- chiamate ----

    private static final int MAX_CLOSED_CALLS = 300;
    private static final int MAX_EVENTS = 40;

    /** Tutte, anche concluse e annullate, dalla più recente. */
    public List<DispatchCall> calls() {
        List<DispatchCall> out = new ArrayList<>(calls);
        Collections.reverse(out);
        return out;
    }

    /** Solo quelle ancora aperte, dalla più recente. */
    public List<DispatchCall> activeCalls() {
        return calls().stream().filter(DispatchCall::active).toList();
    }

    public Optional<DispatchCall> call(String id) {
        return calls.stream().filter(c -> c.id.equals(id)).findFirst();
    }

    /** La chiamata aperta di cui l'agente (id giocatore) è l'incaricato. */
    public Optional<DispatchCall> activeCallOf(String agentId) {
        return calls.stream().filter(c -> c.active() && !agentId.isEmpty() && c.assigneeId.equals(agentId)).findFirst();
    }

    public DispatchCall addCall(CallType type, String description, String dimension, int x, int y, int z, long now,
            String caller) {
        return addCall(type, description, dimension, x, y, z, now, caller, "");
    }

    public DispatchCall addCall(CallType type, String description, String dimension, int x, int y, int z, long now,
            String caller, String callerId) {
        DispatchCall c = new DispatchCall(String.format(java.util.Locale.ROOT, "D-%03d", nextCallNo++), type,
                description, dimension, x, y, z, now, caller);
        c.callerId = callerId;
        calls.add(c);
        log(c, "RECEIVED", Agente.SYSTEM, "Chiamata ricevuta", now);
        return c;
    }

    private void log(DispatchCall c, String type, Agente by, String text, long now) {
        c.events.add(new EventoChiamata(now, type, by.name(), by.no(), text));
        if (c.events.size() > MAX_EVENTS) c.events.remove(1);
    }

    private Optional<DispatchCall> activeCall(String id) {
        return call(id).filter(DispatchCall::active);
    }

    /** Prende in carico una chiamata in attesa. Solo uno ci riesce: se è già assegnata non cambia nulla. */
    public boolean takeCall(String id, Agente who, long now) {
        Optional<DispatchCall> found = activeCall(id);
        if (found.isEmpty() || found.get().state != StatoChiamata.PENDING || found.get().assigned()) return false;
        assign(found.get(), who, now);
        log(found.get(), "ASSIGNED", who, who.name() + " ha preso in carico la chiamata", now);
        return true;
    }

    /** Assegna (o riassegna) la chiamata a un agente. @return false se è chiusa o l'agente è già l'incaricato. */
    public boolean assignCall(String id, Agente target, Agente by, long now) {
        Optional<DispatchCall> found = activeCall(id);
        if (found.isEmpty() || (found.get().assigned() && found.get().assigneeId.equals(target.id()))) return false;
        assign(found.get(), target, now);
        log(found.get(), "ASSIGNED", by, "Assegnata a " + target.name() + " da " + by.name(), now);
        return true;
    }

    private static void assign(DispatchCall c, Agente who, long now) {
        c.assignee = who.name();
        c.assigneeNo = who.no();
        c.assigneeId = who.id();
        c.assignedAt = now;
        c.state = StatoChiamata.ASSIGNED;
    }

    /** Toglie l'incaricato e rimette la chiamata in attesa. */
    public boolean releaseCall(String id, Agente by, String reason, long now) {
        Optional<DispatchCall> found = activeCall(id);
        if (found.isEmpty() || !found.get().assigned()) return false;
        DispatchCall c = found.get();
        String was = c.assignee;
        c.assignee = "";
        c.assigneeNo = "";
        c.assigneeId = "";
        c.assignedAt = 0;
        c.state = StatoChiamata.PENDING;
        log(c, "RELEASED", by, reason.isEmpty() ? was + " ha rilasciato la chiamata" : reason, now);
        return true;
    }

    /** In viaggio, sul posto o in intervento: solo se c'è un incaricato e lo stato cambia davvero. */
    public boolean advanceCall(String id, StatoChiamata target, Agente by, long now) {
        if (target != StatoChiamata.EN_ROUTE && target != StatoChiamata.ON_SCENE
                && target != StatoChiamata.INTERVENING) {
            return false;
        }
        Optional<DispatchCall> found = activeCall(id);
        if (found.isEmpty() || !found.get().assigned() || found.get().state == target) return false;
        found.get().state = target;
        log(found.get(), target.name(), by, found.get().assignee + ": " + target.label().toLowerCase(java.util.Locale.ROOT),
                now);
        return true;
    }

    public boolean requestBackup(String id, Agente by, long now) {
        Optional<DispatchCall> found = activeCall(id);
        if (found.isEmpty() || !found.get().assigned() || found.get().state == StatoChiamata.BACKUP_REQUESTED) {
            return false;
        }
        found.get().state = StatoChiamata.BACKUP_REQUESTED;
        log(found.get(), "BACKUP", by, "Richiesti rinforzi da " + by.name(), now);
        return true;
    }

    public boolean setPriority(String id, int priority, Agente by, long now) {
        int p = Math.max(1, Math.min(4, priority));
        Optional<DispatchCall> found = activeCall(id);
        if (found.isEmpty() || found.get().priority == p) return false;
        found.get().priority = p;
        log(found.get(), "PRIORITY", by, "Priorità portata a P" + p + " da " + by.name(), now);
        return true;
    }

    public boolean closeCall(String id, String outcome, Agente by, long now) {
        return finish(id, StatoChiamata.CLOSED, outcome, by, "Intervento concluso", now);
    }

    public boolean cancelCall(String id, String reason, Agente by, long now) {
        return finish(id, StatoChiamata.CANCELED, reason, by, "Chiamata annullata", now);
    }

    private boolean finish(String id, StatoChiamata end, String note, Agente by, String text, long now) {
        Optional<DispatchCall> found = activeCall(id);
        if (found.isEmpty()) return false;
        DispatchCall c = found.get();
        c.state = end;
        c.outcome = note;
        c.closedAt = now;
        log(c, end.name(), by, note.isEmpty() ? text : text + ": " + note, now);
        trimClosed();
        return true;
    }

    private void trimClosed() {
        List<DispatchCall> done = calls.stream().filter(c -> !c.active()).toList();
        for (int i = 0; i < done.size() - MAX_CLOSED_CALLS; i++) calls.remove(done.get(i));
    }

    /** Concluse e annullate che passano i filtri, dalla più recente. */
    public List<DispatchCall> history(FiltroStorico f, int limit) {
        String q = f.query().strip().toLowerCase(java.util.Locale.ROOT);
        List<DispatchCall> out = new ArrayList<>();
        for (DispatchCall c : calls()) {
            if (c.active()) continue;
            if (!q.isEmpty() && !(c.id.toLowerCase(java.util.Locale.ROOT).contains(q)
                    || c.assignee.toLowerCase(java.util.Locale.ROOT).contains(q)
                    || c.assigneeNo.toLowerCase(java.util.Locale.ROOT).contains(q)
                    || c.caller.toLowerCase(java.util.Locale.ROOT).contains(q)
                    || c.description.toLowerCase(java.util.Locale.ROOT).contains(q))) {
                continue;
            }
            if (!f.category().isEmpty() && !c.type.category().equals(f.category())) continue;
            if (f.priority() != 0 && c.priority != f.priority()) continue;
            if (f.state() != null && c.state != f.state()) continue;
            if (c.time < f.from() || c.time >= f.to()) continue;
            if (!f.all() && !involves(c, f.involvedId(), f.involvedNo())) continue;
            out.add(c);
            if (out.size() >= limit) break;
        }
        return out;
    }

    private static boolean involves(DispatchCall c, String id, String no) {
        if (!id.isEmpty() && (c.assigneeId.equals(id) || c.callerId.equals(id))) return true;
        return !no.isEmpty() && (c.assigneeNo.equals(no) || c.events.stream().anyMatch(e -> no.equals(e.agentNo())));
    }

    /** Annulla le chiamate aperte più vecchie oltre il numero massimo, per non riempire la centrale. */
    public void trimActive(int max, long now) {
        List<DispatchCall> open = calls.stream().filter(DispatchCall::active).toList();
        for (int i = 0; i < open.size() - max; i++) {
            finish(open.get(i).id, StatoChiamata.CANCELED, "troppe chiamate aperte", Agente.SYSTEM, "Chiamata annullata",
                    now);
        }
    }

    public void clear() {
        citizens.clear();
        weapons.clear();
        calls.clear();
        fines.clear();
        board = "";
        nextId = 1;
        nextCitizenNo = 1;
        nextCallNo = 1;
    }

    // ---- JSON ----

    public String toJson() {
        return toJson(Integer.MAX_VALUE);
    }

    /** {@code maxRecords}: quanti precedenti per cittadino (i più recenti), per tenere il pacchetto piccolo. */
    public String toJson(int maxRecords) {
        return toJson(maxRecords, o -> {});
    }

    /** Come {@link #toJson(int)}, con in più i campi che {@code extra} aggiunge (stato che non si salva). */
    public String toJson(int maxRecords, java.util.function.Consumer<JsonObject> extra) {
        JsonObject o = new JsonObject();
        o.addProperty("board", board);
        o.addProperty("next", nextId);
        o.addProperty("nextCitizen", nextCitizenNo);
        o.addProperty("nextCall", nextCallNo);
        JsonArray cs = new JsonArray();
        for (Citizen c : citizens.values()) {
            JsonObject j = new JsonObject();
            j.addProperty("id", c.id.toString());
            j.addProperty("first", c.firstName);
            j.addProperty("last", c.lastName);
            j.addProperty("birth", c.birthDate);
            j.addProperty("idn", c.idNumber);
            j.addProperty("player", c.playerId);
            j.addProperty("wanted", c.wanted);
            j.addProperty("reason", c.wantedReason);
            j.addProperty("danger", c.danger.name());
            j.addProperty("gun", c.weaponLicense);
            j.addProperty("drive", c.drivingLicenseValid);
            JsonArray rs = new JsonArray();
            int from = Math.max(0, c.records.size() - maxRecords);
            for (CriminalRecord r : c.records.subList(from, c.records.size())) {
                JsonObject rj = new JsonObject();
                rj.addProperty("id", r.id());
                rj.addProperty("time", r.time());
                rj.addProperty("officer", r.officer());
                rj.addProperty("offense", r.offense());
                rj.addProperty("fine", r.fine());
                rj.addProperty("jail", r.jailMonths());
                rj.addProperty("notes", r.notes());
                rj.addProperty("kind", r.kind().name());
                rj.addProperty("arch", r.archived());
                rj.addProperty("archBy", r.archivedBy());
                rs.add(rj);
            }
            j.add("records", rs);
            cs.add(j);
        }
        o.add("citizens", cs);
        JsonArray ws = new JsonArray();
        for (WeaponEntry w : weapons.values()) {
            JsonObject j = new JsonObject();
            j.addProperty("serial", w.serial());
            j.addProperty("type", w.type());
            j.addProperty("ownerId", w.ownerId());
            j.addProperty("owner", w.ownerName());
            j.addProperty("status", w.status().name());
            ws.add(j);
        }
        o.add("weapons", ws);
        JsonArray cl = new JsonArray();
        boolean forSave = maxRecords == Integer.MAX_VALUE;
        for (DispatchCall c : calls) {
            // Ai client solo le aperte; il salvataggio tiene anche lo storico.
            if (!forSave && !c.active()) continue;
            JsonObject j = callJson(c);
            cl.add(j);
        }
        o.add("calls", cl);
        JsonArray fs = new JsonArray();
        // Nel pacchetto per i client solo le più recenti; il salvataggio le tiene tutte.
        int fromFine = maxRecords == Integer.MAX_VALUE ? 0 : Math.max(0, fines.size() - MAX_FINES_SENT);
        for (Fine f : fines.subList(fromFine, fines.size())) {
            JsonObject j = new JsonObject();
            j.addProperty("id", f.id);
            j.addProperty("cid", f.citizenId);
            j.addProperty("cname", f.citizenName);
            j.addProperty("code", f.code);
            j.addProperty("cat", FineCatalog.categoryOf(f.code));
            j.addProperty("offense", f.offense);
            j.addProperty("amount", f.amount);
            j.addProperty("reason", f.reason);
            j.addProperty("officer", f.officer);
            j.addProperty("time", f.time);
            j.addProperty("status", f.status.name());
            j.addProperty("rec", f.recordId);
            fs.add(j);
        }
        o.add("fines", fs);
        extra.accept(o);
        return o.toString();
    }

    /** Una chiamata come la vedono i client e il salvataggio. */
    public static JsonObject callJson(DispatchCall c) {
        JsonObject j = new JsonObject();
        j.addProperty("id", c.id);
        j.addProperty("type", c.type.name());
        j.addProperty("desc", c.description);
        j.addProperty("dim", c.dimension);
        j.addProperty("x", c.x);
        j.addProperty("y", c.y);
        j.addProperty("z", c.z);
        j.addProperty("time", c.time);
        j.addProperty("caller", c.caller);
        j.addProperty("callerId", c.callerId);
        j.addProperty("prio", c.priority);
        j.addProperty("state", c.state.name());
        j.addProperty("assignee", c.assignee);
        j.addProperty("assigneeNo", c.assigneeNo);
        j.addProperty("assigneeId", c.assigneeId);
        j.addProperty("assignedAt", c.assignedAt);
        j.addProperty("parent", c.parent);
        j.addProperty("outcome", c.outcome);
        j.addProperty("closedAt", c.closedAt);
        JsonArray es = new JsonArray();
        for (EventoChiamata e : c.events) {
            JsonObject ej = new JsonObject();
            ej.addProperty("time", e.time());
            ej.addProperty("type", e.type());
            ej.addProperty("agent", e.agent());
            ej.addProperty("agentNo", e.agentNo());
            ej.addProperty("text", e.text());
            es.add(ej);
        }
        j.add("events", es);
        return j;
    }

    private static String optString(JsonObject j, String key) {
        return j.has(key) ? j.get(key).getAsString() : "";
    }

    /** Un database vuoto se il testo non si legge: meglio ripartire che bloccare il server. */
    public static PoliceDatabase fromJson(String json) {
        PoliceDatabase db = new PoliceDatabase();
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            db.nextId = o.get("next").getAsInt();
            db.nextCitizenNo = o.has("nextCitizen") ? o.get("nextCitizen").getAsInt() : 1;
            db.board = o.has("board") ? o.get("board").getAsString() : "";
            for (JsonElement e : o.getAsJsonArray("citizens")) {
                JsonObject j = e.getAsJsonObject();
                Citizen c = new Citizen(UUID.fromString(j.get("id").getAsString()), j.get("first").getAsString(),
                        j.get("last").getAsString(), j.get("birth").getAsString());
                if (j.has("idn")) c.idNumber = j.get("idn").getAsString();
                if (j.has("player")) c.playerId = j.get("player").getAsString();
                c.wanted = j.get("wanted").getAsBoolean();
                c.wantedReason = j.get("reason").getAsString();
                c.danger = Danger.parse(j.get("danger").getAsString());
                c.weaponLicense = j.get("gun").getAsBoolean();
                c.drivingLicenseValid = j.get("drive").getAsBoolean();
                for (JsonElement re : j.getAsJsonArray("records")) {
                    JsonObject r = re.getAsJsonObject();
                    int jail = r.get("jail").getAsInt();
                    RecordKind kind = r.has("kind")
                            ? RecordKind.parse(r.get("kind").getAsString(), CriminalRecord.legacyKind(jail))
                            : CriminalRecord.legacyKind(jail);
                    c.records.add(new CriminalRecord(r.get("id").getAsString(), r.get("time").getAsLong(),
                            r.get("officer").getAsString(), r.get("offense").getAsString(), r.get("fine").getAsLong(),
                            jail, r.get("notes").getAsString(), kind, r.has("arch") && r.get("arch").getAsBoolean(),
                            r.has("archBy") ? r.get("archBy").getAsString() : ""));
                }
                db.addCitizen(c);
            }
            for (JsonElement e : o.getAsJsonArray("weapons")) {
                JsonObject j = e.getAsJsonObject();
                db.putWeapon(new WeaponEntry(j.get("serial").getAsString(), j.get("type").getAsString(),
                        j.get("ownerId").getAsString(), j.get("owner").getAsString(),
                        WeaponStatus.parse(j.get("status").getAsString())));
            }
            db.nextCallNo = o.has("nextCall") ? o.get("nextCall").getAsInt() : 1;
            for (JsonElement e : o.getAsJsonArray("calls")) {
                JsonObject j = e.getAsJsonObject();
                DispatchCall c = new DispatchCall(j.get("id").getAsString(), CallType.parse(j.get("type").getAsString()),
                        j.get("desc").getAsString(), j.get("dim").getAsString(), j.get("x").getAsInt(),
                        j.get("y").getAsInt(), j.get("z").getAsInt(), j.get("time").getAsLong(),
                        j.get("caller").getAsString());
                c.callerId = optString(j, "callerId");
                if (j.has("prio")) c.priority = Math.max(1, Math.min(4, j.get("prio").getAsInt()));
                c.assignee = optString(j, "assignee");
                c.assigneeNo = optString(j, "assigneeNo");
                c.assigneeId = optString(j, "assigneeId");
                if (c.assignee.isEmpty() && j.has("units") && j.getAsJsonArray("units").size() > 0) {
                    // Dati di prima del rinnovo: la prima unità diventa l'incaricato.
                    c.assignee = j.getAsJsonArray("units").get(0).getAsString();
                }
                c.assignedAt = j.has("assignedAt") ? j.get("assignedAt").getAsLong() : 0;
                c.parent = optString(j, "parent");
                c.outcome = optString(j, "outcome");
                c.closedAt = j.has("closedAt") ? j.get("closedAt").getAsLong() : 0;
                c.state = StatoChiamata.parse(optString(j, "state"),
                        c.assigned() ? StatoChiamata.ASSIGNED : StatoChiamata.PENDING);
                if (j.has("events")) {
                    for (JsonElement ee : j.getAsJsonArray("events")) {
                        JsonObject ej = ee.getAsJsonObject();
                        c.events.add(new EventoChiamata(ej.get("time").getAsLong(), optString(ej, "type"),
                                optString(ej, "agent"), optString(ej, "agentNo"), optString(ej, "text")));
                    }
                }
                db.calls.add(c);
            }
            if (o.has("fines")) {
                for (JsonElement e : o.getAsJsonArray("fines")) {
                    JsonObject j = e.getAsJsonObject();
                    db.fines.add(new Fine(j.get("id").getAsString(), j.get("cid").getAsString(),
                            j.get("cname").getAsString(), j.get("code").getAsString(), j.get("offense").getAsString(),
                            j.get("amount").getAsLong(), j.get("reason").getAsString(), j.get("officer").getAsString(),
                            j.get("time").getAsLong(), FineStatus.parse(j.get("status").getAsString(), FineStatus.UNPAID),
                            j.get("rec").getAsString()));
                }
            }
            return db;
        } catch (RuntimeException e) {
            return new PoliceDatabase();
        }
    }
}
