package dev.qrm.vehicles.logic;

import dev.qrm.client.api.rig.BoneTransform;
import java.util.HashMap;
import java.util.Map;

/**
 * Le parti del veicolo che il giocatore può azionare: portiere, cofano, freno a mano, chiave, interruttore dei fari
 * e leva del cambio. Lo stato è una maschera di bit (sincronizzata come un intero) più i fari e la marcia, che hanno
 * il loro dato; l'animazione è procedurale, con gli angoli e le durate delle animazioni disegnate in Blockbench.
 */
public final class VehicleParts {
    private VehicleParts() {}

    public static final int DOOR_L = 1;
    public static final int DOOR_R = 2;
    public static final int HOOD = 4;
    public static final int PARKING_BRAKE = 8;
    public static final int KEY_INSERTED = 16;

    /** Canali animati. */
    public static final int CH_DOOR_L = 0;
    public static final int CH_DOOR_R = 1;
    public static final int CH_HOOD = 2;
    public static final int CH_PARKING_BRAKE = 3;
    public static final int CH_LIGHT_SWITCH = 4;
    /** 1 = chiave inserita, 0 = tolta (la chiave sparisce). */
    public static final int CH_KEY = 5;
    /** Posizione del blocchetto: 0 spento, 1 acceso, 1,75 avviamento. */
    public static final int CH_KEY_TURN = 6;
    /** Marcia selezionata (-1 R, 0 N, 1.. marce), che la leva raggiunge senza scatto. */
    public static final int CH_SHIFTER = 7;
    public static final int CHANNELS = 8;

    /** Quanto dura (tick, a 20 al secondo) un percorso di una unità di ciascun canale. */
    private static final float[] DURATION_TICKS = {16, 16, 24, 6, 3, 2, 4, 2};

    /** Il bit che una zona {@code hit_*} aziona come parte mobile, o 0 se la zona è altro. */
    public static int bitOfZone(String zone) {
        return switch (zone) {
            case "hit_door_handle_l", "hit_door_l" -> DOOR_L;
            case "hit_door_handle_r", "hit_door_r" -> DOOR_R;
            case "hit_hood" -> HOOD;
            case "hit_handbrake" -> PARKING_BRAKE;
            default -> 0;
        };
    }

    public static boolean has(int flags, int bit) {
        return (flags & bit) != 0;
    }

    /** Portiere e cofano si chiudono da sole sopra questa velocità (blocchi per tick) e non si aprono in marcia. */
    public static final double MAX_OPEN_SPEED = 0.1;

    /**
     * Valori che i canali inseguono.
     *
     * @param engine 0 spento, 1 acceso, 2 in avviamento
     * @param gear marcia selezionata
     */
    public static float[] targets(int flags, boolean lights, int engine, int gear) {
        float[] t = new float[CHANNELS];
        t[CH_DOOR_L] = has(flags, DOOR_L) ? 1 : 0;
        t[CH_DOOR_R] = has(flags, DOOR_R) ? 1 : 0;
        t[CH_HOOD] = has(flags, HOOD) ? 1 : 0;
        t[CH_PARKING_BRAKE] = has(flags, PARKING_BRAKE) ? 1 : 0;
        t[CH_LIGHT_SWITCH] = lights ? 1 : 0;
        t[CH_KEY] = has(flags, KEY_INSERTED) ? 1 : 0;
        t[CH_KEY_TURN] = engine == 2 ? 1.75f : engine == 1 ? 1 : 0;
        t[CH_SHIFTER] = gear;
        return t;
    }

    /** I canali, che inseguono i valori richiesti a velocità costante. */
    public static final class Animator {
        private final float[] value = new float[CHANNELS];
        private final float[] previous = new float[CHANNELS];

        /** Porta subito i canali ai valori (al primo fotogramma di un veicolo già in uso). */
        public void snap(float[] targets) {
            for (int i = 0; i < CHANNELS; i++) {
                value[i] = targets[i];
                previous[i] = value[i];
            }
        }

        /** Un tick di gioco. */
        public void tick(float[] targets) {
            for (int i = 0; i < CHANNELS; i++) {
                previous[i] = value[i];
                float step = 1f / DURATION_TICKS[i];
                value[i] = value[i] < targets[i] ? Math.min(targets[i], value[i] + step)
                        : Math.max(targets[i], value[i] - step);
            }
        }

        /** Valore di un canale, interpolato tra due tick. */
        public float value(int channel, float partialTick) {
            return previous[channel] + (value[channel] - previous[channel]) * partialTick;
        }

        /** I valori di tutti i canali nel fotogramma corrente. */
        public float[] values(float partialTick) {
            float[] out = new float[CHANNELS];
            for (int i = 0; i < CHANNELS; i++) out[i] = value(i, partialTick);
            return out;
        }
    }

    private static final float DOOR_MAX_DEG = 62;
    private static final float HOOD_MAX_DEG = 55;
    private static final float HANDBRAKE_MAX_DEG = 25;
    /** Quanto affonda l'interruttore dei fari, in unità del modello. */
    private static final float SWITCH_TRAVEL = 0.42f;
    private static final float KEY_TURN_DEG = 40;
    /** Inclinazione della leva in folle, e quanto cala per ogni marcia in più (gradi attorno a X). */
    private static final float SHIFTER_NEUTRAL_DEG = 15;
    private static final float SHIFTER_STEP_DEG = 10;

    private static float ease(float f) {
        return f * f * (3 - 2 * f);
    }

    /** Pose delle ossa mobili per i valori dei canali. Le ossa a riposo non compaiono. */
    public static Map<String, BoneTransform> pose(float[] v) {
        Map<String, BoneTransform> out = new HashMap<>();
        if (v[CH_DOOR_L] > 0) out.put("door_l", rot(0, -DOOR_MAX_DEG * ease(v[CH_DOOR_L]), 0));
        if (v[CH_DOOR_R] > 0) out.put("door_r", rot(0, DOOR_MAX_DEG * ease(v[CH_DOOR_R]), 0));
        if (v[CH_HOOD] > 0) out.put("hood", rot(HOOD_MAX_DEG * ease(v[CH_HOOD]), 0, 0));
        if (v[CH_PARKING_BRAKE] > 0) out.put("handbrake", rot(HANDBRAKE_MAX_DEG * ease(v[CH_PARKING_BRAKE]), 0, 0));
        if (v[CH_LIGHT_SWITCH] > 0) {
            out.put("light_switch", new BoneTransform(new float[] {0, 0, SWITCH_TRAVEL * ease(v[CH_LIGHT_SWITCH])},
                    new float[] {0, 0, 0}, new float[] {1, 1, 1}));
        }
        float key = Math.max(0, Math.min(1, v[CH_KEY]));
        if (key < 1 || v[CH_KEY_TURN] > 0) {
            out.put("ignition_key", new BoneTransform(new float[] {0, 0, 0}, new float[] {0, 0, -KEY_TURN_DEG * v[CH_KEY_TURN]},
                    new float[] {key, key, key}));
        }
        out.put("gear_shifter", rot(SHIFTER_NEUTRAL_DEG - SHIFTER_STEP_DEG * v[CH_SHIFTER], 0, 0));
        return out;
    }

    private static BoneTransform rot(float x, float y, float z) {
        return new BoneTransform(new float[] {0, 0, 0}, new float[] {x, y, z}, new float[] {1, 1, 1});
    }
}
