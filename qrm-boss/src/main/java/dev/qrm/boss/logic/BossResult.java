package dev.qrm.boss.logic;

import java.util.Locale;

/** Esito di una richiesta del menu. Il testo è la chiave {@code qrm_boss.result.<nome>}. */
public enum BossResult {
    OK,
    NO_CHARACTER,
    STALE_CHARACTER,
    NO_PERMISSION,
    NO_ORG,
    RATE_LIMITED,
    READ_ONLY,
    NO_TARGET,
    SELF,
    LEVEL_TOO_HIGH,
    ALREADY_MEMBER,
    NOT_MEMBER,
    UNKNOWN_RANK,
    NO_CURRENCY,
    NO_ACCOUNT,
    INVALID_AMOUNT,
    INSUFFICIENT_FUNDS,
    FROZEN,
    ERROR;

    public String translationKey() {
        return "qrm_boss.result." + name().toLowerCase(Locale.ROOT);
    }

    /** Senza uno snapshot, questi esiti chiudono il menu (il Character o l'azienda non sono più validi). */
    public boolean closesScreen() {
        return this == NO_CHARACTER || this == STALE_CHARACTER || this == NO_PERMISSION || this == NO_ORG;
    }

    public static BossResult fromOrdinal(int ordinal) {
        BossResult[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : ERROR;
    }
}
