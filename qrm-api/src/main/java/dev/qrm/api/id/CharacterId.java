package dev.qrm.api.id;

import java.util.Objects;
import java.util.UUID;

public record CharacterId(UUID value) {
    public CharacterId {
        Objects.requireNonNull(value, "value");
    }

    public static CharacterId random() {
        return new CharacterId(UUID.randomUUID());
    }
}
