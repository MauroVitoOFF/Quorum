package dev.qrm.arrival.logic;

import dev.qrm.api.character.CharacterStatus;
import dev.qrm.api.character.PlayerCharacter;

/** Chi deve vivere l'arrivo: un Character attivo, creato dopo l'installazione, che non l'ha ancora vissuto. */
public final class ArrivalPolicy {
    private final ArrivalStorage storage;

    public ArrivalPolicy(ArrivalStorage storage) {
        this.storage = storage;
    }

    public boolean shouldArrive(PlayerCharacter character) {
        return character.status() == CharacterStatus.ACTIVE
                && !storage.hasArrived(character.id())
                && !character.createdAt().isBefore(storage.installedAt());
    }
}
