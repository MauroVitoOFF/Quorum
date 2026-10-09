package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class LimitsProviderTest {
    private static final BankLimits DEFAULTS = new BankLimits(1000, 5000);

    @Test
    void usesDefaultsWhenNoOverride() {
        assertEquals(DEFAULTS, LimitsProvider.of(DEFAULTS, List.of()).forCurrency("EUR"));
    }

    @Test
    void appliesPerCurrencyOverride() {
        LimitsProvider p = LimitsProvider.of(DEFAULTS, List.of("EUR:200:300"));
        assertEquals(new BankLimits(200, 300), p.forCurrency("EUR"));
        assertEquals(DEFAULTS, p.forCurrency("USD"));
    }

    @Test
    void validatesOverrideSyntax() {
        assertTrue(LimitsProvider.isValidOverride("EUR:1:2"));
        assertFalse(LimitsProvider.isValidOverride("EUR:1"));
        assertFalse(LimitsProvider.isValidOverride("EUR:x:2"));
        assertFalse(LimitsProvider.isValidOverride("EUR:0:2"));
        assertThrows(IllegalArgumentException.class, () -> LimitsProvider.of(DEFAULTS, List.of("bad")));
    }
}
