package dev.qrm.police.weapon.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qrm.police.weapon.logic.anim.AnimClip;
import dev.qrm.police.weapon.logic.anim.AnimEvent;
import dev.qrm.police.weapon.logic.anim.AnimFormat;
import dev.qrm.police.weapon.logic.mesh.ArmSolver;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Definizione di un'arma, letta da {@code weapons/<id>.json}: balistica, posa in prima persona, punti di
 * ancoraggio e tabella delle azioni. Logica pura, senza classi di Minecraft: la leggono sia il server sia il client
 * dal classpath (le risorse stanno nello stesso jar).
 *
 * <p>Durata e istanti di commit di un'azione con clip non si scrivono: si ricavano dalla clip stessa (lunghezza ed
 * eventi {@code commit}), così non c'è più un secondo posto da tenere allineato. Un'azione senza clip avrebbe durata e
 * commit nel file ({@code ticks}, {@code commits}).
 */
public final class WeaponDef {
    private static final String DIR = "/assets/qrm_police/weapons/";
    private static volatile Map<String, WeaponDef> byId;

    /** Balistica: danni, dispersione (gradi) e gittata (blocchi). */
    public record Ballistics(float bodyDamage, float headDamage, double hipSpread, double aimSpread,
            double crouchFactor, double range) {}

    /** Posa in prima persona: sedicesimi di blocco, gradi, scala. */
    public record Pose(float x, float y, float z, float rx, float ry, float rz, float scale) {}

    /** Calibrazione aggiunta al bone di ogni braccio: sedicesimi di blocco e gradi. */
    public record ArmCalibration(float x, float y, float z, float rx, float ry, float rz) {}

    /** Punto nello spazio del modello (unità Blockbench) legato a un bone. */
    public record Anchor(String bone, float[] point) {}

    /** Un'azione: clip (o {@code null}), se blocca lo sparo e, per le azioni senza clip, durata e commit propri. */
    public record Action(String clip, boolean blocksFire, int ticks, List<Commit> commits) {}

    /** Istante (tick) in cui lo stato di gioco cambia davvero. */
    public record Commit(int tick, CommitPoint point) {}

    private final String id;
    private final String item;
    private final String texture;
    private final String rig;
    private final String animations;
    private final Ballistics ballistics;
    private final Pose pose;
    private final ArmCalibration armCalibration;
    private final boolean crosshair;
    private final boolean renderOnlyInActions;
    private final String rightTarget;
    private final String leftDefault;
    private final Map<String, ArmSolver.Target> targets;
    private final Map<String, List<ArmSolver.Step>> leftSchedule;
    private final Map<String, List<ArmSolver.Step>> rightSchedule;
    private final Anchor muzzle;
    private final Anchor magazineTop;
    private final String hintBone;
    private final int magazineCapacity;
    private final String bulletItem;
    private final Map<String, String> clipAliases;
    private final Map<String, Action> actions;
    private final Map<String, AnimClip> clips;

    private WeaponDef(JsonObject o, Map<String, AnimClip> clips) {
        this.id = o.get("id").getAsString();
        this.item = o.get("item").getAsString();
        this.texture = o.get("texture").getAsString();
        this.rig = o.get("rig").getAsString();
        this.animations = o.get("animations").getAsString();
        JsonObject b = o.getAsJsonObject("ballistics");
        this.ballistics = new Ballistics(b.get("bodyDamage").getAsFloat(), b.get("headDamage").getAsFloat(),
                b.get("hipSpread").getAsDouble(), b.get("aimSpread").getAsDouble(),
                b.get("crouchFactor").getAsDouble(), b.get("range").getAsDouble());
        JsonObject fp = o.getAsJsonObject("firstPerson");
        JsonObject p = fp.getAsJsonObject("pose");
        this.pose = new Pose(p.get("x").getAsFloat(), p.get("y").getAsFloat(), p.get("z").getAsFloat(),
                p.get("rx").getAsFloat(), p.get("ry").getAsFloat(), p.get("rz").getAsFloat(),
                p.get("scale").getAsFloat());
        this.crosshair = !fp.has("crosshair") || fp.get("crosshair").getAsBoolean();
        this.renderOnlyInActions = fp.has("renderOnlyInActions") && fp.get("renderOnlyInActions").getAsBoolean();
        JsonObject c = fp.getAsJsonObject("armCalibration");
        this.armCalibration = new ArmCalibration(c.get("x").getAsFloat(), c.get("y").getAsFloat(),
                c.get("z").getAsFloat(), c.get("rx").getAsFloat(), c.get("ry").getAsFloat(),
                c.get("rz").getAsFloat());
        JsonObject hands = o.getAsJsonObject("hands");
        this.rightTarget = hands.get("right").getAsString();
        this.leftDefault = hands.get("leftDefault").getAsString();
        Map<String, ArmSolver.Target> tg = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : hands.getAsJsonObject("targets").entrySet()) {
            JsonObject t = e.getValue().getAsJsonObject();
            tg.put(e.getKey(), new ArmSolver.Target(t.get("bone").getAsString(), floats(t.getAsJsonArray("point")),
                    floats(t.getAsJsonArray("shoulder"))));
        }
        this.targets = Map.copyOf(tg);
        if (!targets.containsKey(rightTarget) || !targets.containsKey(leftDefault)) {
            throw new IllegalStateException("mano: punto di presa mancante");
        }
        this.leftSchedule = schedules(hands.getAsJsonObject("leftSchedule"), targets);
        this.rightSchedule = hands.has("rightSchedule")
                ? schedules(hands.getAsJsonObject("rightSchedule"), targets) : Map.of();
        JsonObject a = o.getAsJsonObject("anchors");
        this.muzzle = anchor(a.getAsJsonObject("muzzle"));
        this.magazineTop = anchor(a.getAsJsonObject("magazineTop"));
        this.hintBone = a.get("hintBone").getAsString();
        JsonObject mg = o.getAsJsonObject("magazine");
        this.magazineCapacity = mg.get("capacity").getAsInt();
        this.bulletItem = mg.get("bulletItem").getAsString();
        if (magazineCapacity <= 0) throw new IllegalStateException("capienza del caricatore non valida");
        Map<String, String> aliases = new LinkedHashMap<>();
        if (o.has("clipAliases")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("clipAliases").entrySet()) {
                aliases.put(e.getKey(), e.getValue().getAsString());
            }
        }
        this.clipAliases = Map.copyOf(aliases);
        this.clips = withAliases(clips, this.clipAliases);
        Map<String, Action> acts = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("actions").entrySet()) {
            acts.put(e.getKey(), action(e.getKey(), e.getValue().getAsJsonObject(), clips));
        }
        this.actions = Map.copyOf(acts);
    }

    /** Programmi delle mani per clip: [[inizio, fine, punto], ...]. */
    private static Map<String, List<ArmSolver.Step>> schedules(JsonObject o, Map<String, ArmSolver.Target> targets) {
        Map<String, List<ArmSolver.Step>> sched = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            List<ArmSolver.Step> steps = new ArrayList<>();
            for (JsonElement se : e.getValue().getAsJsonArray()) {
                JsonArray a3 = se.getAsJsonArray();
                String target = a3.get(2).getAsString();
                if (!ArmSolver.FREEZE.equals(target) && !targets.containsKey(target)) {
                    throw new IllegalStateException("mano: punto di presa sconosciuto " + target);
                }
                steps.add(new ArmSolver.Step(a3.get(0).getAsDouble(), a3.get(1).getAsDouble(), target));
            }
            sched.put(e.getKey(), List.copyOf(steps));
        }
        return Map.copyOf(sched);
    }

    private static float[] floats(JsonArray a) {
        return new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    private static Anchor anchor(JsonObject o) {
        JsonArray pt = o.getAsJsonArray("point");
        return new Anchor(o.get("bone").getAsString(),
                new float[] {pt.get(0).getAsFloat(), pt.get(1).getAsFloat(), pt.get(2).getAsFloat()});
    }

    private static Action action(String name, JsonObject o, Map<String, AnimClip> clips) {
        boolean blocks = o.get("blocksFire").getAsBoolean();
        if (!o.has("clip")) {
            List<Commit> commits = new ArrayList<>();
            if (o.has("commits")) {
                for (JsonElement e : o.getAsJsonArray("commits")) {
                    JsonObject c = e.getAsJsonObject();
                    commits.add(new Commit(c.get("tick").getAsInt(), point(c.get("point").getAsString())));
                }
            }
            return new Action(null, blocks, o.get("ticks").getAsInt(), List.copyOf(commits));
        }
        String clipName = o.get("clip").getAsString();
        AnimClip clip = clips.get(clipName);
        if (clip == null) {
            throw new IllegalStateException("azione " + name + ": clip mancante " + clipName);
        }
        // piccola tolleranza: 0,8 · 20 non deve diventare 17 per un errore di virgola mobile
        int ticks = (int) Math.ceil(clip.length() * 20 - 1e-6);
        List<Commit> commits = new ArrayList<>();
        for (AnimEvent ev : clip.events()) {
            if ("commit".equals(ev.type()) && !ev.args().isEmpty()) {
                commits.add(new Commit((int) Math.round(ev.t() * 20), point(ev.args().get(0))));
            }
        }
        commits.sort((x, y) -> Integer.compare(x.tick(), y.tick()));
        return new Action(clipName, blocks, ticks, List.copyOf(commits));
    }

    /** Le clip con i nomi alias aggiunti (o sostituiti): per esempio {@code idle} che rimanda a {@code mag_idle}. */
    public static <T> Map<String, T> withAliases(Map<String, T> clips, Map<String, String> aliases) {
        if (aliases.isEmpty()) return clips;
        Map<String, T> out = new LinkedHashMap<>(clips);
        for (Map.Entry<String, String> e : aliases.entrySet()) {
            T target = clips.get(e.getValue());
            if (target != null) out.put(e.getKey(), target);
        }
        return Map.copyOf(out);
    }

    private static CommitPoint point(String arg) {
        for (CommitPoint p : CommitPoint.values()) {
            if (p.arg().equals(arg)) return p;
        }
        throw new IllegalStateException("punto di commit sconosciuto: " + arg);
    }

    /** Tutte le armi, nell'ordine dell'indice {@code weapons/index.json}, caricate una volta sola dal classpath. */
    public static Map<String, WeaponDef> all() {
        Map<String, WeaponDef> m = byId;
        if (m == null) {
            synchronized (WeaponDef.class) {
                m = byId;
                if (m == null) {
                    Map<String, WeaponDef> loaded = new LinkedHashMap<>();
                    JsonObject index = JsonParser.parseReader(reader(DIR + "index.json")).getAsJsonObject();
                    for (JsonElement e : index.getAsJsonArray("weapons")) {
                        WeaponDef d = load(DIR + e.getAsString() + ".json");
                        loaded.put(d.id(), d);
                    }
                    m = Map.copyOf(loaded);
                    byId = m;
                }
            }
        }
        return m;
    }

    /** L'arma con questo id ({@code police_pistol}). */
    public static Optional<WeaponDef> byId(String id) {
        return Optional.ofNullable(all().get(id));
    }

    /** L'arma associata a un oggetto, per id di registro ({@code qrm_police:police_pistol}). */
    public static Optional<WeaponDef> forItem(String itemId) {
        for (WeaponDef d : all().values()) {
            if (d.item.equals(itemId)) return Optional.of(d);
        }
        return Optional.empty();
    }

    /** La pistola d'ordinanza: la logica del server (stati, azioni) è ancora quella di quest'arma. */
    public static WeaponDef pistol() {
        return byId("police_pistol").orElseThrow(() -> new IllegalStateException("manca police_pistol"));
    }

    /** Legge una definizione dal classpath, con le clip che essa indica. */
    public static WeaponDef load(String resource) {
        JsonObject o = JsonParser.parseReader(reader(resource)).getAsJsonObject();
        String animations = o.get("animations").getAsString();
        Map<String, AnimClip> clips = AnimFormat.parse(reader(classpathPath(animations)));
        return new WeaponDef(o, clips);
    }

    /** {@code qrm_police:animations/x.json} → {@code /assets/qrm_police/animations/x.json}. */
    private static String classpathPath(String id) {
        int colon = id.indexOf(':');
        return "/assets/" + id.substring(0, colon) + "/" + id.substring(colon + 1);
    }

    private static Reader reader(String resource) {
        InputStream in = WeaponDef.class.getResourceAsStream(resource);
        if (in == null) throw new IllegalStateException("risorsa mancante: " + resource);
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }

    public String id() {
        return id;
    }

    /** Vero se il mirino dell'arma sostituisce quello vanilla quando la si impugna. */
    public boolean showCrosshair() {
        return crosshair;
    }

    /** Vero se si disegna in prima persona solo durante un'azione (il caricatore): il resto del tempo resta il vanilla. */
    public boolean renderOnlyInActions() {
        return renderOnlyInActions;
    }

    /** Id di registro dell'oggetto dell'arma. */
    public String item() {
        return item;
    }

    /** Identificatore della texture, come {@code qrm_police:textures/item/police_pistol.png}. */
    public String texture() {
        return texture;
    }

    public String rig() {
        return rig;
    }

    public String animations() {
        return animations;
    }

    public Ballistics ballistics() {
        return ballistics;
    }

    public Pose pose() {
        return pose;
    }

    public ArmCalibration armCalibration() {
        return armCalibration;
    }

    /** Capienza del caricatore d'ordinanza: il massimo di colpi, anche per il riempimento a mano. */
    public int magazineCapacity() {
        return magazineCapacity;
    }

    /** Id di registro dell'oggetto proiettile che riempie il caricatore. */
    public String bulletItem() {
        return bulletItem;
    }

    /** Punti di presa delle mani, per nome. */
    public Map<String, ArmSolver.Target> handTargets() {
        return targets;
    }

    /** Nome del punto di presa di base della mano destra (l'impugnatura). */
    public String rightDefault() {
        return rightTarget;
    }

    /** Programma della mano destra per la clip; vuoto se resta sull'impugnatura. */
    public List<ArmSolver.Step> rightSchedule(String clip) {
        return rightSchedule.getOrDefault(clip, List.of());
    }

    /** Presa della mano destra. */
    public ArmSolver.Target rightHand() {
        return targets.get(rightTarget);
    }

    /** Punto da cui parte la mano sinistra (la presa a due mani). */
    public String leftDefault() {
        return leftDefault;
    }

    /** Programma della mano sinistra per la clip; vuoto se la mano sta sempre sulla presa di base. */
    public List<ArmSolver.Step> leftSchedule(String clip) {
        return leftSchedule.getOrDefault(clip, List.of());
    }

    public Anchor muzzle() {
        return muzzle;
    }

    public Anchor magazineTop() {
        return magazineTop;
    }

    public String hintBone() {
        return hintBone;
    }

    /** Azione con questo nome ({@code reload_empty}…), se definita. */
    public Optional<Action> action(String name) {
        return Optional.ofNullable(actions.get(name));
    }

    /** Nomi alias delle clip: la clip di questo nome è un'altra (per esempio {@code idle} → {@code mag_idle}). */
    public Map<String, String> clipAliases() {
        return clipAliases;
    }

    /** Clip lette dal file di animazione dell'arma. */
    public Map<String, AnimClip> clips() {
        return clips;
    }
}
