package dev.qrm.locker.logic;

import dev.qrm.api.data.ModDataService;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.id.CharacterId;
import java.util.Optional;

/** I documenti stanno nei dati dei mod di QRM, sotto il namespace {@code qrm_locker}. */
public final class ModDataLockerStorage implements LockerStorage {
    private final ModDataService data;

    public ModDataLockerStorage(ModDataService data) {
        this.data = data;
    }

    @Override
    public Optional<String> read(CharacterId character, String key) {
        return data.get(LockerKeys.NAMESPACE, OwnerRef.character(character), key);
    }

    @Override
    public void write(CharacterId character, String key, String json) {
        data.put(LockerKeys.NAMESPACE, OwnerRef.character(character), key, json);
    }
}
