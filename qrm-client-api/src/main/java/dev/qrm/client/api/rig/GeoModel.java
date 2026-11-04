package dev.qrm.client.api.rig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Un modello Bedrock ({@code *.geo.json}, quello che Blockbench esporta in formato GeckoLib/Bedrock) letto
 * direttamente: rig, mesh di ogni osso, locator e dimensioni della texture.
 *
 * <p>Tutte le coordinate sono in unità del modello (16 = 1 blocco) e già nello spazio di gioco: il file Bedrock ha
 * l'asse X invertito, quindi pivot, cubi e locator vengono specchiati su X. Il davanti del modello è -Z.
 *
 * <p>Supporta solo ciò che serve ai veicoli: cubi con UV a scatola ({@code "uv": [u, v]}) e {@code inflate}. UV per
 * faccia, rotazione di cubi o ossa e {@code mirror} sono rifiutati con un errore che nomina l'osso.
 */
public record GeoModel(Rig rig, Map<String, PartMesh> meshes, Map<String, float[]> locators, int textureWidth,
        int textureHeight) {

    public GeoModel {
        meshes = Map.copyOf(meshes);
        locators = Map.copyOf(locators);
    }

    /** Legge il modello. File non valido o funzione non supportata: IllegalArgumentException. */
    public static GeoModel parse(Reader json) {
        JsonObject geometry;
        try {
            JsonObject root = JsonParser.parseReader(json).getAsJsonObject();
            geometry = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Modello Bedrock non leggibile: " + e.getMessage(), e);
        }
        JsonObject description = geometry.getAsJsonObject("description");
        if (description == null || !description.has("texture_width") || !description.has("texture_height")) {
            throw new IllegalArgumentException("Modello senza texture_width/texture_height");
        }
        int tw = description.get("texture_width").getAsInt();
        int th = description.get("texture_height").getAsInt();
        if (tw <= 0 || th <= 0) throw new IllegalArgumentException("Dimensioni della texture non valide");

        Map<String, Rig.Bone> bones = new LinkedHashMap<>();
        Map<String, PartMesh> meshes = new LinkedHashMap<>();
        Map<String, float[]> locators = new LinkedHashMap<>();
        JsonArray array = geometry.has("bones") ? geometry.getAsJsonArray("bones") : new JsonArray();
        for (JsonElement el : array) {
            JsonObject b = el.getAsJsonObject();
            String name = b.get("name").getAsString();
            try {
                if (bones.containsKey(name)) throw new IllegalArgumentException("osso duplicato");
                if (b.has("rotation") && nonZero(floats(b.getAsJsonArray("rotation"), 3, "rotation"))) {
                    throw new IllegalArgumentException("rotazione dell'osso non supportata");
                }
                float[] pivot = b.has("pivot") ? floats(b.getAsJsonArray("pivot"), 3, "pivot") : new float[3];
                pivot[0] = -pivot[0];
                JsonElement parent = b.get("parent");
                String parentName = parent == null || parent.isJsonNull() ? null : parent.getAsString();
                List<PartMesh.Quad> quads = new ArrayList<>();
                if (b.has("cubes")) {
                    for (JsonElement c : b.getAsJsonArray("cubes")) cube(c.getAsJsonObject(), tw, th, quads);
                }
                bones.put(name, new Rig.Bone(name, parentName, pivot, !quads.isEmpty()));
                if (!quads.isEmpty()) meshes.put(name, new PartMesh(quads));
                if (b.has("locators")) {
                    for (Map.Entry<String, JsonElement> loc : b.getAsJsonObject("locators").entrySet()) {
                        JsonElement v = loc.getValue();
                        JsonArray pos = v.isJsonObject() ? v.getAsJsonObject().getAsJsonArray("offset")
                                : v.getAsJsonArray();
                        float[] p = floats(pos, 3, "locator " + loc.getKey());
                        p[0] = -p[0];
                        locators.put(loc.getKey(), p);
                    }
                }
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Osso '" + name + "': " + e.getMessage(), e);
            }
        }
        return new GeoModel(new Rig(ordered(bones), null), meshes, locators, tw, th);
    }

    private static void cube(JsonObject c, int tw, int th, List<PartMesh.Quad> out) {
        if (c.has("rotation") || c.has("pivot")) throw new IllegalArgumentException("rotazione dei cubi non supportata");
        if (c.has("mirror") && c.get("mirror").getAsBoolean()) throw new IllegalArgumentException("mirror non supportato");
        if (!c.has("uv") || !c.get("uv").isJsonArray()) {
            throw new IllegalArgumentException("sono supportati solo gli UV a scatola ([u, v])");
        }
        float[] origin = floats(c.getAsJsonArray("origin"), 3, "origin");
        float[] size = floats(c.getAsJsonArray("size"), 3, "size");
        float[] uv = floats(c.getAsJsonArray("uv"), 2, "uv");
        float inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : 0f;
        float w = size[0], h = size[1], d = size[2], u = uv[0], v = uv[1];

        // Spazio di gioco: X specchiata.
        float[] from = {-(origin[0] + w) - inflate, origin[1] - inflate, origin[2] - inflate};
        float[] to = {-origin[0] + inflate, origin[1] + h + inflate, origin[2] + d + inflate};

        for (MeshBuilder.Face face : MeshBuilder.Face.values()) {
            float[] px = switch (face) {
                case EAST -> new float[] {u, v + d, u + d, v + d + h};
                case NORTH -> new float[] {u + d, v + d, u + d + w, v + d + h};
                case WEST -> new float[] {u + d + w, v + d, u + 2 * d + w, v + d + h};
                case SOUTH -> new float[] {u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h};
                case UP -> new float[] {u + d, v, u + d + w, v + d};
                case DOWN -> new float[] {u + d + w, v + d, u + d + 2 * w, v};
            };
            List<PartMesh.Vertex> vertices = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) {
                int[] k = face.corners[i];
                float x = k[0] == 0 ? from[0] : to[0];
                float y = k[1] == 0 ? from[1] : to[1];
                float z = k[2] == 0 ? from[2] : to[2];
                float pu = (i <= 1 ? px[0] : px[2]) / tw;
                float pv = (i == 0 || i == 3 ? px[1] : px[3]) / th;
                vertices.add(new PartMesh.Vertex(x, y, z, pu, pv));
            }
            out.add(new PartMesh.Quad(vertices, face.nx, face.ny, face.nz));
        }
    }

    /** Genitore prima del figlio, mantenendo l'ordine del file dove possibile. */
    private static List<Rig.Bone> ordered(Map<String, Rig.Bone> bones) {
        for (Rig.Bone b : bones.values()) {
            if (b.parent() != null && !bones.containsKey(b.parent())) {
                throw new IllegalArgumentException("Osso '" + b.name() + "': genitore sconosciuto '" + b.parent() + "'");
            }
        }
        List<Rig.Bone> out = new ArrayList<>(bones.size());
        Set<String> placed = new HashSet<>();
        while (out.size() < bones.size()) {
            boolean progress = false;
            for (Rig.Bone b : bones.values()) {
                if (placed.contains(b.name())) continue;
                if (b.parent() == null || placed.contains(b.parent())) {
                    out.add(b);
                    placed.add(b.name());
                    progress = true;
                }
            }
            if (!progress) throw new IllegalArgumentException("Ciclo tra le ossa del modello");
        }
        return out;
    }

    private static boolean nonZero(float[] a) {
        for (float f : a) if (f != 0f) return true;
        return false;
    }

    private static float[] floats(JsonArray a, int n, String what) {
        if (a == null || a.size() != n) throw new IllegalArgumentException("'" + what + "' deve avere " + n + " valori");
        float[] r = new float[n];
        for (int i = 0; i < n; i++) r[i] = a.get(i).getAsFloat();
        return r;
    }
}
