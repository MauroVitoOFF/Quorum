package dev.qrm.api.event;

import java.util.function.Consumer;

public interface EventBus {
    /**
     * @param owner nome del mod/modulo, usato nei log se l'handler lancia un'eccezione
     * @param ignoreCancelled se true l'handler viene saltato quando l'evento è già annullato
     */
    <E extends Event> Registration register(
            Class<E> type, Priority priority, boolean ignoreCancelled, String owner, Consumer<? super E> handler);

    default <E extends Event> Registration register(Class<E> type, String owner, Consumer<? super E> handler) {
        return register(type, Priority.NORMAL, false, owner, handler);
    }

    /** Consegna l'evento in modo sincrono e lo restituisce. */
    <E extends Event> E post(E event);
}
