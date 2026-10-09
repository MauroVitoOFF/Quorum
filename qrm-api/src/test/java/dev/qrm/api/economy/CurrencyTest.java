package dev.qrm.api.economy;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CurrencyTest {
    private final Currency usd = new Currency("USD", "$", 2);
    private final Currency yen = new Currency("JPY", "¥", 0);

    @Test
    void lowercaseCodeRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Currency("usd", "$", 2));
    }

    @Test
    void decimalsOutOfRangeRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Currency("USD", "$", 9));
        assertThrows(IllegalArgumentException.class, () -> new Currency("USD", "$", -1));
    }

    @Test
    void parseAmount() {
        assertEquals(1250, usd.parseAmount("12.50"));
        assertEquals(1250, usd.parseAmount("12.5"));
        assertEquals(1200, usd.parseAmount("12"));
        assertEquals(1250, usd.parseAmount("12.500"));
        assertEquals(5, yen.parseAmount("5"));
    }

    @Test
    void parseRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> usd.parseAmount("12.555"));
        assertThrows(IllegalArgumentException.class, () -> usd.parseAmount("abc"));
        assertThrows(IllegalArgumentException.class, () -> usd.parseAmount("-1"));
        assertThrows(IllegalArgumentException.class, () -> yen.parseAmount("1.5"));
    }

    @Test
    void format() {
        assertEquals("12.50 $", usd.format(1250));
        assertEquals("0.05 $", usd.format(5));
        assertEquals("5 ¥", yen.format(5));
    }

    @Test
    void hostileExponentsAreRejectedQuicklyWithoutExpandingTheNumber() {
        // Il testo arriva dal pannello staff: un esponente enorme non deve bloccare il thread del server.
        for (String text : new String[] {"1e999999999", "1E+2147483647", "0.1e-999999999", "9".repeat(5000)}) {
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(2),
                    () -> assertThrows(IllegalArgumentException.class, () -> usd.parseAmount(text)), text);
        }
        assertEquals(0, usd.parseAmount("0e-999999999"));
    }
}
