package dev.qrm.neoforge.net;

/** Esito di un'azione dei personaggi: il server non invia mai testo, solo questo codice. */
public enum ResultCode {
    OK, SLOTS_FULL, INVALID_NAME, NAME_TAKEN, INVALID_DATE, NOT_FOUND, NOT_OWNER, NOT_USABLE, RATE_LIMITED, ERROR;

    public static ResultCode fromOrdinal(int ordinal) {
        ResultCode[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : ERROR;
    }
}
