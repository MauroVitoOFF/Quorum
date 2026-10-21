package dev.qrm.police.weapon.logic.input;

/**
 * Limite di frequenza per giocatore delle intenzioni diverse dallo sparo (lo sparo ha già il suo intervallo): al
 * massimo {@code max} intenzioni in una finestra scorrevole di {@code windowTicks} tick; le eccedenti si ignorano in
 * silenzio. Serve contro un client modificato che, ripetendo interazioni, farebbe riavviare di continuo le sessioni
 * (e i loro suoni) per chi gli sta intorno.
 *
 * <p>Il tempo è un contatore di tick qualsiasi, purché crescente.
 */
public final class InputRateLimit {
    /** Intenzioni ammesse per finestra: 10 al secondo. */
    public static final int DEFAULT_MAX = 10;
    public static final int DEFAULT_WINDOW_TICKS = 20;

    private static final long NEVER = Long.MIN_VALUE / 2;

    private final long[] times;
    private final int windowTicks;
    /** Indice della più vecchia delle ultime {@code max} intenzioni ammesse. */
    private int oldest;

    public InputRateLimit() {
        this(DEFAULT_MAX, DEFAULT_WINDOW_TICKS);
    }

    public InputRateLimit(int max, int windowTicks) {
        if (max <= 0 || windowTicks <= 0) throw new IllegalArgumentException("max e windowTicks devono essere > 0");
        this.times = new long[max];
        this.windowTicks = windowTicks;
        java.util.Arrays.fill(times, NEVER);
    }

    /** Vero (e la registra) se un'intenzione al tick {@code now} rientra nel limite; falso se va ignorata. */
    public boolean allow(long now) {
        if (now - times[oldest] < windowTicks) return false;
        times[oldest] = now;
        oldest = (oldest + 1) % times.length;
        return true;
    }
}
