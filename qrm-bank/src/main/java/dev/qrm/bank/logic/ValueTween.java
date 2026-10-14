package dev.qrm.bank.logic;

/**
 * Valore che scorre dal numero mostrato al nuovo in {@link #DURATION_MS} (rallenta verso la fine). Serve alla
 * schermata per far "contare" saldo e contanti; il tempo lo passa il chiamante, così si può provare senza orologio.
 */
public final class ValueTween {
    public static final long DURATION_MS = 450;

    private long from;
    private long to;
    private long start;

    public ValueTween(long value) {
        this.from = value;
        this.to = value;
    }

    /** Il valore da mostrare all'istante {@code now}. */
    public long value(long now) {
        long elapsed = now - start;
        if (from == to || elapsed >= DURATION_MS) return to;
        if (elapsed <= 0) return from;
        double t = (double) elapsed / DURATION_MS;
        double eased = 1 - Math.pow(1 - t, 3);
        return from + Math.round((to - from) * eased);
    }

    /** Nuovo traguardo: si riparte dal valore che si vedeva in quel momento. */
    public void retarget(long target, long now) {
        if (target == to) return;
        this.from = value(now);
        this.to = target;
        this.start = now;
    }

    public boolean running(long now) {
        return from != to && now - start < DURATION_MS;
    }
}
