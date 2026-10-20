package dev.qrm.immersion.logic;

/** La posa delle braccia di chi sta comunicando col corpo. Sincronizzata dal server a chi vede. */
public enum BodyPose {
    NONE,
    /** Resa: braccia sopra le spalle. Blocca ogni azione di gioco. */
    HANDS_UP,
    /** Il braccio destro indica ciò che si guarda. */
    POINTING;

    /** Vero se chi è in questa posa non può attaccare, usare oggetti né costruire. */
    public boolean blocksActions() {
        return this == HANDS_UP;
    }

    public static BodyPose byOrdinal(int ordinal) {
        BodyPose[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
    }
}
