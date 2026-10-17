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
 * Il database di polizia: anagrafe, armi, BOLO e chiamate. Solo dati e regole, nessun Minecraft; si salva come un
 * unico JSON. Non è thread-safe: lo usa il thread del server.
 */
public final class PoliceDatabase {
    private final Map<UUID, Citizen> citizens = new LinkedHashMap<>();
    private final Map<String, WeaponEntry> weapons = new LinkedHashMap<>();
    private final List<BoloEntry> bolos = new ArrayList<>();
    private final List<DispatchCall> calls = new ArrayList<>();
    private int nextId = 1;

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
        citizens.put(c.id, c);
        return c;
    }

    /** Il cittadino con questo id, creato con i dati dati se manca. */
    public Citizen ensureCitizen(UUID id, String first, String last, String birth) {
        return citizens.computeIfAbsent(id, k -> new Citizen(k, first, last, birth));
    }

    public List<Citizen> search(String query) {
        return citizens.values().stream().filter(c -> c.matches(query)).toList();
    }

    public int wantedCount() {
        return (int) citizens.values().stream().filter(c -> c.wanted).count();
    }

    public CriminalRecord addRecord(Citizen c, String officer, String offense, long fine, int jailMonths, String notes,
            long now) {
        CriminalRecord r = new CriminalRecord(newId("R"), now, officer, offense, Math.max(0, fine),
                Math.max(0, jailMonths), notes);
        c.records.add(r);
        return r;
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

    // ---- BOLO ----

    public List<BoloEntry> bolos() {
        return Collections.unmodifiableList(bolos);
    }

    public BoloEntry addBolo(String title, String description, int priority, String author, long now) {
        BoloEntry b = new BoloEntry(newId("B"), title, description, Math.max(1, Math.min(3, priority)), author, now);
        bolos.add(b);
        return b;
    }

    public boolean removeBolo(String id) {
        return bolos.removeIf(b -> b.id().equals(id));
    }

    // ---- chiamate ----

    /** Dalla più recente. */
    public List<DispatchCall> calls() {
        List<DispatchCall> out = new ArrayList<>(calls);
        Collections.reverse(out);
        return out;
    }

    public Optional<DispatchCall> call(String id) {
        return calls.stream().filter(c -> c.id.equals(id)).findFirst();
    }

    public DispatchCall addCall(CallType type, String description, String dimension, int x, int y, int z, long now,
            String caller) {
        DispatchCall c = new DispatchCall(newId("C"), type, description, dimension, x, y, z, now, caller);
        calls.add(c);
        return c;
    }

    /** Aggiunge l'unità alla chiamata (una volta sola). */
    public boolean takeCall(String id, String unit) {
        Optional<DispatchCall> c = call(id);
        if (c.isEmpty() || c.get().units.contains(unit)) return false;
        c.get().units.add(unit);
        return true;
    }

    public boolean closeCall(String id) {
        return calls.removeIf(c -> c.id.equals(id));
    }

    public void clear() {
        citizens.clear();
        weapons.clear();
        bolos.clear();
        calls.clear();
        nextId = 1;
    }

    // ---- JSON ----

    public String toJson() {
        return toJson(Integer.MAX_VALUE);
    }

    /** {@code maxRecords}: quanti precedenti per cittadino (i più recenti), per tenere il pacchetto piccolo. */
    public String toJson(int maxRecords) {
        JsonObject o = new JsonObject();
        o.addProperty("next", nextId);
        JsonArray cs = new JsonArray();
        for (Citizen c : citizens.values()) {
            JsonObject j = new JsonObject();
            j.addProperty("id", c.id.toString());
            j.addProperty("first", c.firstName);
            j.addProperty("last", c.lastName);
            j.addProperty("birth", c.birthDate);
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
        JsonArray bs = new JsonArray();
        for (BoloEntry b : bolos) {
            JsonObject j = new JsonObject();
            j.addProperty("id", b.id());
            j.addProperty("title", b.title());
            j.addProperty("desc", b.description());
            j.addProperty("prio", b.priority());
            j.addProperty("author", b.author());
            j.addProperty("time", b.time());
            bs.add(j);
        }
        o.add("bolos", bs);
        JsonArray cl = new JsonArray();
        for (DispatchCall c : calls) {
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
            JsonArray us = new JsonArray();
            c.units.forEach(us::add);
            j.add("units", us);
            cl.add(j);
        }
        o.add("calls", cl);
        return o.toString();
    }

    /** Un database vuoto se il testo non si legge: meglio ripartire che bloccare il server. */
    public static PoliceDatabase fromJson(String json) {
        PoliceDatabase db = new PoliceDatabase();
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            db.nextId = o.get("next").getAsInt();
            for (JsonElement e : o.getAsJsonArray("citizens")) {
                JsonObject j = e.getAsJsonObject();
                Citizen c = new Citizen(UUID.fromString(j.get("id").getAsString()), j.get("first").getAsString(),
                        j.get("last").getAsString(), j.get("birth").getAsString());
                c.wanted = j.get("wanted").getAsBoolean();
                c.wantedReason = j.get("reason").getAsString();
                c.danger = Danger.parse(j.get("danger").getAsString());
                c.weaponLicense = j.get("gun").getAsBoolean();
                c.drivingLicenseValid = j.get("drive").getAsBoolean();
                for (JsonElement re : j.getAsJsonArray("records")) {
                    JsonObject r = re.getAsJsonObject();
                    c.records.add(new CriminalRecord(r.get("id").getAsString(), r.get("time").getAsLong(),
                            r.get("officer").getAsString(), r.get("offense").getAsString(), r.get("fine").getAsLong(),
                            r.get("jail").getAsInt(), r.get("notes").getAsString()));
                }
                db.citizens.put(c.id, c);
            }
            for (JsonElement e : o.getAsJsonArray("weapons")) {
                JsonObject j = e.getAsJsonObject();
                db.putWeapon(new WeaponEntry(j.get("serial").getAsString(), j.get("type").getAsString(),
                        j.get("ownerId").getAsString(), j.get("owner").getAsString(),
                        WeaponStatus.parse(j.get("status").getAsString())));
            }
            for (JsonElement e : o.getAsJsonArray("bolos")) {
                JsonObject j = e.getAsJsonObject();
                db.bolos.add(new BoloEntry(j.get("id").getAsString(), j.get("title").getAsString(),
                        j.get("desc").getAsString(), j.get("prio").getAsInt(), j.get("author").getAsString(),
                        j.get("time").getAsLong()));
            }
            for (JsonElement e : o.getAsJsonArray("calls")) {
                JsonObject j = e.getAsJsonObject();
                DispatchCall c = new DispatchCall(j.get("id").getAsString(), CallType.parse(j.get("type").getAsString()),
                        j.get("desc").getAsString(), j.get("dim").getAsString(), j.get("x").getAsInt(),
                        j.get("y").getAsInt(), j.get("z").getAsInt(), j.get("time").getAsLong(),
                        j.get("caller").getAsString());
                for (JsonElement u : j.getAsJsonArray("units")) c.units.add(u.getAsString());
                db.calls.add(c);
            }
            return db;
        } catch (RuntimeException e) {
            return new PoliceDatabase();
        }
    }
}
