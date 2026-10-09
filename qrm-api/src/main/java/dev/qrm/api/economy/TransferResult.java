package dev.qrm.api.economy;

import dev.qrm.api.Money;
import dev.qrm.api.id.AccountId;

public sealed interface TransferResult {
    record Success(Transaction transaction) implements TransferResult {}

    /** Stessa idempotencyKey già applicata: nessuna nuova scrittura. */
    record Duplicate(Transaction original) implements TransferResult {}

    record InsufficientFunds(Money requested, Money available) implements TransferResult {}

    record AccountNotFound(AccountId id) implements TransferResult {}

    record AccountFrozen(AccountId id) implements TransferResult {}

    record UnknownCurrency(String code) implements TransferResult {}

    record InvalidRequest(String reason) implements TransferResult {}

    record Cancelled(String by, String reason) implements TransferResult {}
}
