package dev.qrm.boss.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActionLimiterTest {
    private static final class MutableClock extends Clock {
        long now;

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(now);
        }
    }

    @Test
    void burstsAreDroppedUntilTheIntervalPasses() {
        MutableClock clock = new MutableClock();
        ActionLimiter limiter = new ActionLimiter(clock);
        UUID a = UUID.randomUUID();
        assertTrue(limiter.tryAcquire(a, 250));
        clock.now = 100;
        assertFalse(limiter.tryAcquire(a, 250));
        assertTrue(limiter.tryAcquire(UUID.randomUUID(), 250), "un altro giocatore non e' limitato");
        clock.now = 250;
        assertTrue(limiter.tryAcquire(a, 250));
        limiter.forget(a);
        assertTrue(limiter.tryAcquire(a, 250));
    }
}
