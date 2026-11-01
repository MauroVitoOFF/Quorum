package dev.qrm.api.internal;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Uso interno di QRM Core e degli adattatori: non fa parte dell'API pubblica e può cambiare senza preavviso. */
public final class ServiceRegistry {
    private static final Map<Class<?>, Object> SERVICES = new ConcurrentHashMap<>();

    private ServiceRegistry() {}

    public static <T> T get(Class<T> type) {
        Object service = SERVICES.get(type);
        if (service == null) {
            throw new IllegalStateException("QRM service not available: " + type.getName()
                    + " (QRM is not ready yet - wait for QrmReadyEvent)");
        }
        return type.cast(service);
    }

    public static <T> void register(Class<T> type, T implementation) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(implementation, "implementation");
        if (SERVICES.putIfAbsent(type, implementation) != null) {
            throw new IllegalStateException("Service already registered: " + type.getName());
        }
    }

    public static void unregister(Class<?> type) {
        SERVICES.remove(type);
    }

    /** Solo per i test. */
    public static void clear() {
        SERVICES.clear();
    }
}
