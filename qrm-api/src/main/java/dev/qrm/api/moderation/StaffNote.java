package dev.qrm.api.moderation;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Un appunto privato dello staff su un giocatore ({@code id} è un numero progressivo). */
public record StaffNote(long id, UUID player, String playerName, Optional<UUID> staffId, String staffName, String text,
        Instant at) {}
