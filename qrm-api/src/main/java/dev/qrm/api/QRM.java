package dev.qrm.api;

import dev.qrm.api.event.EventBus;
import dev.qrm.api.internal.ServiceRegistry;

/** Registro dei servizi QRM: unico punto d'accesso per i mod di terzi. */
public final class QRM {
    private QRM() {}

    public static <T> T get(Class<T> type) {
        return ServiceRegistry.get(type);
    }

    /** Scorciatoia per {@code get(EventBus.class)}; disponibile appena QRM è caricato. */
    public static EventBus events() {
        return get(EventBus.class);
    }
}
