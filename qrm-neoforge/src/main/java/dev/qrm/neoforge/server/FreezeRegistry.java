package dev.qrm.neoforge.server;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giocatori congelati dallo staff e il punto in cui sono stati bloccati. Pura e generica nel tipo del mondo {@code L}
 * (in gioco è {@code ServerLevel}); si azzera al logout. Il blocco vero e proprio lo fa il tick del giocatore.
 */
public final class FreezeRegistry<L> {
    /** Il punto di blocco: mondo, posizione e direzione dello sguardo. */
    public record Anchor<L>(L level, double x, double y, double z, float yRot, float xRot) {}

    private final Map<UUID, Anchor<L>> frozen = new ConcurrentHashMap<>();

    /** @return true se dopo la chiamata il giocatore è congelato, false se è stato scongelato */
    public boolean toggle(UUID player, Anchor<L> where) {
        if (frozen.remove(player) != null) return false;
        frozen.put(player, where);
        return true;
    }

    public Optional<Anchor<L>> anchorOf(UUID player) {
        return Optional.ofNullable(frozen.get(player));
    }

    public boolean isFrozen(UUID player) {
        return frozen.containsKey(player);
    }

    public void forget(UUID player) {
        frozen.remove(player);
    }

    public boolean isEmpty() {
        return frozen.isEmpty();
    }
}
