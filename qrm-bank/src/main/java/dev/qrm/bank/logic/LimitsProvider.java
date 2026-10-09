package dev.qrm.bank.logic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface LimitsProvider {
    BankLimits forCurrency(String currency);

    /** Voci "CODICE:perOperazione:alGiorno"; le valute non elencate usano {@code defaults}. */
    static LimitsProvider of(BankLimits defaults, List<String> overrides) {
        Map<String, BankLimits> map = new HashMap<>();
        for (String entry : overrides) {
            String[] p = entry.split(":");
            if (p.length != 3) throw new IllegalArgumentException("invalid limit override: " + entry);
            try {
                map.put(p[0], new BankLimits(Long.parseLong(p[1]), Long.parseLong(p[2])));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid limit override: " + entry, e);
            }
        }
        return currency -> map.getOrDefault(currency, defaults);
    }

    static boolean isValidOverride(String entry) {
        try {
            of(BankLimits.DEFAULT, List.of(entry));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
