package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CashDenominationsTest {
    private static CashDenominations.Spec spec(int decimals, Long... values) {
        return new CashDenominations.Spec(decimals, List.of(values));
    }

    @Test
    void parsesValidJsonAndSortsDescending() {
        CashDenominations d = CashDenominations.parse(
                "{\"EUR\": {\"decimals\": 2, \"denominations\": [500, 100, 1000]}}");
        assertTrue(d.has("EUR"));
        assertFalse(d.has("USD"));
        assertEquals(List.of(1000L, 500L, 100L), d.descending("EUR"));
        assertEquals(2, d.decimals("EUR"));
        assertEquals(List.of(), d.descending("USD"));
    }

    @Test
    void defaultJsonIsValid() {
        CashDenominations d = CashDenominations.parse(CashDenominations.DEFAULT_JSON);
        assertTrue(d.has("QRM"));
        assertEquals(List.of(10000L, 5000L, 1000L, 500L, 100L), d.descending("QRM"));
    }

    @Test
    void rejectsDenominationsThatAreNotMultiplesOfTheSmallerOne() {
        assertThrows(IllegalArgumentException.class, () -> CashDenominations.of(Map.of("EUR", spec(2, 100L, 250L))));
    }

    @Test
    void rejectsDuplicatesNonPositiveAndEmpty() {
        assertThrows(IllegalArgumentException.class, () -> CashDenominations.of(Map.of("EUR", spec(2, 100L, 100L))));
        assertThrows(IllegalArgumentException.class, () -> CashDenominations.of(Map.of("EUR", spec(2, 0L))));
        assertThrows(IllegalArgumentException.class, () -> CashDenominations.of(Map.of("EUR", spec(2, -5L))));
        assertThrows(IllegalArgumentException.class, () -> CashDenominations.of(Map.of("EUR", spec(2))));
    }

    @Test
    void rejectsBadCurrencyCodeAndDecimals() {
        assertThrows(IllegalArgumentException.class, () -> CashDenominations.of(Map.of("eur", spec(2, 100L))));
        assertThrows(IllegalArgumentException.class, () -> CashDenominations.of(Map.of("EUR", spec(9, 100L))));
        assertThrows(IllegalArgumentException.class, () -> CashDenominations.of(Map.of("EUR", spec(-1, 100L))));
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(IllegalArgumentException.class, () -> CashDenominations.parse("not json"));
        assertThrows(IllegalArgumentException.class, () -> CashDenominations.parse("[1,2]"));
        assertThrows(IllegalArgumentException.class,
                () -> CashDenominations.parse("{\"EUR\": {\"decimals\": 2, \"denominations\": [1.5]}}"));
        assertThrows(IllegalArgumentException.class, () -> CashDenominations.parse("{\"EUR\": {\"decimals\": 2}}"));
    }

    @Test
    void itemNameAndLabel() {
        CashDenominations d = CashDenominations.of(Map.of("EUR", spec(2, 100L, 500L)));
        assertEquals("cash_eur_500", CashDenominations.itemName("EUR", 500));
        assertEquals("5.00 EUR", d.label("EUR", 500));
    }
}
