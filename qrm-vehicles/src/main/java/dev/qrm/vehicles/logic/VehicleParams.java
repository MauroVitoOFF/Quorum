package dev.qrm.vehicles.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

/**
 * Lettura e scrittura in JSON dei parametri di guida. Il file di un veicolo ha i campi di {@link VehicleDef} e, sotto
 * {@code powertrain}, quelli di {@link Powertrain}; un file può contenere solo i campi da cambiare, gli altri restano
 * quelli predefiniti. I fondi stanno in un unico file con un oggetto per id.
 */
public final class VehicleParams {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private VehicleParams() {}

    // ---- veicoli ----

    public static String toJson(VehicleDef def) {
        JsonObject o = new JsonObject();
        o.addProperty("seats", def.seats());
        o.addProperty("maxSpeed", def.maxSpeed());
        o.addProperty("maxSteerDeg", def.maxSteerDeg());
        o.addProperty("steerRateDeg", def.steerRateDeg());
        o.addProperty("wheelBase", def.wheelBase());
        o.addProperty("fuelCapacity", def.fuelCapacity());
        o.addProperty("fuelPer100Blocks", def.fuelPer100Blocks());
        Powertrain p = def.powertrain();
        JsonObject t = new JsonObject();
        t.addProperty("massKg", p.massKg());
        t.addProperty("wheelRadius", p.wheelRadius());
        t.add("rpm", array(p.rpm()));
        t.add("torqueNm", array(p.torqueNm()));
        t.addProperty("idleRpm", p.idleRpm());
        t.addProperty("redlineRpm", p.redlineRpm());
        t.addProperty("launchRpm", p.launchRpm());
        t.addProperty("shiftUpRpm", p.shiftUpRpm());
        t.addProperty("shiftDownRpm", p.shiftDownRpm());
        t.add("gearRatios", array(p.gearRatios()));
        t.addProperty("reverseRatio", p.reverseRatio());
        t.addProperty("efficiency", p.efficiency());
        t.addProperty("engineBrakeNm", p.engineBrakeNm());
        t.addProperty("dragArea", p.dragArea());
        t.addProperty("rollingResistance", p.rollingResistance());
        t.addProperty("driveWeightFraction", p.driveWeightFraction());
        t.addProperty("brakeG", p.brakeG());
        t.addProperty("tyreMu", p.tyreMu());
        t.addProperty("frontWeight", p.frontWeight());
        t.addProperty("frontDriveShare", p.frontDriveShare());
        t.addProperty("frontCornering", p.frontCornering());
        t.addProperty("rearCornering", p.rearCornering());
        t.addProperty("yawGyration", p.yawGyration());
        o.add("powertrain", t);
        return GSON.toJson(o);
    }

    /**
     * Applica il JSON al veicolo base: i campi presenti sostituiscono quelli predefiniti.
     *
     * @throws IllegalArgumentException se il JSON non è valido o i valori non sono ammessi
     */
    public static VehicleDef overlay(VehicleDef base, String json) {
        JsonObject o = parse(json);
        Powertrain bp = base.powertrain();
        JsonObject t = o.has("powertrain") ? object(o.get("powertrain"), "powertrain") : new JsonObject();
        Powertrain p;
        try {
            p = new Powertrain(
                    num(t, "massKg", bp.massKg(), true), num(t, "wheelRadius", bp.wheelRadius(), true),
                    arr(t, "rpm", bp.rpm()), arr(t, "torqueNm", bp.torqueNm()),
                    num(t, "idleRpm", bp.idleRpm(), true), num(t, "redlineRpm", bp.redlineRpm(), true),
                    num(t, "launchRpm", bp.launchRpm(), true), num(t, "shiftUpRpm", bp.shiftUpRpm(), true),
                    num(t, "shiftDownRpm", bp.shiftDownRpm(), true), arr(t, "gearRatios", bp.gearRatios()),
                    num(t, "reverseRatio", bp.reverseRatio(), true), num(t, "efficiency", bp.efficiency(), true),
                    num(t, "engineBrakeNm", bp.engineBrakeNm(), false), num(t, "dragArea", bp.dragArea(), true),
                    num(t, "rollingResistance", bp.rollingResistance(), true),
                    num(t, "driveWeightFraction", bp.driveWeightFraction(), true),
                    num(t, "brakeG", bp.brakeG(), true), num(t, "tyreMu", bp.tyreMu(), true),
                    num(t, "frontWeight", bp.frontWeight(), true),
                    num(t, "frontDriveShare", bp.frontDriveShare(), false),
                    num(t, "frontCornering", bp.frontCornering(), true),
                    num(t, "rearCornering", bp.rearCornering(), true), num(t, "yawGyration", bp.yawGyration(), true));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("powertrain: " + e.getMessage(), e);
        }
        validate(p);
        int seats = (int) num(o, "seats", base.seats(), true);
        return new VehicleDef(base.id(), seats, num(o, "maxSpeed", base.maxSpeed(), true),
                num(o, "maxSteerDeg", base.maxSteerDeg(), true), num(o, "steerRateDeg", base.steerRateDeg(), true),
                num(o, "wheelBase", base.wheelBase(), true), num(o, "fuelCapacity", base.fuelCapacity(), true),
                num(o, "fuelPer100Blocks", base.fuelPer100Blocks(), false), p);
    }

    private static void validate(Powertrain p) {
        for (int i = 1; i < p.rpm().length; i++) {
            if (!(p.rpm()[i] > p.rpm()[i - 1])) throw new IllegalArgumentException("rpm deve essere crescente");
        }
        if (!(p.redlineRpm() > p.idleRpm())) throw new IllegalArgumentException("redlineRpm deve superare idleRpm");
        if (!(p.shiftUpRpm() > p.shiftDownRpm())) {
            throw new IllegalArgumentException("shiftUpRpm deve superare shiftDownRpm");
        }
        if (p.efficiency() > 1) throw new IllegalArgumentException("efficiency è tra 0 e 1");
        if (p.frontWeight() >= 1) throw new IllegalArgumentException("frontWeight è tra 0 e 1 (esclusi)");
        if (p.frontDriveShare() > 1) throw new IllegalArgumentException("frontDriveShare è tra 0 e 1");
        for (double r : p.gearRatios()) if (!(r > 0)) throw new IllegalArgumentException("gearRatios deve essere positivo");
    }

    // ---- fondi ----

    public static String surfacesToJson(List<Surface> surfaces) {
        JsonObject o = new JsonObject();
        for (Surface s : surfaces) {
            JsonObject e = new JsonObject();
            e.addProperty("grip", s.gripFactor());
            e.addProperty("speed", s.speedFactor());
            e.addProperty("roll", s.rollFactor());
            o.add(s.id(), e);
        }
        return GSON.toJson(o);
    }

    /** I fondi descritti dal JSON, con i campi mancanti presi da quello predefinito dello stesso id. */
    public static List<Surface> parseSurfaces(String json) {
        JsonObject o = parse(json);
        List<Surface> out = new ArrayList<>();
        for (var entry : o.entrySet()) {
            Surface base = Surface.get(entry.getKey());
            JsonObject e = object(entry.getValue(), entry.getKey());
            out.add(new Surface(entry.getKey(), num(e, "grip", base.gripFactor(), true),
                    num(e, "speed", base.speedFactor(), true), num(e, "roll", base.rollFactor(), true)));
        }
        return out;
    }

    // ---- aiuti ----

    private static JsonArray array(double[] values) {
        JsonArray a = new JsonArray();
        for (double v : values) a.add(v);
        return a;
    }

    private static JsonObject parse(String json) {
        try {
            return object(JsonParser.parseString(json), "radice");
        } catch (JsonParseException ex) {
            throw new IllegalArgumentException("JSON non valido: " + ex.getMessage(), ex);
        }
    }

    private static JsonObject object(JsonElement e, String name) {
        if (e == null || !e.isJsonObject()) throw new IllegalArgumentException(name + " deve essere un oggetto");
        return e.getAsJsonObject();
    }

    private static double num(JsonObject o, String key, double fallback, boolean positive) {
        if (!o.has(key)) return fallback;
        double v;
        try {
            v = o.get(key).getAsDouble();
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(key + " deve essere un numero");
        }
        if (!Double.isFinite(v) || v < 0 || (positive && v == 0)) {
            throw new IllegalArgumentException(key + " non valido: " + v);
        }
        return v;
    }

    private static double[] arr(JsonObject o, String key, double[] fallback) {
        if (!o.has(key)) return fallback;
        if (!o.get(key).isJsonArray()) throw new IllegalArgumentException(key + " deve essere una lista");
        JsonArray a = o.getAsJsonArray(key);
        double[] out = new double[a.size()];
        for (int i = 0; i < out.length; i++) {
            try {
                out[i] = a.get(i).getAsDouble();
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException(key + "[" + i + "] deve essere un numero");
            }
            if (!Double.isFinite(out[i]) || out[i] < 0) {
                throw new IllegalArgumentException(key + "[" + i + "] non valido");
            }
        }
        return out;
    }
}
