package dev.qrm.api.economy;

import java.util.Collection;
import java.util.Optional;

public interface CurrencyRegistry {
    /** Registra una valuta. Ri-registrare la stessa definizione è consentito; una definizione diversa con lo stesso codice no. */
    void register(Currency currency);

    Optional<Currency> find(String code);

    Collection<Currency> all();
}
