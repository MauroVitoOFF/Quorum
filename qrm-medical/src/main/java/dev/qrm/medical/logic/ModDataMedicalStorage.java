package dev.qrm.medical.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qrm.api.data.ModDataService;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.id.CharacterId;
import java.util.Optional;

/** Lo stato medico sta nei dati dei mod di QRM: namespace {@code qrm_medical}. */
public final class ModDataMedicalStorage implements MedicalStorage {
    public static final String NAMESPACE = "qrm_medical";
    public static final String PROFILE_KEY = "profile";
    public static final String HOSPITAL_KEY = "hospital";

    private final ModDataService data;

    public ModDataMedicalStorage(ModDataService data) {
        this.data = data;
    }

    @Override
    public Optional<MedicalProfile> read(CharacterId character) {
        Optional<String> raw = data.get(NAMESPACE, OwnerRef.character(character), PROFILE_KEY);
        if (raw.isEmpty()) return Optional.empty();
        try {
            JsonObject o = JsonParser.parseString(raw.get()).getAsJsonObject();
            MedicalProfile p = new MedicalProfile();
            p.setState(MedicalState.byOrdinal(o.get("state").getAsInt()));
            p.setBleedOutTicks(o.get("bleed").getAsInt());
            p.setDownedTicks(o.get("downed").getAsInt());
            for (var e : o.getAsJsonArray("injuries")) {
                JsonObject i = e.getAsJsonObject();
                p.addInjury(new InjuryRecord(InjuryType.parse(i.get("type").getAsString()),
                        i.get("at").getAsLong(), i.get("sev").getAsFloat()));
            }
            return Optional.of(p);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    @Override
    public void write(CharacterId character, MedicalProfile p) {
        OwnerRef owner = OwnerRef.character(character);
        if (p.state() == MedicalState.HEALTHY && p.injuries().isEmpty()) {
            data.delete(NAMESPACE, owner, PROFILE_KEY);
            return;
        }
        JsonObject o = new JsonObject();
        o.addProperty("state", p.state().ordinal());
        o.addProperty("bleed", p.bleedOutTicks());
        o.addProperty("downed", p.downedTicks());
        JsonArray arr = new JsonArray();
        for (InjuryRecord r : p.injuries()) {
            JsonObject i = new JsonObject();
            i.addProperty("type", r.type().name());
            i.addProperty("at", r.timestamp());
            i.addProperty("sev", r.severity());
            arr.add(i);
        }
        o.add("injuries", arr);
        data.put(NAMESPACE, owner, PROFILE_KEY, o.toString());
    }

    @Override
    public Optional<Hospital> hospital() {
        return data.get(NAMESPACE, OwnerRef.system(), HOSPITAL_KEY).flatMap(raw -> {
            try {
                JsonObject o = JsonParser.parseString(raw).getAsJsonObject();
                return Optional.of(new Hospital(o.get("dim").getAsString(), o.get("x").getAsDouble(),
                        o.get("y").getAsDouble(), o.get("z").getAsDouble(), o.get("yaw").getAsFloat()));
            } catch (RuntimeException e) {
                return Optional.empty();
            }
        });
    }

    @Override
    public void setHospital(Hospital h) {
        JsonObject o = new JsonObject();
        o.addProperty("dim", h.dimension());
        o.addProperty("x", h.x());
        o.addProperty("y", h.y());
        o.addProperty("z", h.z());
        o.addProperty("yaw", h.yaw());
        data.put(NAMESPACE, OwnerRef.system(), HOSPITAL_KEY, o.toString());
    }
}
