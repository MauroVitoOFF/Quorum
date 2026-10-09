package dev.qrm.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MoneyTest {
    @Test
    void ofCreatesValue() {
        Money m = Money.of("USD", 250);
        assertEquals("USD", m.currency());
        assertEquals(250, m.amount());
    }

    @Test
    void negativeAmountRejected() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("USD", -1));
    }

    @Test
    void blankCurrencyRejected() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(" ", 1));
    }

    @Test
    void plusSameCurrency() {
        assertEquals(Money.of("USD", 300), Money.of("USD", 100).plus(Money.of("USD", 200)));
    }

    @Test
    void plusDifferentCurrencyRejected() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("USD", 1).plus(Money.of("EUR", 1)));
    }

    @Test
    void minusCannotGoNegative() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("USD", 1).minus(Money.of("USD", 2)));
    }

    @Test
    void plusOverflowThrows() {
        assertThrows(ArithmeticException.class, () -> Money.of("USD", Long.MAX_VALUE).plus(Money.of("USD", 1)));
    }

    @Test
    void isZero() {
        assertTrue(Money.of("USD", 0).isZero());
        assertFalse(Money.of("USD", 1).isZero());
    }
}
