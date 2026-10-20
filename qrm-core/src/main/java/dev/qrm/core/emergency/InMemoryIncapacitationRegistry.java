package dev.qrm.core.emergency;

import dev.qrm.api.emergency.IncapacitationRegistry;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** Registro in memoria, thread-safe, dei controlli sullo stato "a terra". */
public final class InMemoryIncapacitationRegistry implements IncapacitationRegistry {
    private final Map<String, Predicate<UUID>> checks = new ConcurrentHashMap<>();

    @Override
    public void register(String moduleId, Predicate<UUID> check) {
        if (checks.putIfAbsent(moduleId, check) != null) {
            throw new IllegalArgumentException("Incapacitation check already registered for module " + moduleId);
        }
    }

    @Override
    public boolean isIncapacitated(UUID playerId) {
        for (Predicate<UUID> check : checks.values()) {
            if (check.test(playerId)) {
                return true;
            }
        }
        return false;
    }
}
