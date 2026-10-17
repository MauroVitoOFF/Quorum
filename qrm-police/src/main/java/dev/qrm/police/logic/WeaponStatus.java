package dev.qrm.police.logic;

/** Stato di un'arma nel registro. */
public enum WeaponStatus {
    REGULAR, STOLEN, SEIZED, POLICE_ISSUE;

    public static WeaponStatus parse(String s) {
        try {
            return valueOf(s);
        } catch (RuntimeException e) {
            return REGULAR;
        }
    }
}
