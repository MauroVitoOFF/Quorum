package dev.qrm.bank.net;

/** Esito di un'azione staff della banca: il server non invia mai testo, solo questo codice. */
public enum StaffBankResult {
    OK, NO_PERMISSION, RATE_LIMITED, NO_TARGET, NO_CHARACTER, ATM_GIVEN, AUDIT_CLEAN, AUDIT_ISSUES, CASH_EXCESS, INVALID,
    INSUFFICIENT_FUNDS, FROZEN, UNKNOWN_CURRENCY, ERROR;

    public static StaffBankResult fromOrdinal(int ordinal) {
        StaffBankResult[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : ERROR;
    }
}
