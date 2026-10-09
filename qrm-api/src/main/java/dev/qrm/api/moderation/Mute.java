package dev.qrm.api.moderation;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Il silenziamento di un giocatore. {@code expiresAt} vuoto = permanente. {@code staffId} vuoto = la console.
 * Un giocatore ha al massimo un mute attivo: uno nuovo sostituisce il precedente.
 */
public record Mute(UUID player, String playerName, Optional<UUID> staffId, String staffName, String reason, Instant at,
        Optional<Instant> expiresAt) {
    /** Il mute vale ancora all'istante {@code now}? Un mute permanente vale sempre. */
    public boolean activeAt(Instant now) {
        return expiresAt.map(e -> e.isAfter(now)).orElse(true);
    }

    public boolean permanent() {
        return expiresAt.isEmpty();
    }
}
