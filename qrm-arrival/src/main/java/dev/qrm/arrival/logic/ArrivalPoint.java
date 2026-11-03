package dev.qrm.arrival.logic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Dove si risveglia il personaggio: dimensione (es. {@code minecraft:overworld}), coordinate e direzione. */
public record ArrivalPoint(String dimension, double x, double y, double z, float yaw) {
    public ArrivalPoint {
        if (dimension == null || dimension.isBlank()) throw new IllegalArgumentException("dimension is blank");
    }

    /** @throws IllegalArgumentException se il JSON è rotto o manca un campo */
    public static ArrivalPoint parse(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            return new ArrivalPoint(o.get("dimension").getAsString(), o.get("x").getAsDouble(),
                    o.get("y").getAsDouble(), o.get("z").getAsDouble(), o.get("yaw").getAsFloat());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("invalid arrival point: " + e.getMessage(), e);
        }
    }

    public String toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("dimension", dimension);
        o.addProperty("x", x);
        o.addProperty("y", y);
        o.addProperty("z", z);
        o.addProperty("yaw", yaw);
        return o.toString();
    }
}
