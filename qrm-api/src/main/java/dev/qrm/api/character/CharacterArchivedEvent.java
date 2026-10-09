package dev.qrm.api.character;

import dev.qrm.api.event.Event;

public record CharacterArchivedEvent(PlayerCharacter character) implements Event {}
