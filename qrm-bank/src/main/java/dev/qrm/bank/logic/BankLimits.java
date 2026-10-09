package dev.qrm.bank.logic;

/** Limiti di ritiro per valuta, in unità minime. */
public record BankLimits(long perOperation, long perDay) {
    /** Prudente con 2 decimali: 1000.00 per ritiro, 5000.00 al giorno. */
    public static final BankLimits DEFAULT = new BankLimits(100_000L, 500_000L);

    public BankLimits {
        if (perOperation <= 0 || perDay <= 0) throw new IllegalArgumentException("limits must be positive");
    }
}
