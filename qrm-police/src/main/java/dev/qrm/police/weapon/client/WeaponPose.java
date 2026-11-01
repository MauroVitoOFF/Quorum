package dev.qrm.police.weapon.client;

import dev.qrm.police.weapon.logic.WeaponDef;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Posa dell'arma in mano in prima persona, modificabile in gioco con {@code /pistolpose} per la messa a punto: posizione
 * e rotazione dell'arma (come il {@code display} del modello: sedicesimi di blocco e gradi) e calibrazione del
 * braccio sul bone. Lo spostamento di {@code root} in mira si ricalcola da qui, così la tacca resta al centro dello
 * schermo qualunque sia la posa.
 */
public final class WeaponPose {

    /** Nomi dei parametri, nell'ordine di {@link #VALUES}. */
    public static final List<String> NAMES = List.of(
            "x", "y", "z", "rx", "ry", "rz", "scale",
            "armx", "army", "armz", "armrx", "armry", "armrz");

    /** Valori correnti di ogni arma, per id; i valori di partenza sono quelli della definizione ({@code weapons/*.json}). */
    private static final Map<String, float[]> BY_WEAPON = new HashMap<>();
    private static WeaponDef active = WeaponDef.pistol();
    private static float[] VALUES = BY_WEAPON.computeIfAbsent(active.id(), k -> defaults(active));

    /** Tacca di mira nel modello (0..1) e profondità a cui deve stare in mira (blocchi, davanti alla camera). */
    private static final float NOTCH_Y = 11.5f / 16f;
    private static final float AIM_DEPTH = -0.776f;

    private WeaponPose() {}

    /** Sceglie l'arma di cui si leggono e si regolano i valori (quella in mano). */
    public static void select(WeaponDef weapon) {
        if (weapon == active) return;
        active = weapon;
        VALUES = BY_WEAPON.computeIfAbsent(weapon.id(), k -> defaults(weapon));
    }

    /** Sceglie l'arma che si ha in mano; falso se non se ne impugna una. */
    public static boolean selectHeld() {
        net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return false;
        java.util.Optional<WeaponDef> held = HeldWeapon.of(player.getMainHandItem());
        held.ifPresent(WeaponPose::select);
        return held.isPresent();
    }

    private static float[] defaults(WeaponDef d) {
        WeaponDef.Pose p = d.pose();
        WeaponDef.ArmCalibration c = d.armCalibration();
        return new float[] {p.x(), p.y(), p.z(), p.rx(), p.ry(), p.rz(), p.scale(), c.x(), c.y(), c.z(), c.rx(), c.ry(),
            c.rz()};
    }

    public static float get(String name) {
        return VALUES[NAMES.indexOf(name)];
    }

    /** @return falso se il nome non esiste */
    public static boolean set(String name, float value) {
        int i = NAMES.indexOf(name);
        if (i < 0) return false;
        VALUES[i] = value;
        return true;
    }

    public static void reset() {
        System.arraycopy(defaults(active), 0, VALUES, 0, VALUES.length);
    }

    public static float x() { return VALUES[0] / 16f; }
    public static float y() { return VALUES[1] / 16f; }
    public static float z() { return VALUES[2] / 16f; }
    public static float rotX() { return VALUES[3]; }
    public static float rotY() { return VALUES[4]; }
    public static float rotZ() { return VALUES[5]; }
    public static float scale() { return VALUES[6]; }
    public static float armX() { return VALUES[7]; }
    public static float armY() { return VALUES[8]; }
    public static float armZ() { return VALUES[9]; }
    public static float armRotX() { return VALUES[10]; }
    public static float armRotY() { return VALUES[11]; }
    public static float armRotZ() { return VALUES[12]; }

    /**
     * Spostamento di {@code root} in mira (unità Blockbench) che porta la tacca al centro, alla profondità
     * {@link #AIM_DEPTH}: punto della tacca nella camera = T + R·s·(n + o/16 - 0,5), con T l'abbassamento vanilla
     * più il display, R la rotazione del display, n la tacca e o lo spostamento cercato.
     */
    static float[] aimOffset() {
        Vector3f t = new Vector3f(0.56f + x(), -0.52f + y(), -0.72f + z());
        Vector3f d = new Vector3f(-t.x, -t.y, AIM_DEPTH - t.z);
        new Quaternionf().rotationXYZ((float) Math.toRadians(rotX()), (float) Math.toRadians(rotY()),
                (float) Math.toRadians(rotZ())).conjugate().transform(d);
        float s = scale();
        return new float[] {
            (d.x / s) * 16f,
            (d.y / s - (NOTCH_Y - 0.5f)) * 16f,
            (d.z / s) * 16f
        };
    }

    /** Riga con tutti i valori, da incollare a Claude. */
    public static String summary() {
        StringBuilder sb = new StringBuilder("pistolpose");
        for (int i = 0; i < NAMES.size(); i++) {
            sb.append(' ').append(NAMES.get(i)).append('=').append(String.format(Locale.ROOT, "%.2f", VALUES[i]));
        }
        return sb.toString();
    }
}
