package dev.qrm.boss.logic;

import dev.qrm.api.economy.Currency;
import java.util.Locale;

/**
 * Importo richiesto dal menu: {@code "all"}, una percentuale intera ({@code "25%"}) oppure un valore decimale libero
 * ({@code "500"}, {@code "12.50"}). Le percentuali si calcolano sul saldo di partenza e arrotondano per difetto.
 */
public final class AmountSpec {
    public static final String ALL = "all";

    private AmountSpec() {}

    /** @return l'importo in unità minime (>= 0), oppure -1 se il testo non è un importo valido */
    public static long resolve(String text, long base, Currency currency) {
        if (text == null) return -1;
        String t = text.strip().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return -1;
        if (t.equals(ALL)) return base;
        if (t.endsWith("%")) {
            int pct;
            try {
                pct = Integer.parseInt(t.substring(0, t.length() - 1).strip());
            } catch (NumberFormatException e) {
                return -1;
            }
            if (pct < 1 || pct > 100) return -1;
            // floor(base * pct / 100) senza overflow: base = 100q + r
            return (base / 100) * pct + ((base % 100) * pct) / 100;
        }
        try {
            return currency.parseAmount(t);
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }
}
