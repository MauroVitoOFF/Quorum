package dev.qrm.core.economy;

import dev.qrm.api.Money;
import dev.qrm.api.economy.Account;
import dev.qrm.api.economy.Currency;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.economy.TransferRequest;
import dev.qrm.api.economy.TransferResult;
import dev.qrm.api.id.CharacterId;
import dev.qrm.core.TestClock;
import dev.qrm.core.db.Database;
import dev.qrm.core.db.Migrator;
import dev.qrm.core.event.SimpleEventBus;
import java.time.Instant;
import java.util.UUID;

public final class EconomyEnv implements AutoCloseable {
    public final Database db = Database.open("jdbc:sqlite::memory:");
    public final SimpleEventBus bus = new SimpleEventBus();
    public final TestClock clock = new TestClock(Instant.parse("2026-01-01T00:00:00Z"));
    public final InMemoryCurrencyRegistry currencies = new InMemoryCurrencyRegistry();
    public final DefaultAccountService accounts;
    public final DefaultTransactionService txs;

    public EconomyEnv() {
        Migrator.migrate(db);
        currencies.register(new Currency("USD", "$", 2));
        currencies.register(new Currency("EUR", "€", 2));
        accounts = new DefaultAccountService(db, bus, clock);
        txs = new DefaultTransactionService(db, bus, clock, currencies);
    }

    public Account newAccount() {
        return accounts.create(OwnerRef.character(CharacterId.random()), false);
    }

    public void fund(Account account, String currency, long amount) {
        TransferResult r = txs.transfer(TransferRequest.builder()
                .from(accounts.systemAccount().id())
                .to(account.id())
                .amount(Money.of(currency, amount))
                .reason("test.fund")
                .idempotencyKey("fund-" + UUID.randomUUID())
                .build());
        if (!(r instanceof TransferResult.Success)) throw new IllegalStateException("fund failed: " + r);
    }

    @Override
    public void close() {
        db.close();
    }
}
