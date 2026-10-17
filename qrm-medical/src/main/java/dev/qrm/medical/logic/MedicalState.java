package dev.qrm.medical.logic;

/** Gli stadi di un giocatore ferito. L'ordine è la gravità crescente. */
public enum MedicalState {
    HEALTHY,
    DOWNED,
    UNCONSCIOUS,
    CLINICALLY_DEAD;

    /** Vero se il giocatore non può agire (a terra, incosciente). */
    public boolean incapacitated() {
        return this == DOWNED || this == UNCONSCIOUS;
    }

    public static MedicalState byOrdinal(int ordinal) {
        MedicalState[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : HEALTHY;
    }
}
