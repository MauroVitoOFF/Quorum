package dev.qrm.api.character;

public sealed interface CreateCharacterResult {
    record Created(PlayerCharacter character) implements CreateCharacterResult {}

    record UnknownPlayer() implements CreateCharacterResult {}

    record SlotsFull() implements CreateCharacterResult {}

    record InvalidName(String reason) implements CreateCharacterResult {}

    record NameTaken() implements CreateCharacterResult {}
}
