package dev.qrm.medical.logic;

import java.util.Locale;

/** Categoria di una ferita, per la diagnosi dei medici e della scientifica. */
public enum InjuryType {
    FALL, BLUNT, BLADE, BULLET, FIRE, EXPLOSION, STARVATION, DROWNING, OTHER;

    public String langKey() {
        return "qrm_medical.injury." + name().toLowerCase(Locale.ROOT);
    }

    public static InjuryType parse(String s) {
        try {
            return valueOf(s);
        } catch (IllegalArgumentException | NullPointerException e) {
            return OTHER;
        }
    }
}
