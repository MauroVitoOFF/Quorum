package dev.qrm.api.point;

import java.util.Collection;
import java.util.Optional;

public interface PointTypeRegistry {
    /** Registra un tipo. La stessa definizione è consentita; una diversa con lo stesso id lancia {@link IllegalArgumentException}. */
    void register(PointType type);

    Optional<PointType> find(String id);

    Collection<PointType> all();
}
