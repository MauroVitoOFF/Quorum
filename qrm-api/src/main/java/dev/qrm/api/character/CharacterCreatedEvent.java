package dev.qrm.api.character;

import dev.qrm.api.event.Event;

public record CharacterCreatedEvent(PlayerCharacter character) implements Event {}
