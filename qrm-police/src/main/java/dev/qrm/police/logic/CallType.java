package dev.qrm.police.logic;

/** Tipo di chiamata d'emergenza. */
public enum CallType {
    MEDICAL, POLICE, SHOOTING;

    public static CallType parse(String s) {
        try {
            return valueOf(s.toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException e) {
            return POLICE;
        }
    }
}
