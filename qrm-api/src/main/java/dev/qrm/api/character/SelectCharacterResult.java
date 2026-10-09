package dev.qrm.api.character;

public sealed interface SelectCharacterResult {
    record Selected(PlayerCharacter character) implements SelectCharacterResult {}

    record NotFound() implements SelectCharacterResult {}

    record NotOwner() implements SelectCharacterResult {}

    record NotUsable(CharacterStatus status) implements SelectCharacterResult {}
}
