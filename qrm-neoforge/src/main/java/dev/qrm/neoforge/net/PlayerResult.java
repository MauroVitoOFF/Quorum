package dev.qrm.neoforge.net;

/** Esito di un'azione sul giocatore: solo codici, i testi stanno nei file lingua del client. */
public enum PlayerResult {
    OK,
    NO_PERMISSION,
    NO_TARGET,
    RATE_LIMITED,
    INVALID,
    NO_CHARACTER,
    UNKNOWN_ITEM,
    FROZEN,
    UNFROZEN,
    NOT_MUTED,
    ERROR;

    public static PlayerResult fromOrdinal(int ordinal) {
        PlayerResult[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : ERROR;
    }
}
