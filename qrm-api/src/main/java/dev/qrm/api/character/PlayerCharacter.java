package dev.qrm.api.character;

import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import java.time.Instant;
import java.time.LocalDate;

public record PlayerCharacter(
        CharacterId id,
        PlayerId playerId,
        String firstName,
        String lastName,
        LocalDate birthDate,
        CharacterStatus status,
        Instant createdAt) {}
