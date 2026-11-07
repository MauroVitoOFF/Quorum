package dev.qrm.vehicles.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qrm.client.api.rig.BonePose;
import dev.qrm.client.api.rig.BoneTransform;
import dev.qrm.client.api.rig.GeoModel;
import dev.qrm.client.api.rig.PartMesh;
import dev.qrm.client.api.rig.Rig;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4d;
import org.joml.Vector3d;

/**
 * Le hitbox di un veicolo, disegnate in Blockbench come cubi di due tipi di osso:
 * <ul>
 *   <li>{@code hitbox}: l'ingombro solido. Il giocatore non lo attraversa.</li>
 *   <li>{@code hit_<osso>}: una zona di interazione. Se nel modello c'è l'osso {@code <osso>} la zona è la sua
 *       geometria vera (i suoi poligoni e quelli dei suoi figli), che segue la posa dell'osso; i cubi della zona
 *       servono solo se quell'osso non esiste o è vuoto (per esempio un sedile disegnato a parte).</li>
 * </ul>
 * Le scatole seguono il veicolo e ruotano col muso (non con beccheggio e rollio). Coordinate come nel modello: blocchi,
 * davanti = -Z, destra = +X, origine al centro del veicolo a terra. Nessuna dipendenza da Minecraft.
 */
public final class VehicleHitboxes {

    public static final String SOLID_BONE = "hitbox";
    public static final String ZONE_PREFIX = "hit_";

    /** Una scatola allineata agli assi del modello: centro e semi-dimensioni in blocchi. */
    public record Box(double cx, double cy, double cz, double hx, double hy, double hz) {}

    /** Zona colpita da un raggio: nome della zona e distanza. */
    public record Hit(String zone, double distance) {}

    /** Un osso del file: genitore e perno (unità del modello, X già specchiata come nel rig). */
    public record Node(String parent, float[] pivot) {}

    public static final VehicleHitboxes NONE = new VehicleHitboxes(List.of(), Map.of());

    private final List<Box> solid;
    private final Map<String, List<Box>> zones;
    /** Osso genitore di ogni zona: le zone fatte di cubi seguono la posa di quell'osso. */
    private final Map<String, String> zoneParents;
    private final Map<String, Node> nodes;
    /** Per le zone che sono un osso del modello: i quad dell'osso e dei figli (4 vertici x,y,z in blocchi = 12 valori). */
    private final Map<String, double[][]> meshes;

    public VehicleHitboxes(List<Box> solid, Map<String, List<Box>> zones) {
        this(solid, zones, Map.of(), Map.of(), Map.of());
    }

    public VehicleHitboxes(List<Box> solid, Map<String, List<Box>> zones, Map<String, String> zoneParents,
            Map<String, Node> nodes, Map<String, double[][]> meshes) {
        this.solid = List.copyOf(solid);
        Map<String, List<Box>> copy = new LinkedHashMap<>();
        zones.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        this.zones = java.util.Collections.unmodifiableMap(copy);
        this.zoneParents = Map.copyOf(zoneParents);
        this.nodes = Map.copyOf(nodes);
        this.meshes = Map.copyOf(meshes);
    }

    /**
     * Raggio del "fascio" del mirino del giocatore (blocchi): le parti vere sono piccole (una maniglia, una chiave) e si
     * prendono anche passando di striscio.
     */
    public static final double AIM_MARGIN = 0.08;
    /** Il server è più largo: tra il fotogramma del client e il suo tick la parte si è mossa di poco. */
    public static final double SERVER_MARGIN = 0.25;

    public List<Box> solid() {
        return solid;
    }

    public Map<String, List<Box>> zones() {
        return zones;
    }

    public boolean hasSolid() {
        return !solid.isEmpty();
    }

    /** Il nome dell'osso da illuminare per una zona {@code hit_<osso>}. */
    public static String boneOfZone(String zone) {
        return zone.startsWith(ZONE_PREFIX) ? zone.substring(ZONE_PREFIX.length()) : zone;
    }

    /** L'osso da illuminare puntando una zona (come {@link #boneOfZone}). */
    public String highlightBone(String zone) {
        return boneOfZone(zone);
    }

    // ---- lettura del geo di Blockbench ----

    /** Legge le ossa {@code hitbox} e {@code hit_*} da un file Bedrock; senza queste ossa restituisce {@link #NONE}. */
    public static VehicleHitboxes parse(Reader json) {
        String text;
        try {
            text = new String(readAll(json));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        JsonObject geometry;
        try {
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            geometry = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Modello Bedrock non leggibile: " + e.getMessage(), e);
        }
        List<Box> solid = new ArrayList<>();
        Map<String, List<Box>> zones = new LinkedHashMap<>();
        Map<String, String> zoneParents = new LinkedHashMap<>();
        Map<String, Node> nodes = new LinkedHashMap<>();
        if (!geometry.has("bones")) return NONE;
        for (JsonElement el : geometry.getAsJsonArray("bones")) {
            JsonObject bone = el.getAsJsonObject();
            if (!bone.has("name")) continue;
            String name = bone.get("name").getAsString();
            float[] pivot = {0, 0, 0};
            if (bone.has("pivot") && bone.getAsJsonArray("pivot").size() == 3) {
                JsonArray pv = bone.getAsJsonArray("pivot");
                pivot = new float[] {-pv.get(0).getAsFloat(), pv.get(1).getAsFloat(), pv.get(2).getAsFloat()};
            }
            String parent = bone.has("parent") ? bone.get("parent").getAsString() : null;
            nodes.put(name, new Node(parent, pivot));
            if (parent != null && isZoneBone(name)) zoneParents.put(name, parent);
            boolean isSolid = name.equals(SOLID_BONE);
            if (!isSolid && !isZoneBone(name)) continue;
            List<Box> boxes = new ArrayList<>();
            if (bone.has("cubes")) {
                for (JsonElement c : bone.getAsJsonArray("cubes")) boxes.add(box(name, c.getAsJsonObject()));
            }
            if (isSolid) solid.addAll(boxes);
            else if (!boxes.isEmpty()) zones.computeIfAbsent(name, k -> new ArrayList<>()).addAll(boxes);
        }
        return new VehicleHitboxes(solid, zones, zoneParents, nodes, meshesOf(text, zones.keySet()));
    }

    private static char[] readAll(Reader r) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
        char[] out = new char[sb.length()];
        sb.getChars(0, sb.length(), out, 0);
        return out;
    }

    /**
     * Per ogni zona {@code hit_<osso>} con un osso {@code <osso>} che ha geometria: i quad di quell'osso e dei suoi
     * discendenti, in blocchi. Un modello che il lettore del gioco non accetta (file di prova) non dà nessuna mesh.
     */
    private static Map<String, double[][]> meshesOf(String text, Iterable<String> zoneNames) {
        GeoModel model;
        try {
            model = GeoModel.parse(new StringReader(text));
        } catch (RuntimeException e) {
            return Map.of();
        }
        Map<String, double[][]> out = new LinkedHashMap<>();
        for (String zone : zoneNames) {
            String bone = boneOfZone(zone);
            if (model.rig().bone(bone).isEmpty()) continue;
            List<double[]> quads = new ArrayList<>();
            for (Rig.Bone b : model.rig().bones()) {
                if (!descendsFrom(model.rig(), b.name(), bone)) continue;
                PartMesh mesh = model.meshes().get(b.name());
                if (mesh == null) continue;
                for (PartMesh.Quad q : mesh.quads()) {
                    double[] v = new double[12];
                    for (int i = 0; i < 4; i++) {
                        v[i * 3] = q.vertices().get(i).x() / 16.0;
                        v[i * 3 + 1] = q.vertices().get(i).y() / 16.0;
                        v[i * 3 + 2] = q.vertices().get(i).z() / 16.0;
                    }
                    quads.add(v);
                }
            }
            if (!quads.isEmpty()) out.put(zone, quads.toArray(new double[0][]));
        }
        return out;
    }

    private static boolean descendsFrom(Rig rig, String bone, String ancestor) {
        String cur = bone;
        for (int guard = 0; cur != null && guard < 64; guard++) {
            if (cur.equals(ancestor)) return true;
            cur = rig.bone(cur).map(Rig.Bone::parent).orElse(null);
        }
        return false;
    }

    /** Un osso di zona ({@code hit_<osso>}): non si disegna e non fa parte del rig. */
    public static boolean isZoneBone(String name) {
        return name.startsWith(ZONE_PREFIX) && name.length() > ZONE_PREFIX.length();
    }

    private static Box box(String bone, JsonObject cube) {
        try {
            double[] o = triple(cube.getAsJsonArray("origin"));
            double[] s = triple(cube.getAsJsonArray("size"));
            // Pixel Blockbench -> blocchi; l'asse X del file Bedrock è specchiato rispetto al gioco.
            double cx = -(o[0] + s[0] / 2) / 16;
            return new Box(cx, (o[1] + s[1] / 2) / 16, (o[2] + s[2] / 2) / 16, s[0] / 32, s[1] / 32, s[2] / 32);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Hitbox '" + bone + "': " + e.getMessage(), e);
        }
    }

    private static double[] triple(JsonArray a) {
        if (a == null || a.size() != 3) throw new IllegalArgumentException("origin/size non validi");
        return new double[] {a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    // ---- selezione ----

    /** Ingombro a riposo di una zona nello spazio del modello (blocchi): {minX, minY, minZ, maxX, maxY, maxZ}, o null. */
    public double[] bounds(String zone) {
        double[] r = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE,
                -Double.MAX_VALUE};
        boolean any = false;
        double[][] mesh = meshes.get(zone);
        if (mesh != null) {
            for (double[] q : mesh) {
                for (int i = 0; i < 4; i++) {
                    any = true;
                    for (int k = 0; k < 3; k++) {
                        r[k] = Math.min(r[k], q[i * 3 + k]);
                        r[k + 3] = Math.max(r[k + 3], q[i * 3 + k]);
                    }
                }
            }
        } else {
            for (Box b : zones.getOrDefault(zone, List.of())) {
                any = true;
                double[] c = {b.cx(), b.cy(), b.cz()};
                double[] h = {b.hx(), b.hy(), b.hz()};
                for (int k = 0; k < 3; k++) {
                    r[k] = Math.min(r[k], c[k] - h[k]);
                    r[k + 3] = Math.max(r[k + 3], c[k] + h[k]);
                }
            }
        }
        return any ? r : null;
    }

    /** Il centro a riposo di una zona (blocchi, spazio del modello), o null se non c'è. */
    public double[] center(String zone) {
        double[] b = bounds(zone);
        return b == null ? null : new double[] {(b[0] + b[3]) / 2, (b[1] + b[4]) / 2, (b[2] + b[5]) / 2};
    }

    /**
     * Il riquadro di selezione di una zona: la scatola che la racchiude, ruotata con l'osso a cui è legata (come la
     * zona). Dodici segmenti {@code {x1,y1,z1,x2,y2,z2}} nello spazio del modello; vuoto se la parte non c'è.
     */
    public List<double[]> selection(String zone, Map<String, BoneTransform> pose) {
        double[] b = bounds(zone);
        if (b == null) return List.of();
        double pad = 0.012;
        Box box = new Box((b[0] + b[3]) / 2, (b[1] + b[4]) / 2, (b[2] + b[5]) / 2, (b[3] - b[0]) / 2 + pad,
                (b[4] - b[1]) / 2 + pad, (b[5] - b[2]) / 2 + pad);
        String poseBone = meshes.containsKey(zone) ? boneOfZone(zone) : zoneParents.get(zone);
        Matrix4d m = null;
        if (poseBone != null && !pose.isEmpty()) {
            m = world(poseBone, pose, 0);
            if (Math.abs(m.determinant()) < 1e-9) m = null;
        }
        List<double[]> out = new ArrayList<>();
        addBox(out, box, m);
        return out;
    }

    // ---- urto contro i blocchi ----

    /**
     * Quanto spostare il veicolo perché la sua scocca (le scatole {@code hitbox}, ruotate col muso) non entri nei blocchi
     * dati ({@code {minX, minY, minZ, maxX, maxY, maxZ}} nel mondo). Restituisce {@code {dx, dz, nx, nz}}, con la
     * direzione di spinta unitaria, o {@code null} se non si tocca niente. I blocchi bassi (fino a {@code stepUp} sopra
     * i piedi) li sale la ruota e non contano.
     */
    public double[] bodyPush(double px, double py, double pz, double headingDeg, List<double[]> obstacles, double stepUp) {
        double th = theta(headingDeg);
        double c = Math.cos(th);
        double s = Math.sin(th);
        double[] e1 = {c, -s};
        double[] e2 = {s, c};
        double[][] axes = {{1, 0}, {0, 1}, e1, e2};
        double[] best = null;
        double bestDepth = Double.MAX_VALUE;
        for (Box b : solid) {
            double bottom = py + b.cy() - b.hy();
            double top = py + b.cy() + b.hy();
            double wx = px + b.cx() * c + b.cz() * s;
            double wz = pz - b.cx() * s + b.cz() * c;
            for (double[] o : obstacles) {
                if (o[4] <= bottom + stepUp || o[1] >= top) continue;
                double ax = (o[0] + o[3]) / 2;
                double az = (o[2] + o[5]) / 2;
                double ahx = (o[3] - o[0]) / 2;
                double ahz = (o[5] - o[2]) / 2;
                double minDepth = Double.MAX_VALUE;
                double nx = 0;
                double nz = 0;
                boolean apart = false;
                for (double[] n : axes) {
                    double rBox = b.hx() * Math.abs(n[0] * e1[0] + n[1] * e1[1]) + b.hz() * Math.abs(n[0] * e2[0] + n[1] * e2[1]);
                    double rObs = ahx * Math.abs(n[0]) + ahz * Math.abs(n[1]);
                    double d = (wx - ax) * n[0] + (wz - az) * n[1];
                    double depth = rBox + rObs - Math.abs(d);
                    if (depth <= 1e-6) {
                        apart = true;
                        break;
                    }
                    if (depth < minDepth) {
                        minDepth = depth;
                        double sign = d >= 0 ? 1 : -1;
                        nx = n[0] * sign;
                        nz = n[1] * sign;
                    }
                }
                if (apart) continue;
                if (minDepth < bestDepth) {
                    bestDepth = minDepth;
                    best = new double[] {nx * minDepth, nz * minDepth, nx, nz};
                }
            }
        }
        return best;
    }

    /**
     * Come {@link #bodyPush(double, double, double, double, List, double)} ma contro la scocca di un altro veicolo
     * ({@code other}, con i piedi in {@code ox, oy, oz} e il muso a {@code otherHeadingDeg}): entrambe le scocche sono
     * scatole ruotate, quindi il test di separazione usa gli assi di tutte e due. Restituisce {@code {dx, dz, nx, nz}}
     * per spostare questo veicolo fuori dall'altro (la normale punta dall'altro verso questo), o {@code null}.
     */
    public double[] bodyPush(double px, double py, double pz, double headingDeg, VehicleHitboxes other, double ox,
            double oy, double oz, double otherHeadingDeg) {
        double th = theta(headingDeg);
        double c = Math.cos(th);
        double s = Math.sin(th);
        double tho = theta(otherHeadingDeg);
        double co = Math.cos(tho);
        double so = Math.sin(tho);
        double[] a1 = {c, -s};
        double[] a2 = {s, c};
        double[] b1 = {co, -so};
        double[] b2 = {so, co};
        double[][] axes = {a1, a2, b1, b2};
        double[] best = null;
        double bestDepth = Double.MAX_VALUE;
        for (Box a : solid) {
            double aBottom = py + a.cy() - a.hy();
            double aTop = py + a.cy() + a.hy();
            double ax = px + a.cx() * c + a.cz() * s;
            double az = pz - a.cx() * s + a.cz() * c;
            for (Box b : other.solid) {
                double bBottom = oy + b.cy() - b.hy();
                double bTop = oy + b.cy() + b.hy();
                if (bTop <= aBottom || bBottom >= aTop) continue;
                double bx = ox + b.cx() * co + b.cz() * so;
                double bz = oz - b.cx() * so + b.cz() * co;
                double minDepth = Double.MAX_VALUE;
                double nx = 0;
                double nz = 0;
                boolean apart = false;
                for (double[] n : axes) {
                    double rA = a.hx() * Math.abs(n[0] * a1[0] + n[1] * a1[1])
                            + a.hz() * Math.abs(n[0] * a2[0] + n[1] * a2[1]);
                    double rB = b.hx() * Math.abs(n[0] * b1[0] + n[1] * b1[1])
                            + b.hz() * Math.abs(n[0] * b2[0] + n[1] * b2[1]);
                    double d = (ax - bx) * n[0] + (az - bz) * n[1];
                    double depth = rA + rB - Math.abs(d);
                    if (depth <= 1e-6) {
                        apart = true;
                        break;
                    }
                    if (depth < minDepth) {
                        minDepth = depth;
                        double sign = d >= 0 ? 1 : -1;
                        nx = n[0] * sign;
                        nz = n[1] * sign;
                    }
                }
                if (apart) continue;
                if (minDepth < bestDepth) {
                    bestDepth = minDepth;
                    best = new double[] {nx * minDepth, nz * minDepth, nx, nz};
                }
            }
        }
        return best;
    }

    // ---- vista di debug ----

    /** Gli spigoli di una hitbox, nello spazio del modello (blocchi): ogni segmento è {@code {x1,y1,z1,x2,y2,z2}}. */
    public record Outline(String name, boolean solid, List<double[]> segments) {}

    private static final int[][] BOX_EDGES = {{0, 1}, {1, 3}, {3, 2}, {2, 0}, {4, 5}, {5, 7}, {7, 6}, {6, 4}, {0, 4},
            {1, 5}, {2, 6}, {3, 7}};

    /**
     * Il contorno di tutte le hitbox, con le zone nella posa data (vuota = riposo): è quello che il puntamento vede.
     * L'ingombro solido è fisso; le zone seguono l'osso a cui sono legate, come nel raggio.
     */
    public List<Outline> outlines(Map<String, BoneTransform> pose) {
        List<Outline> out = new ArrayList<>();
        List<double[]> solidSegments = new ArrayList<>();
        for (Box b : solid) addBox(solidSegments, b, null);
        if (!solidSegments.isEmpty()) out.add(new Outline(SOLID_BONE, true, solidSegments));
        java.util.Set<String> names = new java.util.LinkedHashSet<>(zones.keySet());
        names.addAll(meshes.keySet());
        for (String zone : names) {
            double[][] mesh = meshes.get(zone);
            String poseBone = mesh != null ? boneOfZone(zone) : zoneParents.get(zone);
            Matrix4d m = null;
            if (poseBone != null && !pose.isEmpty()) {
                m = world(poseBone, pose, 0);
                if (Math.abs(m.determinant()) < 1e-9) m = null; // parte assente (chiave tolta): si mostra la sua sede
            }
            List<double[]> segments = new ArrayList<>();
            if (mesh != null) {
                for (double[] q : mesh) {
                    for (int i = 0; i < 4; i++) {
                        int j = (i + 1) % 4;
                        segments.add(segment(m, q[i * 3], q[i * 3 + 1], q[i * 3 + 2], q[j * 3], q[j * 3 + 1], q[j * 3 + 2]));
                    }
                }
            } else {
                for (Box b : zones.getOrDefault(zone, List.of())) addBox(segments, b, m);
            }
            if (!segments.isEmpty()) out.add(new Outline(zone, false, segments));
        }
        return out;
    }

    private static void addBox(List<double[]> into, Box b, Matrix4d m) {
        double[][] c = new double[8][];
        for (int i = 0; i < 8; i++) {
            c[i] = new double[] {b.cx() + ((i & 1) == 0 ? -b.hx() : b.hx()), b.cy() + ((i & 2) == 0 ? -b.hy() : b.hy()),
                    b.cz() + ((i & 4) == 0 ? -b.hz() : b.hz())};
        }
        for (int[] e : BOX_EDGES) {
            into.add(segment(m, c[e[0]][0], c[e[0]][1], c[e[0]][2], c[e[1]][0], c[e[1]][1], c[e[1]][2]));
        }
    }

    private static double[] segment(Matrix4d m, double x1, double y1, double z1, double x2, double y2, double z2) {
        if (m == null) return new double[] {x1, y1, z1, x2, y2, z2};
        Vector3d a = m.transformPosition(new Vector3d(x1, y1, z1));
        Vector3d b = m.transformPosition(new Vector3d(x2, y2, z2));
        return new double[] {a.x, a.y, a.z, b.x, b.y, b.z};
    }

    // ---- geometria nel mondo ----

    /** Angolo con cui il modello è ruotato nel mondo: 180° meno la direzione del muso (come nel disegno). */
    private static double theta(double headingDeg) {
        return Math.toRadians(180 - headingDeg);
    }

    /**
     * Il primo punto in cui il raggio colpisce una zona, entro {@code reach} blocchi. Il veicolo sta in
     * ({@code px, py, pz}) con il muso a {@code headingDeg}; il raggio parte da {@code (ox, oy, oz)} con direzione
     * {@code (dx, dy, dz)} (non serve che sia unitaria).
     */
    public Hit raycast(double ox, double oy, double oz, double dx, double dy, double dz, double reach, double px,
            double py, double pz, double headingDeg) {
        return raycast(ox, oy, oz, dx, dy, dz, reach, px, py, pz, headingDeg, Map.of());
    }

    /** Matrice dell'osso nello spazio del modello (in blocchi) per la posa data; le ossa fuori posa sono a riposo. */
    private Matrix4d world(String bone, Map<String, BoneTransform> pose, int depth) {
        Node n = nodes.get(bone);
        if (n == null || depth > 64) return new Matrix4d();
        Matrix4d m = n.parent() == null ? new Matrix4d() : world(n.parent(), pose, depth + 1);
        return m.mul(new Matrix4d(BonePose.local(n.pivot(), pose.getOrDefault(bone, BoneTransform.REST))));
    }

    /**
     * Come sopra, ma con le zone che seguono la posa delle ossa a cui sono appese: una zona dentro la cartella
     * {@code door_l} ruota con la portiera. {@code pose} dà le trasformazioni delle ossa mobili (vuota = riposo).
     */
    public Hit raycast(double ox, double oy, double oz, double dx, double dy, double dz, double reach, double px,
            double py, double pz, double headingDeg, Map<String, BoneTransform> pose) {
        return raycast(ox, oy, oz, dx, dy, dz, reach, px, py, pz, headingDeg, pose, 0);
    }

    /**
     * Come sopra, con un {@code margin}: il raggio diventa un fascio spesso (raggio {@code margin} blocchi), così le
     * parti piccole si prendono anche di striscio. Una parte aperta resta valida anche dove stava da chiusa: chi
     * vuole richiuderla la trova guardando lì o guardando la parte aperta.
     */
    public Hit raycast(double ox, double oy, double oz, double dx, double dy, double dz, double reach, double px,
            double py, double pz, double headingDeg, Map<String, BoneTransform> pose, double margin) {
        return raycast(ox, oy, oz, dx, dy, dz, reach, px, py, pz, headingDeg, pose, margin, null);
    }

    /** La zona {@code zone} è colpita dal raggio (con il margine dato)? Serve al server per verificare il client. */
    public boolean hitsZone(String zone, double ox, double oy, double oz, double dx, double dy, double dz,
            double reach, double px, double py, double pz, double headingDeg, Map<String, BoneTransform> pose,
            double margin) {
        return raycast(ox, oy, oz, dx, dy, dz, reach, px, py, pz, headingDeg, pose, margin, zone) != null;
    }

    private Hit raycast(double ox, double oy, double oz, double dx, double dy, double dz, double reach, double px,
            double py, double pz, double headingDeg, Map<String, BoneTransform> pose, double margin, String only) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!(len > 1e-9)) return null;
        dx /= len;
        dy /= len;
        dz /= len;
        double th = theta(headingDeg);
        double c = Math.cos(th);
        double s = Math.sin(th);
        // Dal mondo al modello: rotazione inversa attorno a Y.
        double[] lo = {(ox - px) * c - (oz - pz) * s, oy - py, (ox - px) * s + (oz - pz) * c};
        double[] ld = {dx * c - dz * s, dy, dx * s + dz * c};
        Hit best = null;
        double bestScore = Double.MAX_VALUE;
        java.util.Set<String> names = new java.util.LinkedHashSet<>(zones.keySet());
        names.addAll(meshes.keySet());
        for (String zone : names) {
            if (only != null && !only.equals(zone)) continue;
            double[][] mesh = meshes.get(zone);
            // Con la geometria vera la zona segue l'osso che le dà il nome; con i cubi, l'osso a cui è appesa.
            String poseBone = mesh != null ? boneOfZone(zone) : zoneParents.get(zone);
            boolean moved = poseBone != null && !pose.isEmpty();
            double t = -1;
            if (moved) {
                Matrix4d m = world(poseBone, pose, 0);
                if (Math.abs(m.determinant()) >= 1e-9) {
                    Matrix4d inv = m.invert();
                    Vector3d o = inv.transformPosition(new Vector3d(lo[0], lo[1], lo[2]));
                    Vector3d d = inv.transformDirection(new Vector3d(ld[0], ld[1], ld[2]));
                    t = hitZone(zone, mesh, new double[] {o.x, o.y, o.z}, new double[] {d.x, d.y, d.z}, margin);
                } else {
                    // Parte rimpicciolita a zero (chiave tolta): resta la sua sede, dove rimetterla.
                    t = hitZone(zone, mesh, lo, ld, margin);
                }
                // Solo dove la parte sta davvero: dove stava da chiusa ora c'è la carrozzeria (o un sedile).
            } else {
                t = hitZone(zone, mesh, lo, ld, margin);
            }
            if (t < 0) continue;
            double real = t >= FUZZY ? t - FUZZY : t;
            if (real > reach) continue;
            // Un colpo preciso batte sempre uno di sola tolleranza, qualunque sia la distanza.
            if (best == null || t < bestScore) {
                best = new Hit(zone, real);
                bestScore = t;
            }
        }
        return best;
    }

    /** Distanza aggiunta a un colpo "di tolleranza" perché perda sempre contro uno preciso. */
    private static final double FUZZY = 1000;

    /**
     * Punteggio dell'impatto con una zona: la distanza se il raggio colpisce la geometria vera; altrimenti, con un
     * {@code margin}, la distanza (più {@link #FUZZY}) se passa entro {@code margin} dalla scatola che la racchiude;
     * -1 se manca. Così le parti minuscole (una chiave di due centimetri) si prendono anche mirando vicino, ma una
     * zona precisa vince sempre su una solo vicina.
     */
    private double hitZone(String zone, double[][] mesh, double[] o, double[] d, double margin) {
        double t = -1;
        if (mesh != null) {
            for (double[] q : mesh) t = nearest(t, quadT(q, o, d));
        } else {
            for (Box b : zones.getOrDefault(zone, List.of())) t = nearest(t, slab(o[0], o[1], o[2], d[0], d[1], d[2], b, 0));
        }
        if (t >= 0 || margin <= 0) return t;
        double[] bb = bounds(zone);
        if (bb == null) return -1;
        Box around = new Box((bb[0] + bb[3]) / 2, (bb[1] + bb[4]) / 2, (bb[2] + bb[5]) / 2, (bb[3] - bb[0]) / 2,
                (bb[4] - bb[1]) / 2, (bb[5] - bb[2]) / 2);
        double f = slab(o[0], o[1], o[2], d[0], d[1], d[2], around, margin);
        return f >= 0 ? f + FUZZY : -1;
    }

    /** Impatto raggio-quad (due triangoli, a due facce): distanza o -1. */
    private static double quadT(double[] q, double[] o, double[] d) {
        double t1 = triT(q, 0, 1, 2, o, d);
        double t2 = triT(q, 0, 2, 3, o, d);
        return nearest(t1, t2);
    }

    /** Möller-Trumbore sui vertici i0,i1,i2 del quad. */
    private static double triT(double[] q, int i0, int i1, int i2, double[] o, double[] d) {
        double ax = q[i0 * 3];
        double ay = q[i0 * 3 + 1];
        double az = q[i0 * 3 + 2];
        double e1x = q[i1 * 3] - ax;
        double e1y = q[i1 * 3 + 1] - ay;
        double e1z = q[i1 * 3 + 2] - az;
        double e2x = q[i2 * 3] - ax;
        double e2y = q[i2 * 3 + 1] - ay;
        double e2z = q[i2 * 3 + 2] - az;
        double px = d[1] * e2z - d[2] * e2y;
        double py = d[2] * e2x - d[0] * e2z;
        double pz = d[0] * e2y - d[1] * e2x;
        double det = e1x * px + e1y * py + e1z * pz;
        if (Math.abs(det) < 1e-12) return -1;
        double inv = 1 / det;
        double tx = o[0] - ax;
        double ty = o[1] - ay;
        double tz = o[2] - az;
        double u = (tx * px + ty * py + tz * pz) * inv;
        if (u < 0 || u > 1) return -1;
        double qx = ty * e1z - tz * e1y;
        double qy = tz * e1x - tx * e1z;
        double qz = tx * e1y - ty * e1x;
        double v = (d[0] * qx + d[1] * qy + d[2] * qz) * inv;
        if (v < 0 || u + v > 1) return -1;
        double t = (e2x * qx + e2y * qy + e2z * qz) * inv;
        return t >= 0 ? t : -1;
    }

    private static double nearest(double a, double b) {
        if (a < 0) return b;
        if (b < 0) return a;
        return Math.min(a, b);
    }

    /** Distanza lungo il raggio dall'ingresso nella scatola (0 se l'origine è dentro), o -1 se non la colpisce. */
    private static double slab(double ox, double oy, double oz, double dx, double dy, double dz, Box b, double m) {
        double tMin = 0;
        double tMax = Double.MAX_VALUE;
        double[] o = {ox, oy, oz};
        double[] d = {dx, dy, dz};
        double[] lo = {b.cx() - b.hx() - m, b.cy() - b.hy() - m, b.cz() - b.hz() - m};
        double[] hi = {b.cx() + b.hx() + m, b.cy() + b.hy() + m, b.cz() + b.hz() + m};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-12) {
                if (o[i] < lo[i] || o[i] > hi[i]) return -1;
            } else {
                double t1 = (lo[i] - o[i]) / d[i];
                double t2 = (hi[i] - o[i]) / d[i];
                tMin = Math.max(tMin, Math.min(t1, t2));
                tMax = Math.min(tMax, Math.max(t1, t2));
                if (tMin > tMax) return -1;
            }
        }
        return tMin;
    }

    /** Quanto un giocatore può salire da solo sopra l'ingombro (gradino): oltre, l'ingombro è un muro. */
    public static final double STEP_UP = 0.6;
    /** Raggio del giocatore in orizzontale. */
    public static final double PLAYER_RADIUS = 0.3;
    /** Altezza del giocatore. */
    public static final double PLAYER_HEIGHT = 1.8;

    /**
     * Come spostare un giocatore (piedi in {@code x, y, z}) perché non entri nell'ingombro. Restituisce
     * {@code {dx, dy, dz}} da sommare alla posizione, o {@code null} se non c'è sovrapposizione. Un ostacolo basso
     * (fino a {@link #STEP_UP}) si sale, altrimenti si viene spinti fuori dal lato più vicino.
     */
    public double[] pushOut(double x, double y, double z, double px, double py, double pz, double headingDeg) {
        double th = theta(headingDeg);
        double c = Math.cos(th);
        double s = Math.sin(th);
        double lx = (x - px) * c - (z - pz) * s;
        double lz = (x - px) * s + (z - pz) * c;
        double ly = y - py;
        for (Box b : solid) {
            double top = b.cy() + b.hy();
            double bottom = b.cy() - b.hy();
            if (ly + PLAYER_HEIGHT <= bottom || ly >= top) continue;
            double penX = b.hx() + PLAYER_RADIUS - Math.abs(lx - b.cx());
            double penZ = b.hz() + PLAYER_RADIUS - Math.abs(lz - b.cz());
            if (penX <= 0 || penZ <= 0) continue;
            if (top - ly <= STEP_UP) return new double[] {0, top - ly, 0};
            double mx;
            double mz;
            if (penX < penZ) {
                mx = Math.signum(lx - b.cx() == 0 ? 1 : lx - b.cx()) * penX;
                mz = 0;
            } else {
                mx = 0;
                mz = Math.signum(lz - b.cz() == 0 ? 1 : lz - b.cz()) * penZ;
            }
            // Dal modello al mondo: rotazione diretta attorno a Y.
            return new double[] {mx * c + mz * s, 0, -mx * s + mz * c};
        }
        return null;
    }
}
