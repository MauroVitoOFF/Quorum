package dev.qrm.locker.logic;

import dev.qrm.api.id.CharacterId;
import java.util.Optional;

/** Lettura e scrittura del documento di un armadietto. */
public interface LockerStorage {
    Optional<String> read(CharacterId character, String key);

    void write(CharacterId character, String key, String json);
}
