package dev.qrm.core.event;

import dev.qrm.api.event.CancellableEvent;
import dev.qrm.api.event.Event;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.event.Priority;
import dev.qrm.api.event.Registration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class SimpleEventBus implements EventBus {
    private static final System.Logger LOG = System.getLogger("qrm.events");

    private record Handler(
            Class<?> type, Priority priority, boolean ignoreCancelled, String owner, Consumer<Object> fn, long seq) {}

    private final CopyOnWriteArrayList<Handler> handlers = new CopyOnWriteArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public <E extends Event> Registration register(
            Class<E> type, Priority priority, boolean ignoreCancelled, String owner, Consumer<? super E> handler) {
        Handler h = new Handler(
                type, priority, ignoreCancelled, owner, o -> handler.accept(type.cast(o)), sequence.getAndIncrement());
        handlers.add(h);
        return () -> handlers.remove(h);
    }

    @Override
    public <E extends Event> E post(E event) {
        List<Handler> matching = handlers.stream()
                .filter(h -> h.type().isInstance(event))
                .sorted(Comparator.comparingInt((Handler h) -> h.priority().ordinal()).thenComparingLong(Handler::seq))
                .toList();
        for (Handler h : matching) {
            if (h.ignoreCancelled() && event instanceof CancellableEvent c && c.isCancelled()) continue;
            try {
                h.fn().accept(event);
            } catch (Throwable t) {
                LOG.log(System.Logger.Level.ERROR,
                        "Handler of '" + h.owner() + "' failed for " + event.getClass().getSimpleName(), t);
            }
        }
        return event;
    }
}
