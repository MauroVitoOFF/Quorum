package dev.qrm.api.economy;

import dev.qrm.api.id.CharacterId;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Titolare di un conto. Il tipo è estendibile da altri moduli (es. "ORGANIZATION"). */
public record OwnerRef(String type, UUID id) {
    public static final String CHARACTER = "CHARACTER";
    public static final String SYSTEM = "SYSTEM";
    public static final String ORGANIZATION = "ORGANIZATION";
    private static final Pattern TYPE = Pattern.compile("[A-Z0-9_]{1,32}");

    public OwnerRef {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
        if (!TYPE.matcher(type).matches()) throw new IllegalArgumentException("invalid owner type: " + type);
    }

    public static OwnerRef character(CharacterId id) {
        return new OwnerRef(CHARACTER, id.value());
    }

    public static OwnerRef system() {
        return new OwnerRef(SYSTEM, new UUID(0L, 0L));
    }
}
