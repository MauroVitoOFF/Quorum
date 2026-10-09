package dev.qrm.bank.logic;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Intervallo minimo fra due richieste dello stesso giocatore (l'intervallo è un parametro: è configurabile). */
public final class ActionLimiter {
    private final Clock clock;
    private final Map<UUID, Long> last = new ConcurrentHashMap<>();

    public ActionLimiter(Clock clock) {
        this.clock = clock;
    }

    /** @return true se la richiesta può procedere (e la registra) */
    public boolean tryAcquire(UUID player, long minIntervalMillis) {
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
