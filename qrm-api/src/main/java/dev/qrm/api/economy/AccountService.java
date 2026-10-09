package dev.qrm.api.economy;

import dev.qrm.api.Money;
import dev.qrm.api.id.AccountId;
import java.util.List;
import java.util.Optional;

public interface AccountService {
    /**
     * Crea un conto. In v0.1 lo scoperto ({@code overdraftAllowed}) e' riservato ai conti SYSTEM:
     * per ogni altro tipo di proprietario con {@code true} lancia {@link IllegalArgumentException}.
     * Poiche' solo il conto SYSTEM puo' andare in negativo, {@link #balance} e {@link #balances}
     * riportano un saldo negativo come 0; la vista esatta del libro mastro e' {@code AuditService}.
     */
    Account create(OwnerRef owner, boolean overdraftAllowed);

    Optional<Account> find(AccountId id);

    List<Account> accountsOf(OwnerRef owner);

    /** Conto di sistema (scoperto consentito) per creare/distruggere denaro. Creato al primo uso. */
    Account systemAccount();

    /** Saldo in una valuta; 0 se il conto non l'ha mai usata o e' in scoperto (SYSTEM). */
    Money balance(AccountId id, String currency);

    /** Tutti i saldi positivi del conto (saldi nulli o negativi omessi). */
    List<Money> balances(AccountId id);

    /** @return false se il conto non esiste. */
    boolean setStatus(AccountId id, AccountStatus status);
}
