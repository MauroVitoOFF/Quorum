package dev.qrm.neoforge.server;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Intervallo minimo fra due richieste dello stesso giocatore. */
public final class RateLimiter {
    private final long minIntervalMillis;
    private final Clock clock;
    private final Map<UUID, Long> last = new ConcurrentHashMap<>();

    public RateLimiter(long minIntervalMillis, Clock clock) {
        this.minIntervalMillis = minIntervalMillis;
        this.clock = clock;
    }

    /** @return true se la richiesta può procedere (e la registra), false se arriva troppo presto */
    public boolean tryAcquire(UUID player) {
        long now = clock.millis();
        boolean[] allowed = {false};
        last.compute(player, (k, previous) -> {
            if (previous == null || now - previous >= minIntervalMillis) {
                allowed[0] = true;
                return now;
            }
            return previous;
        });
        return allowed[0];
    }

    public void forget(UUID player) {
        last.remove(player);
    }
}
