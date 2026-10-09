package dev.qrm.bank.logic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Tagli del contante per valuta, da {@code config/qrm_bank/cash.json}. Regole: codice valuta {@code [A-Z0-9_]{2,16}},
 * decimali 0..8, tagli interi &gt; 0 e distinti, ognuno multiplo del precedente più piccolo (così la scomposizione
 * greedy è ottima).
 */
public final class CashDenominations {
    public static final String DEFAULT_JSON = """
            {
              "QRM": { "decimals": 2, "denominations": [100, 500, 1000, 5000, 10000] }
            }
            """;

    private static final Pattern CODE = Pattern.compile("[A-Z0-9_]{2,16}");
    private static final long MAX_DENOMINATION = 1_000_000_000_000L;

    /** Dichiarazione grezza di una valuta (tagli in qualsiasi ordine). */
    public record Spec(int decimals, List<Long> denominations) {}

    private record Entry(int decimals, List<Long> descending) {}

    private final Map<String, Entry> entries;

    private CashDenominations(Map<String, Entry> entries) {
        this.entries = entries;
    }

    /** @throws IllegalArgumentException se una regola è violata */
    public static CashDenominations of(Map<String, Spec> raw) {
        Map<String, Entry> out = new TreeMap<>();
        for (Map.Entry<String, Spec> e : raw.entrySet()) {
            String code = e.getKey();
            Spec spec = e.getValue();
            if (!CODE.matcher(code).matches()) throw new IllegalArgumentException("invalid currency code: " + code);
            if (spec.decimals() < 0 || spec.decimals() > 8) {
                throw new IllegalArgumentException(code + ": decimals must be 0..8");
            }
            List<Long> ascending = spec.denominations().stream().sorted().toList();
            if (ascending.isEmpty()) throw new IllegalArgumentException(code + ": no denominations");
            for (int i = 0; i < ascending.size(); i++) {
                long v = ascending.get(i);
                if (v <= 0 || v > MAX_DENOMINATION) {
                    throw new IllegalArgumentException(code + ": denomination out of range: " + v);
                }
                if (i == 0) continue;
                long previous = ascending.get(i - 1);
                if (v == previous) throw new IllegalArgumentException(code + ": duplicate denomination " + v);
                if (v % previous != 0) {
                    throw new IllegalArgumentException(code + ": " + v + " is not a multiple of " + previous);
                }
            }
            List<Long> descending = new ArrayList<>(ascending);
            Collections.reverse(descending);
            out.put(code, new Entry(spec.decimals(), List.copyOf(descending)));
        }
        return new CashDenominations(Collections.unmodifiableMap(out));
    }

    /** @throws IllegalArgumentException se il JSON è malformato o viola le regole */
    public static CashDenominations parse(String json) {
        Map<String, Spec> raw = new TreeMap<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                if (!o.has("decimals") || !o.has("denominations")) {
                    throw new IllegalArgumentException(e.getKey() + ": needs decimals and denominations");
                }
                int decimals = new BigDecimal(o.get("decimals").getAsString()).intValueExact();
                List<Long> values = new ArrayList<>();
                for (JsonElement el : o.getAsJsonArray("denominations")) {
                    values.add(new BigDecimal(el.getAsString()).longValueExact());
                }
                raw.put(e.getKey(), new Spec(decimals, values));
            }
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException | ClassCastException
                | NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("invalid cash.json: " + e.getMessage(), e);
        }
        return of(raw);
    }

    public boolean has(String currency) {
        return entries.containsKey(currency);
    }

    /** Valute con contante, in ordine alfabetico. */
    public Set<String> currencies() {
        return entries.keySet();
    }

    public int decimals(String currency) {
        Entry e = entries.get(currency);
        if (e == null) throw new IllegalArgumentException("no cash for " + currency);
        return e.decimals();
    }

    /** Tagli in ordine decrescente; vuota se la valuta non ha contante. */
    public List<Long> descending(String currency) {
        Entry e = entries.get(currency);
        return e == null ? List.of() : e.descending();
    }

    /** Importo leggibile ("5.00 EUR") usato nel nome dell'item. */
    public String label(String currency, long amount) {
        return BigDecimal.valueOf(amount, decimals(currency)).toPlainString() + " " + currency;
    }

    public static String itemName(String currency, long amount) {
        return "cash_" + currency.toLowerCase(Locale.ROOT) + "_" + amount;
    }
}
