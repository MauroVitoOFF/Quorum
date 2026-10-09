package dev.qrm.api;

import dev.qrm.api.event.EventBus;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Registro dei servizi QRM: unico punto d'accesso per i mod di terzi. */
public final class QRM {
    private static final Map<Class<?>, Object> SERVICES = new ConcurrentHashMap<>();

    private QRM() {}

    public static <T> T get(Class<T> type) {
        Object service = SERVICES.get(type);
        if (service == null) {
            throw new IllegalStateException("QRM service not available: " + type.getName()
                    + " (QRM is not ready yet - wait for QrmReadyEvent)");
        }
        return type.cast(service);
    }

    /** Uso interno: chiamato solo da QRM Core/adattatori. */
    public static <T> void register(Class<T> type, T implementation) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(implementation, "implementation");
        if (SERVICES.putIfAbsent(type, implementation) != null) {
            throw new IllegalStateException("Service already registered: " + type.getName());
        }
    }

    /** Uso interno. */
    public static void unregister(Class<?> type) {
        SERVICES.remove(type);
    }

    /** Uso interno (test). */
    public static void clear() {
        SERVICES.clear();
    }

    /** Scorciatoia per {@code get(EventBus.class)}; disponibile appena QRM è caricato. */
    public static EventBus events() {
        return get(EventBus.class);
    }
}
