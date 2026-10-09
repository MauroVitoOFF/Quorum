package dev.qrm.core.economy;

import dev.qrm.api.economy.Currency;
import dev.qrm.api.economy.CurrencyRegistry;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCurrencyRegistry implements CurrencyRegistry {
    private final Map<String, Currency> currencies = new ConcurrentHashMap<>();

    @Override
    public void register(Currency currency) {
        Currency existing = currencies.putIfAbsent(currency.code(), currency);
        if (existing != null && !existing.equals(currency)) {
            throw new IllegalArgumentException("Currency " + currency.code() + " already registered with a different definition");
        }
    }

    /** Rimuove una valuta (uso interno dell'adattatore, es. a fine mondo). No-op se assente. */
    public void remove(String code) {
        currencies.remove(code);
    }

    @Override
    public Optional<Currency> find(String code) {
        return Optional.ofNullable(currencies.get(code));
    }

    @Override
    public Collection<Currency> all() {
        return currencies.values();
    }
}
