package dev.qrm.bank.net;

/** Azioni della schermata staff della banca. {@code REFRESH} chiede solo lo stato aggiornato. */
public enum StaffBankAction {
    REFRESH, FREEZE, UNFREEZE, GIVE_ATM, AUDIT,
    /** dà denaro al bersaglio: importo e valuta nel pacchetto */
    GRANT,
    /** toglie denaro al bersaglio: importo e valuta nel pacchetto */
    TAKE;

    public static StaffBankAction fromOrdinal(int ordinal) {
        StaffBankAction[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : REFRESH;
    }

    /** Azioni che agiscono su un giocatore e quindi richiedono un bersaglio. */
    public boolean needsTarget() {
        return this == FREEZE || this == UNFREEZE || this == GIVE_ATM || this == GRANT || this == TAKE;
    }
}
