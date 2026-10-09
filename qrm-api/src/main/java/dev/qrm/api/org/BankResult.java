package dev.qrm.api.org;

import dev.qrm.api.economy.TransferResult;

public sealed interface BankResult {
    /** Il permesso c'era: l'esito è quello di {@code TransactionService}. */
    record Transferred(TransferResult result) implements BankResult {}

    /** Manca il permesso {@code node} (oggi sempre {@code org.account.withdraw}). */
    record Denied(String node) implements BankResult {}

    /** Organizzazione inesistente o archiviata. */
    record Failed(OrgError error) implements BankResult {}
}
