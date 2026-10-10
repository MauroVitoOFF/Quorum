package dev.qrm.core.point;

import dev.qrm.api.point.PointType;
import dev.qrm.api.point.PointTypeRegistry;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPointTypeRegistry implements PointTypeRegistry {
    private final Map<String, PointType> types = new ConcurrentHashMap<>();

    @Override
    public void register(PointType type) {
        PointType existing = types.putIfAbsent(type.id(), type);
        if (existing != null && !existing.equals(type)) {
            throw new IllegalArgumentException("Point type " + type.id() + " already registered with a different definition");
        }
    }

    @Override
    public Optional<PointType> find(String id) {
        return Optional.ofNullable(types.get(id));
    }

    @Override
    public Collection<PointType> all() {
        return types.values();
    }
}
