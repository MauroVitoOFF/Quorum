package dev.qrm.core.event;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.event.CancellableEvent;
import dev.qrm.api.event.Event;
import dev.qrm.api.event.Priority;
import dev.qrm.api.event.Registration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimpleEventBusTest {
    record Ping(List<String> log) implements Event {}

    static class Cancelable extends CancellableEvent {}

    private final SimpleEventBus bus = new SimpleEventBus();

    @Test
    void handlersRunInPriorityOrder() {
        Ping ping = new Ping(new ArrayList<>());
        bus.register(Ping.class, Priority.LATE, false, "t", e -> e.log().add("late"));
        bus.register(Ping.class, Priority.EARLIEST, false, "t", e -> e.log().add("earliest"));
        bus.register(Ping.class, Priority.NORMAL, false, "t", e -> e.log().add("normal"));
        bus.post(ping);
        assertEquals(List.of("earliest", "normal", "late"), ping.log());
    }

    @Test
    void samePriorityKeepsRegistrationOrder() {
        Ping ping = new Ping(new ArrayList<>());
        bus.register(Ping.class, "t", e -> e.log().add("first"));
        bus.register(Ping.class, "t", e -> e.log().add("second"));
        bus.post(ping);
        assertEquals(List.of("first", "second"), ping.log());
    }

    @Test
    void ignoreCancelledSkipsHandlerAfterCancel() {
        List<String> log = new ArrayList<>();
        bus.register(Cancelable.class, Priority.EARLY, false, "canceller", e -> e.cancel("canceller", "no"));
        bus.register(Cancelable.class, Priority.NORMAL, true, "skipped", e -> log.add("skipped"));
        bus.register(Cancelable.class, Priority.LATE, false, "called", e -> log.add("called"));
        Cancelable event = bus.post(new Cancelable());
        assertTrue(event.isCancelled());
        assertEquals(List.of("called"), log);
    }

    @Test
    void firstCancellerWins() {
        Cancelable e = new Cancelable();
        e.cancel("a", "first");
        e.cancel("b", "second");
        assertEquals("a", e.cancelledBy());
        assertEquals("first", e.cancelReason());
    }

    @Test
    void failingHandlerIsIsolated() {
        Ping ping = new Ping(new ArrayList<>());
        bus.register(Ping.class, Priority.EARLY, false, "broken", e -> {
            throw new IllegalStateException("boom");
        });
        bus.register(Ping.class, Priority.NORMAL, false, "ok", e -> e.log().add("ok"));
        bus.post(ping);
        assertEquals(List.of("ok"), ping.log());
    }

    @Test
    void unregisterStopsDelivery() {
        Ping ping = new Ping(new ArrayList<>());
        Registration r = bus.register(Ping.class, "t", e -> e.log().add("x"));
        r.unregister();
        bus.post(ping);
        assertTrue(ping.log().isEmpty());
    }

    @Test
    void postReturnsSameEvent() {
        Ping ping = new Ping(new ArrayList<>());
        assertSame(ping, bus.post(ping));
    }
}
