package dev.qrm.api.org;

import dev.qrm.api.Money;
import dev.qrm.api.id.AccountId;
import dev.qrm.api.id.CharacterId;

public interface OrganizationBankService {
    String WITHDRAW_NODE = "org.account.withdraw";

    /**
     * Prelievo dal conto dell'organizzazione per conto di un personaggio: richiede
     * {@code can(by, orgId, WITHDRAW_NODE)}. I depositi non passano di qui (chiunque può trasferire verso il conto).
     *
     * <p>Lo spazio di {@code idempotencyKey} è globale a {@code TransactionService}: una chiave già usata da QUALSIASI
     * altra transazione fa restituire {@code BankResult.Transferred(TransferResult.Duplicate(<quell'altra transazione>))},
     * anche se non c'entra con questa organizzazione o con questo prelievo. I chiamanti dovrebbero prefissare le chiavi
     * (es. {@code "org:<orgId>:<scopo>:<uuid>"}).
     */
    BankResult transfer(CharacterId by, String orgId, AccountId to, Money amount, String idempotencyKey);
}
