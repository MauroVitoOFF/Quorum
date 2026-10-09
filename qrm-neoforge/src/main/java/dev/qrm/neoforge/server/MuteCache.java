package dev.qrm.neoforge.server;

import dev.qrm.api.moderation.Mute;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * I mute dei giocatori online, in memoria: evita una lettura del database a ogni messaggio. Si carica al login, si aggiorna a ogni
 * mute e unmute e si svuota al logout. Un mute scaduto non vale più (e viene tolto alla prima lettura). Pura.
 */
public final class MuteCache {
    private final Map<UUID, Mute> mutes = new ConcurrentHashMap<>();

    public void put(Mute mute) {
        mutes.put(mute.player(), mute);
    }

    /** Riallinea la voce di un giocatore dopo un cambio di mute; per chi è offline non si conserva nulla (non c'è un logout che la tolga). */
    public void refresh(UUID player, Optional<Mute> mute, boolean online) {
        if (online && mute.isPresent()) put(mute.get());
        else remove(player);
    }

    public void remove(UUID player) {
        mutes.remove(player);
    }

    /** Il mute valido all'istante {@code now}, se c'è. */
    public Optional<Mute> active(UUID player, Instant now) {
        Mute m = mutes.get(player);
        if (m == null) return Optional.empty();
        if (!m.activeAt(now)) {
            mutes.remove(player, m);
            return Optional.empty();
        }
        return Optional.of(m);
    }

    public boolean isEmpty() {
        return mutes.isEmpty();
    }
}
