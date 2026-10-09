package dev.qrm.neoforge.server;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RateLimiterTest {
    private static final class MutableClock extends Clock {
        long millis = 1_000;
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis); }
        @Override public long millis() { return millis; }
    }

    @Test
    void firstCallPassesThenBlocksUntilIntervalElapsed() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(250, clock);
        UUID p = UUID.randomUUID();
        assertTrue(limiter.tryAcquire(p));
        clock.millis += 249;
        assertFalse(limiter.tryAcquire(p));
        clock.millis += 1;
        assertTrue(limiter.tryAcquire(p));
    }

    @Test
    void playersAreIndependentAndForgetResets() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(250, clock);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertTrue(limiter.tryAcquire(a));
        assertTrue(limiter.tryAcquire(b));
        assertFalse(limiter.tryAcquire(a));
        limiter.forget(a);
        assertTrue(limiter.tryAcquire(a));
    }
}
