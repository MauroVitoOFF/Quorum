package dev.qrm.api.moderation;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Un avvertimento dato a un giocatore dallo staff ({@code id} è un numero progressivo). */
public record Warning(long id, UUID player, String playerName, Optional<UUID> staffId, String staffName, String reason,
        Instant at) {}
