package dev.qrm.api.economy;

import dev.qrm.api.id.AccountId;
import java.util.List;
import java.util.Optional;

public interface TransactionService {
    TransferResult transfer(TransferRequest request);

    Optional<Transaction> findByKey(String idempotencyKey);

    /** Più recenti per prime. */
    List<Transaction> history(AccountId account, int limit);
}
