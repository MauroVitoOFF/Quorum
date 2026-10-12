package dev.qrm.boss.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.economy.Currency;
import org.junit.jupiter.api.Test;

class AmountSpecTest {
    private static final Currency USD = new Currency("USD", "$", 2);

    @Test
    void fixedAndFreeValuesAreDecimalUnits() {
        assertEquals(500_00, AmountSpec.resolve("500", 1, USD));
        assertEquals(12_50, AmountSpec.resolve(" 12.50 ", 1, USD));
    }

    @Test
    void percentagesFloorOnTheBase() {
        assertEquals(250, AmountSpec.resolve("25%", 1001, USD));
        assertEquals(500, AmountSpec.resolve("50%", 1001, USD));
        assertEquals(0, AmountSpec.resolve("25%", 3, USD));
        assertEquals(1001, AmountSpec.resolve("100%", 1001, USD));
    }

    @Test
    void percentagesDoNotOverflow() {
        long base = Long.MAX_VALUE;
        assertEquals(base / 2, AmountSpec.resolve("50%", base, USD));
    }

    @Test
    void allIsTheWholeBase() {
        assertEquals(777, AmountSpec.resolve("ALL", 777, USD));
        assertEquals(0, AmountSpec.resolve("all", 0, USD));
    }

    @Test
    void garbageIsRejected() {
        for (String bad : new String[] {null, "", "  ", "abc", "-1", "1,5", "0%", "101%", "x%", "1.234", "%"}) {
            assertEquals(-1, AmountSpec.resolve(bad, 1000, USD), String.valueOf(bad));
        }
    }
}
