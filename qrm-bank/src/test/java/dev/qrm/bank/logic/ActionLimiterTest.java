package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActionLimiterTest {
    @Test
    void blocksRequestsThatArriveTooSoonPerPlayer() {
        BankTestClock clock = new BankTestClock(Instant.parse("2026-01-01T00:00:00Z"));
        ActionLimiter limiter = new ActionLimiter(clock);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertTrue(limiter.tryAcquire(a, 250));
        assertFalse(limiter.tryAcquire(a, 250));
        assertTrue(limiter.tryAcquire(b, 250));
        clock.advance(Duration.ofMillis(250));
        assertTrue(limiter.tryAcquire(a, 250));
    }

    @Test
    void forgetResetsPlayer() {
        BankTestClock clock = new BankTestClock(Instant.parse("2026-01-01T00:00:00Z"));
        ActionLimiter limiter = new ActionLimiter(clock);
        UUID a = UUID.randomUUID();
        assertTrue(limiter.tryAcquire(a, 1000));
        limiter.forget(a);
        assertTrue(limiter.tryAcquire(a, 1000));
    }
}
