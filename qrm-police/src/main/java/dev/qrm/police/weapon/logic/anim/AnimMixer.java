package dev.qrm.police.weapon.logic.anim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Miscelatore puro degli strati di animazione della pistola, avanzato a tick (20 al secondo) e campionato con il
 * tick parziale. Le clip arrivano a ogni chiamata, così una ricarica delle risorse (F3+T) vale subito.
 *
 * <p>Ordine degli strati in {@link #pose}, dal basso verso l'alto:
 * <ol>
 *   <li><b>Base</b>: {@code idle} (ciclo) oppure {@code sprint_in} → {@code sprint} (ciclo) → {@code sprint_out};
 *       ogni cambio si dissolve in {@link #BASE_FADE_TICKS}. Se esiste una clip {@code aim} viene fusa sopra con il
 *       peso di mira (che sale e scende in {@code aimTicks}). Una transizione di corsa mancante viene saltata.</li>
 *   <li><b>Azione</b> (sparo, ricarica, estrazione...): entra in {@link #ACTION_FADE_TICKS} e, finita (o fermata),
 *       esce nello stesso tempo. Sovrascrive solo gli ossi che anima: gli altri restano quelli della base. Una nuova
 *       azione si fonde da quella precedente. Una clip {@code hold_last} finita tiene l'ultima posa finché non
 *       arriva un'altra azione, {@link #stop()} o {@link #reset()}.</li>
 *   <li><b>Stato</b> dell'arma (regola R27): carrello bloccato → {@code slide} (e {@code port_cover}, che ne è figlio
 *       e nella clip nasconde il tappo) alla posa finale di {@code fire_last}; nessun caricatore → {@code magazine}
 *       a scala 0. Precedenza, per gruppo di ossi: un'azione <i>in corso</i> vince sullo stato solo sugli ossi che
 *       anima (lì lo stato pesa {@code 1 - ingresso} dell'azione); sugli altri lo stato pesa sempre 1; appena la
 *       clip è finita, anche se {@code hold_last} la tiene ancora, lo stato pesa 1 e prevale sugli ossi che tocca.
 *       Eccezione (R34): "nessun caricatore" prevale su qualsiasi clip tranne {@code reload_tactical} e
 *       {@code reload_empty}, che devono mostrare i caricatori che escono ed entrano. Lo stato non tocca niente
 *       quando non c'è nulla da imporre.</li>
 *   <li><b>Mira statica</b>: senza clip {@code aim}, uno spostamento costante di {@code root} (unità Blockbench)
 *       pesato dalla mira e sommato in cima, così resta anche durante sparo e ricarica.</li>
 * </ol>
 *
 * <p>Gli eventi vengono raccolti solo dalla clip d'azione corrente (non fermata), ciascuno una volta sola, quando
 * il suo istante cade nel tick appena trascorso; quelli a {@code t = 0} subito, in {@link #play}.
 */
public final class AnimMixer {

    public static final int ACTION_FADE_TICKS = 2;
    public static final int BASE_FADE_TICKS = 2;
    public static final double TICKS_PER_SECOND = 20.0;

    public static final String IDLE = "idle";
    public static final String SPRINT_IN = "sprint_in";
    public static final String SPRINT = "sprint";
    public static final String SPRINT_OUT = "sprint_out";
    public static final String AIM = "aim";
    public static final String FIRE_LAST = "fire_last";

    private static final Set<String> SLIDE_BONES = Set.of("slide", "port_cover");
    /** Le sole clip che mostrano il caricatore anche quando lo stato dice che non c'è (escono ed entrano). */
    private static final Set<String> RELOAD_CLIPS = Set.of("reload_tactical", "reload_empty");
    /** Estrazione: il carrello bloccato indietro dallo stato non si anima, altrimenti rientrando "ritorna indietro". */
    private static final String DRAW_CLIP = "draw";
    private static final String MAGAZINE = "magazine";
    private static final String ROOT = "root";
    private static final double EPS = 1e-6;

    /** Stato dell'arma letto dall'oggetto in mano: carrello bloccato indietro, caricatore inserito. */
    public record State(boolean slideLocked, boolean hasMagazine) {
        public static final State NONE = new State(false, true);
    }

    private final int aimTicks;
    private float[] staticAimOffset;

    private long now;

    private String baseClip = IDLE;
    private long baseStart;
    private String basePrevClip;
    private long basePrevStart;
    private long baseSwitch;
    private boolean sprinting;

    private boolean aiming;
    private float aimLevel;

    private String actionClip;
    private long actionStart;
    private long actionStop = -1;
    private double lastEventT;

    private String prevClip;
    private long prevStart;
    private float prevWeight;

    private final List<AnimEvent> pending = new ArrayList<>();

    /**
     * @param aimTicks durata della transizione di mira in tick
     * @param staticAimOffset spostamento di {@code root} in mira piena (unità Blockbench), usato se manca la clip
     *     {@code aim}
     */
    /** Cambia lo spostamento di mira statico (messa a punto della posa in gioco). */
    public void setStaticAimOffset(float[] offset) {
        this.staticAimOffset = offset.clone();
    }

    public AnimMixer(int aimTicks, float[] staticAimOffset) {
        this.aimTicks = Math.max(1, aimTicks);
        this.staticAimOffset = staticAimOffset.clone();
    }

    // ------------------------------------------------------------------ comandi

    /** Avvia la clip d'azione; una clip che non esiste viene ignorata. Gli eventi a {@code t = 0} partono subito. */
    public void play(String clip, Map<String, AnimClip> clips) {
        AnimClip c = clips.get(clip);
        if (c == null) return;
        if (actionClip != null) {
            prevClip = actionClip;
            prevStart = actionStart;
            prevWeight = actionWeight(clips, 0f);
        }
        actionClip = clip;
        actionStart = now;
        actionStop = -1;
        lastEventT = Double.NEGATIVE_INFINITY;
        collectEvents(c, 0.0);
    }

    /** Ferma l'azione corrente: esce in {@link #ACTION_FADE_TICKS} senza emettere altri eventi. */
    public void stop() {
        if (actionClip != null && actionStop < 0) actionStop = now;
    }

    public void setAiming(boolean aiming) {
        this.aiming = aiming;
    }

    public void setSprinting(boolean sprinting) {
        if (this.sprinting == sprinting) return;
        this.sprinting = sprinting;
        switchBase(sprinting ? SPRINT_IN : SPRINT_OUT);
    }

    /** Torna a riposo: nessuna azione, niente corsa né mira, eventi in attesa scartati. */
    public void reset() {
        actionClip = null;
        actionStop = -1;
        prevClip = null;
        sprinting = false;
        aiming = false;
        aimLevel = 0f;
        baseClip = IDLE;
        baseStart = now;
        basePrevClip = null;
        pending.clear();
    }

    /** Avanza di un tick: mira, fasi della base, eventi, fine delle azioni. */
    public void tick(Map<String, AnimClip> clips) {
        now++;
        aimLevel = clamp01(aimLevel + (aiming ? 1f : -1f) / aimTicks);
        advanceBase(clips);

        if (actionClip != null) {
            AnimClip c = clips.get(actionClip);
            if (c == null) {
                actionClip = null;
            } else {
                if (actionStop < 0) collectEvents(c, Math.min((now - actionStart) / TICKS_PER_SECOND, c.length()));
                if (actionWeight(clips, 0f) <= 0f && (actionStop >= 0 || finished(c, now - actionStart))) {
                    actionClip = null;
                }
            }
        }
        if (actionClip == null || ramp(now - actionStart, ACTION_FADE_TICKS) >= 1f) prevClip = null;
    }

    /** Eventi raccolti dall'ultima chiamata (sound, commit, muzzle_flash...), in ordine. */
    public List<AnimEvent> drainEvents() {
        if (pending.isEmpty()) return List.of();
        List<AnimEvent> out = List.copyOf(pending);
        pending.clear();
        return out;
    }

    // ------------------------------------------------------------------ lettura

    public String actionClip() {
        return actionClip;
    }

    /** Secondi trascorsi dall'inizio dell'azione corrente (0 se non ce n'è una): serve al programma delle mani. */
    public double actionSeconds(float partialTick) {
        return actionClip == null ? 0.0 : Math.max(0.0, (now - actionStart + partialTick) / TICKS_PER_SECOND);
    }

    public String baseClip() {
        return baseClip;
    }

    /** Peso di mira all'istante {@code tick corrente + partialTick}. */
    public float aimWeight(float partialTick) {
        return clamp01(aimLevel + (aiming ? 1f : -1f) * partialTick / aimTicks);
    }

    /** Peso dell'azione corrente (0 se non ce n'è una). */
    public float actionWeight(Map<String, AnimClip> clips, float partialTick) {
        if (actionClip == null) return 0f;
        AnimClip c = clips.get(actionClip);
        if (c == null) return 0f;
        double e = now - actionStart + partialTick;
        float w = ramp(e, ACTION_FADE_TICKS);
        if (actionStop >= 0) {
            w = Math.min(w, 1f - ramp(now - actionStop + partialTick, ACTION_FADE_TICKS));
        } else if (!c.holdLast()) {
            double len = c.length() * TICKS_PER_SECOND;
            if (e > len) w = Math.min(w, 1f - ramp(e - len, ACTION_FADE_TICKS));
        }
        return w;
    }

    /**
     * Peso della sovrapposizione dello stato sugli ossi animati dall'azione: {@code 1 - ingresso} durante un'azione
     * in corso, altrimenti 1. Sugli ossi che l'azione non anima lo stato pesa sempre 1.
     */
    public float stateWeight(Map<String, AnimClip> clips, float partialTick) {
        if (actionClip == null || actionStop >= 0) return 1f;
        AnimClip c = clips.get(actionClip);
        if (c == null) return 1f;
        double e = now - actionStart + partialTick;
        if (finished(c, e)) return 1f;
        return 1f - ramp(e, ACTION_FADE_TICKS);
    }

    /** Posa completa, ossi animati soltanto (gli assenti valgono riposo). */
    public Map<String, BoneTransform> pose(Map<String, AnimClip> clips, float partialTick, State state) {
        // 1. Base
        Map<String, BoneTransform> pose = sampleBase(clips, baseClip, baseStart, partialTick);
        if (basePrevClip != null) {
            float f = ramp(now - baseSwitch + partialTick, BASE_FADE_TICKS);
            if (f < 1f) {
                pose = AnimEvaluator.blend(sampleBase(clips, basePrevClip, basePrevStart, partialTick), pose, f);
            }
        }
        float aimW = aimWeight(partialTick);
        AnimClip aimClip = clips.get(AIM);
        if (aimClip != null && aimW > 0f) {
            pose = AnimEvaluator.blend(pose, AnimEvaluator.sample(aimClip, clipTime(aimClip, seconds(now, partialTick))),
                    aimW);
        }

        // 2. Azione (con la precedente che si sta dissolvendo)
        if (actionClip != null && clips.containsKey(actionClip)) {
            Map<String, BoneTransform> cur = sampleAction(clips.get(actionClip), actionStart, partialTick);
            if (prevClip != null && clips.containsKey(prevClip)) {
                float in = ramp(now - actionStart + partialTick, ACTION_FADE_TICKS);
                Map<String, BoneTransform> prev = sampleAction(clips.get(prevClip), prevStart, partialTick);
                Map<String, BoneTransform> out = new HashMap<>(pose);
                for (Map.Entry<String, BoneTransform> e : prev.entrySet()) {
                    float w = cur.containsKey(e.getKey()) ? prevWeight : prevWeight * (1f - in);
                    out.put(e.getKey(), mix(pose.getOrDefault(e.getKey(), BoneTransform.REST), e.getValue(), w));
                }
                pose = out;
            }
            pose = overlay(pose, cur, actionWeight(clips, partialTick));
        }

        // 3. Stato dell'arma
        if (state != null) pose = applyState(pose, clips, state, stateWeight(clips, partialTick), actionBones(clips));

        // 4. Mira statica, additiva su root
        if (aimClip == null && aimW > 0f) {
            BoneTransform r = pose.getOrDefault(ROOT, BoneTransform.REST);
            float[] p = r.position();
            for (int i = 0; i < 3; i++) p[i] += staticAimOffset[i] * aimW;
            Map<String, BoneTransform> out = new HashMap<>(pose);
            out.put(ROOT, new BoneTransform(p, r.rotationDeg(), r.scale()));
            pose = out;
        }
        return pose;
    }

    // ------------------------------------------------------------------ funzioni pure

    /** Tempo della clip: in ciclo riavvolto su {@code length}, altrimenti limitato a {@code [0, length]}. */
    public static double clipTime(AnimClip clip, double seconds) {
        double len = clip.length();
        if (seconds <= 0) return 0;
        if (clip.loop()) return len <= 0 ? 0 : seconds % len;
        return Math.min(seconds, Math.max(0, len));
    }

    /** Rampa lineare da 0 a 1 in {@code fadeTicks}; con durata nulla vale subito 1. */
    public static float ramp(double ticks, int fadeTicks) {
        if (fadeTicks <= 0) return 1f;
        return clamp01((float) (ticks / fadeTicks));
    }

    /** Sovrascrive con peso {@code w} solo gli ossi presenti in {@code top}. */
    public static Map<String, BoneTransform> overlay(Map<String, BoneTransform> base, Map<String, BoneTransform> top,
            float w) {
        if (top.isEmpty() || w <= 0f) return base;
        Map<String, BoneTransform> out = new HashMap<>(base);
        for (Map.Entry<String, BoneTransform> e : top.entrySet()) {
            out.put(e.getKey(), mix(base.getOrDefault(e.getKey(), BoneTransform.REST), e.getValue(), w));
        }
        return out;
    }

    // ------------------------------------------------------------------ interni

    private void switchBase(String clip) {
        basePrevClip = baseClip;
        basePrevStart = baseStart;
        baseSwitch = now;
        baseClip = clip;
        baseStart = now;
    }

    private void advanceBase(Map<String, AnimClip> clips) {
        // Al massimo due passi per tick (transizione mancante → ciclo).
        for (int i = 0; i < 2; i++) {
            String next = SPRINT_IN.equals(baseClip) ? SPRINT : SPRINT_OUT.equals(baseClip) ? IDLE : null;
            if (next == null) return;
            AnimClip c = clips.get(baseClip);
            if (c == null) {
                // Transizione assente: la si salta, la dissolvenza riparte da qui verso la clip successiva.
                baseClip = next;
                baseStart = now;
                baseSwitch = now;
            } else if (finished(c, now - baseStart)) {
                switchBase(next);
            } else {
                return;
            }
        }
    }

    private static boolean finished(AnimClip c, double elapsedTicks) {
        return !c.loop() && elapsedTicks + EPS >= c.length() * TICKS_PER_SECOND;
    }

    private Map<String, BoneTransform> sampleBase(Map<String, AnimClip> clips, String clip, long start, float partial) {
        AnimClip c = clips.get(clip);
        if (c == null) return Map.of();
        return AnimEvaluator.sample(c, clipTime(c, seconds(now - start, partial)));
    }

    private Map<String, BoneTransform> sampleAction(AnimClip c, long start, float partial) {
        return AnimEvaluator.sample(c, clipTime(c, seconds(now - start, partial)));
    }

    private static double seconds(long ticks, float partial) {
        return (ticks + partial) / TICKS_PER_SECOND;
    }

    private void collectEvents(AnimClip c, double upTo) {
        for (AnimEvent e : c.events()) {
            if (e.t() > lastEventT && e.t() <= upTo + EPS) pending.add(e);
        }
        lastEventT = Math.max(lastEventT, upTo + EPS);
    }

    /**
     * Ossi della clip d'azione corrente e di quella che si sta dissolvendo su cui l'azione può prevalere sullo stato.
     * Il caricatore conta solo per le ricariche ({@link #RELOAD_CLIPS}): le altre clip (per esempio
     * {@code ammo_check}, che il server consente anche senza caricatore) non lo fanno mai ricomparire (regola R34). Lo stesso vale per carrello e coperchio nell'estrazione ({@code draw}):
     * a pistola scarica il carrello resta indietro anche mentre la si estrae.
     */
    private Set<String> actionBones(Map<String, AnimClip> clips) {
        Set<String> out = new HashSet<>();
        addActionBones(out, actionClip, clips);
        addActionBones(out, prevClip, clips);
        return out;
    }

    private static void addActionBones(Set<String> out, String name, Map<String, AnimClip> clips) {
        AnimClip clip = name == null ? null : clips.get(name);
        if (clip == null) return;
        for (String bone : clip.bones().keySet()) {
            if (MAGAZINE.equals(bone) && !RELOAD_CLIPS.contains(name)) continue;
            if (SLIDE_BONES.contains(bone) && DRAW_CLIP.equals(name)) continue;
            out.add(bone);
        }
    }

    /**
     * Sovrappone lo stato. Il peso {@code actionW} vale solo per i gruppi di ossi che l'azione anima (carrello e
     * tappo insieme, caricatore da solo); gli altri ricevono lo stato a peso pieno, così un'azione che non tocca il
     * caricatore non lo fa ricomparire e una che non tocca il carrello non lo fa scattare avanti.
     */
    private static Map<String, BoneTransform> applyState(Map<String, BoneTransform> pose, Map<String, AnimClip> clips,
            State state, float actionW, Set<String> animated) {
        Map<String, BoneTransform> out = pose;
        if (state.slideLocked()) {
            AnimClip last = clips.get(FIRE_LAST);
            boolean slideAnimated = SLIDE_BONES.stream().anyMatch(animated::contains);
            float w = slideAnimated ? actionW : 1f;
            if (last != null && w > 0f) {
                Map<String, BoneTransform> end = AnimEvaluator.sample(last, last.length());
                Map<String, BoneTransform> top = new HashMap<>();
                for (String bone : SLIDE_BONES) {
                    BoneTransform t = end.get(bone);
                    if (t != null) top.put(bone, t);
                }
                out = overlay(out, top, w);
            }
        }
        if (!state.hasMagazine()) {
            float w = animated.contains(MAGAZINE) ? actionW : 1f;
            BoneTransform m = out.getOrDefault(MAGAZINE, BoneTransform.REST);
            out = overlay(out, Map.of(MAGAZINE, new BoneTransform(m.position(), m.rotationDeg(), new float[] {0, 0, 0})),
                    w);
        }
        return out;
    }

    private static BoneTransform mix(BoneTransform x, BoneTransform y, float w) {
        if (w >= 1f) return y;
        if (w <= 0f) return x;
        return new BoneTransform(lerp(x.position(), y.position(), w), lerp(x.rotationDeg(), y.rotationDeg(), w),
                lerp(x.scale(), y.scale(), w));
    }

    private static float[] lerp(float[] a, float[] b, float w) {
        float[] o = new float[3];
        for (int i = 0; i < 3; i++) o[i] = a[i] + (b[i] - a[i]) * w;
        return o;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(1f, v);
    }
}
