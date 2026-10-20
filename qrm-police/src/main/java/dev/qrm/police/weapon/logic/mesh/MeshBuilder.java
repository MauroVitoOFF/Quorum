package dev.qrm.police.weapon.logic.mesh;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Converte un modello Java esportato da Blockbench (elementi con {@code from}/{@code to}, rotazione opzionale e
 * facce con UV) nei quad di una {@link PartMesh}, senza passare dal baking dei modelli di Minecraft.
 *
 * <p>Riproduce le regole di Minecraft: ordine dei vertici per faccia, UV del JSON in 0..16 riferite all'intera
 * texture (indipendenti da {@code texture_size}), rotazione della faccia a passi di 90° che sposta gli angoli UV,
 * rotazione dell'elemento destrorsa attorno a {@code origin} (angolo qualsiasi, non solo ±22,5/45) con
 * {@code rescale} facoltativo.
 */
public final class MeshBuilder {

    private MeshBuilder() {}

    /** Le sei facce: nome nel JSON, normale, e per ognuno dei 4 vertici quali estremi (0 = from, 1 = to) usare. */
    private enum Face {
        DOWN("down", 0, -1, 0, new int[][] {{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}}),
        UP("up", 0, 1, 0, new int[][] {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}}),
        NORTH("north", 0, 0, -1, new int[][] {{1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}}),
        SOUTH("south", 0, 0, 1, new int[][] {{0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}}),
        WEST("west", -1, 0, 0, new int[][] {{0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}}),
        EAST("east", 1, 0, 0, new int[][] {{1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}});

        final String key;
        final float nx, ny, nz;
        final int[][] corners;

        Face(String key, float nx, float ny, float nz, int[][] corners) {
            this.key = key;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.corners = corners;
        }
    }

    /** Legge il modello e ne calcola i quad. JSON non valido o elementi incompleti: IllegalArgumentException. */
    public static PartMesh parse(Reader json) {
        JsonObject root;
        try {
            root = JsonParser.parseReader(json).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Modello non leggibile: " + e.getMessage(), e);
        }
        List<PartMesh.Quad> quads = new ArrayList<>();
        JsonArray elements = root.has("elements") ? root.getAsJsonArray("elements") : new JsonArray();
        for (int i = 0; i < elements.size(); i++) {
            try {
                element(elements.get(i).getAsJsonObject(), quads);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Elemento " + i + " non valido: " + e.getMessage(), e);
            }
        }
        return new PartMesh(quads);
    }

    private static void element(JsonObject e, List<PartMesh.Quad> out) {
        float[] from = vec3(e, "from");
        float[] to = vec3(e, "to");
        Rotation rot = e.has("rotation") ? Rotation.of(e.getAsJsonObject("rotation")) : null;
        JsonObject faces = e.has("faces") ? e.getAsJsonObject("faces") : new JsonObject();
        for (Face face : Face.values()) {
            if (!faces.has(face.key)) continue;
            JsonObject f = faces.getAsJsonObject(face.key);
            float[] uv = f.has("uv") ? floats(f.getAsJsonArray("uv"), 4, "uv") : defaultUv(face, from, to);
            int shift = Math.floorMod(f.has("rotation") ? f.get("rotation").getAsInt() : 0, 360);
            if (shift % 90 != 0) throw new IllegalArgumentException("Rotazione UV non multipla di 90: " + shift);
            shift /= 90;
            List<PartMesh.Vertex> vertices = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) {
                int[] c = face.corners[i];
                float[] p = {c[0] == 0 ? from[0] : to[0], c[1] == 0 ? from[1] : to[1], c[2] == 0 ? from[2] : to[2]};
                if (rot != null) p = rot.applyPoint(p);
                // Come CuboidFace.getU/getV: il vertice i prende l'angolo UV (i + rotazione/90) mod 4.
                int k = (i + shift) % 4;
                float u = (k == 0 || k == 1 ? uv[0] : uv[2]) / 16f;
                float v = (k == 0 || k == 3 ? uv[1] : uv[3]) / 16f;
                vertices.add(new PartMesh.Vertex(p[0], p[1], p[2], u, v));
            }
            float[] n = {face.nx, face.ny, face.nz};
            if (rot != null) n = rot.applyNormal(n);
            out.add(new PartMesh.Quad(vertices, n[0], n[1], n[2]));
        }
    }

    /** UV predefinite di Minecraft quando la faccia non le indica (dalla proiezione del cubo). */
    private static float[] defaultUv(Face face, float[] from, float[] to) {
        return switch (face) {
            case DOWN -> new float[] {from[0], 16 - to[2], to[0], 16 - from[2]};
            case UP -> new float[] {from[0], from[2], to[0], to[2]};
            case NORTH -> new float[] {16 - to[0], 16 - to[1], 16 - from[0], 16 - from[1]};
            case SOUTH -> new float[] {from[0], 16 - to[1], to[0], 16 - from[1]};
            case WEST -> new float[] {from[2], 16 - to[1], to[2], 16 - from[1]};
            case EAST -> new float[] {16 - to[2], 16 - to[1], 16 - from[2], 16 - from[1]};
        };
    }

    private static float[] vec3(JsonObject o, String key) {
        if (!o.has(key)) throw new IllegalArgumentException("manca '" + key + "'");
        return floats(o.getAsJsonArray(key), 3, key);
    }

    private static float[] floats(JsonArray a, int n, String what) {
        if (a.size() != n) throw new IllegalArgumentException("'" + what + "' deve avere " + n + " valori");
        float[] r = new float[n];
        for (int i = 0; i < n; i++) {
            JsonElement el = a.get(i);
            r[i] = el.getAsFloat();
        }
        return r;
    }

    /** Rotazione di un elemento su un solo asse, destrorsa (come JOML e Minecraft), attorno a un'origine. */
    private record Rotation(float[] origin, int axis, double cos, double sin, double rescale) {

        static Rotation of(JsonObject r) {
            float[] origin = vec3(r, "origin");
            String axisName = r.get("axis").getAsString();
            int axis = switch (axisName) {
                case "x" -> 0;
                case "y" -> 1;
                case "z" -> 2;
                default -> throw new IllegalArgumentException("asse sconosciuto: " + axisName);
            };
            double angle = Math.toRadians(r.has("angle") ? r.get("angle").getAsDouble() : 0);
            double cos = Math.cos(angle);
            boolean rescale = r.has("rescale") && r.get("rescale").getAsBoolean();
            // rescale: Minecraft allarga le due direzioni perpendicolari all'asse di 1/cos, così il cubo ruotato
            // copre ancora l'intero lato (vale per angoli fino a 45°).
            double scale = rescale && Math.abs(cos) > 1e-6 ? 1 / Math.abs(cos) : 1;
            return new Rotation(origin, axis, cos, Math.sin(angle), scale);
        }

        float[] applyPoint(float[] p) {
            double[] d = {p[0] - origin[0], p[1] - origin[1], p[2] - origin[2]};
            for (int i = 0; i < 3; i++) if (i != axis) d[i] *= rescale;
            double[] r = rotate(d);
            return new float[] {(float) (r[0] + origin[0]), (float) (r[1] + origin[1]), (float) (r[2] + origin[2])};
        }

        float[] applyNormal(float[] n) {
            double[] r = rotate(new double[] {n[0], n[1], n[2]});
            double len = Math.sqrt(r[0] * r[0] + r[1] * r[1] + r[2] * r[2]);
            return new float[] {(float) (r[0] / len), (float) (r[1] / len), (float) (r[2] / len)};
        }

        private double[] rotate(double[] d) {
            double x = d[0], y = d[1], z = d[2];
            return switch (axis) {
                case 0 -> new double[] {x, y * cos - z * sin, y * sin + z * cos};
                case 1 -> new double[] {x * cos + z * sin, y, -x * sin + z * cos};
                default -> new double[] {x * cos - y * sin, x * sin + y * cos, z};
            };
        }
    }
}
