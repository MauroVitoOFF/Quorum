package dev.qrm.core.economy;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.Money;
import dev.qrm.api.economy.*;
import dev.qrm.api.event.Priority;
import dev.qrm.api.id.AccountId;
import dev.qrm.core.QrmStorageException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionServiceTest {
    private EconomyEnv env;
    private Account a;
    private Account b;

    @BeforeEach
    void setUp() {
        env = new EconomyEnv();
        a = env.newAccount();
        b = env.newAccount();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private TransferRequest.Builder req(Account from, Account to, long amount, String key) {
        return TransferRequest.builder().from(from.id()).to(to.id()).amount(Money.of("USD", amount))
                .reason("test.transfer").idempotencyKey(key);
    }

    private long bal(Account acc) {
        return env.accounts.balance(acc.id(), "USD").amount();
    }

    @Test
    void transferMovesFundsAndRecordsTransaction() {
        env.fund(a, "USD", 1000);
        TransferResult r = env.txs.transfer(req(a, b, 300, "k1").build());
        TransferResult.Success s = assertInstanceOf(TransferResult.Success.class, r);
        assertEquals(700, bal(a));
        assertEquals(300, bal(b));
        assertEquals(s.transaction().id(), s.transaction().groupId());
        assertEquals(s.transaction(), env.txs.findByKey("k1").orElseThrow());
    }

    @Test
    void groupIdIsKeptWhenProvided() {
        env.fund(a, "USD", 1000);
        UUID group = UUID.randomUUID();
        var s = (TransferResult.Success) env.txs.transfer(req(a, b, 10, "k1").groupId(group).build());
        assertEquals(group, s.transaction().groupId());
    }

    @Test
    void insufficientFundsChangesNothing() {
        env.fund(a, "USD", 100);
        TransferResult r = env.txs.transfer(req(a, b, 101, "k1").build());
        var f = assertInstanceOf(TransferResult.InsufficientFunds.class, r);
        assertEquals(Money.of("USD", 101), f.requested());
        assertEquals(Money.of("USD", 100), f.available());
        assertEquals(100, bal(a));
        assertEquals(0, bal(b));
        assertTrue(env.txs.findByKey("k1").isEmpty());
    }

    @Test
    void systemAccountMayOverdraw() {
        Account system = env.accounts.systemAccount();
        TransferResult r = env.txs.transfer(req(system, a, 500, "mint").build());
        assertInstanceOf(TransferResult.Success.class, r);
        assertEquals(500, bal(a));
    }

    @Test
    void frozenAccountsRejectedOnBothSides() {
        env.fund(a, "USD", 100);
        env.accounts.setStatus(b.id(), AccountStatus.FROZEN);
        assertEquals(new TransferResult.AccountFrozen(b.id()), env.txs.transfer(req(a, b, 10, "k1").build()));
        env.accounts.setStatus(b.id(), AccountStatus.ACTIVE);
        env.accounts.setStatus(a.id(), AccountStatus.FROZEN);
        assertEquals(new TransferResult.AccountFrozen(a.id()), env.txs.transfer(req(a, b, 10, "k2").build()));
    }

    @Test
    void missingAccount() {
        AccountId ghost = AccountId.random();
        TransferRequest r = TransferRequest.builder().from(a.id()).to(ghost).amount(Money.of("USD", 1))
                .reason("t").idempotencyKey("k1").build();
        assertEquals(new TransferResult.AccountNotFound(ghost), env.txs.transfer(r));
    }

    @Test
    void invalidRequests() {
        assertInstanceOf(TransferResult.InvalidRequest.class, env.txs.transfer(req(a, b, 0, "k0").build()));
        assertInstanceOf(TransferResult.InvalidRequest.class, env.txs.transfer(req(a, a, 5, "k1").build()));
        TransferRequest unknown = TransferRequest.builder().from(a.id()).to(b.id()).amount(Money.of("XXX", 5))
                .reason("t").idempotencyKey("k2").build();
        assertEquals(new TransferResult.UnknownCurrency("XXX"), env.txs.transfer(unknown));
    }

    @Test
    void sameIdempotencyKeyAppliesOnce() {
        env.fund(a, "USD", 1000);
        var first = (TransferResult.Success) env.txs.transfer(req(a, b, 100, "same").build());
        TransferResult second = env.txs.transfer(req(a, b, 100, "same").build());
        var dup = assertInstanceOf(TransferResult.Duplicate.class, second);
        assertEquals(first.transaction(), dup.original());
        assertEquals(900, bal(a));
        assertEquals(100, bal(b));
    }

    @Test
    void preEventCanCancel() {
        env.fund(a, "USD", 1000);
        List<TransactionPostEvent> posts = new ArrayList<>();
        env.bus.register(TransactionPreEvent.class, "blocker", e -> e.cancel("blocker", "forbidden"));
        env.bus.register(TransactionPostEvent.class, "t", posts::add);
        TransferResult r = env.txs.transfer(req(a, b, 100, "k1").build());
        assertEquals(new TransferResult.Cancelled("blocker", "forbidden"), r);
        assertEquals(1000, bal(a));
        assertTrue(posts.isEmpty());
        assertTrue(env.txs.findByKey("k1").isEmpty());
    }

    @Test
    void preEventCanReduceAmount() {
        env.fund(a, "USD", 1000);
        env.bus.register(TransactionPreEvent.class, Priority.NORMAL, false, "half",
                e -> e.setAmount(Money.of("USD", e.request().amount().amount() / 2)));
        var s = (TransferResult.Success) env.txs.transfer(req(a, b, 100, "k1").build());
        assertEquals(Money.of("USD", 50), s.transaction().amount());
        assertEquals(950, bal(a));
        assertEquals(50, bal(b));
    }

    @Test
    void preEventCannotChangeCurrency() {
        env.fund(a, "USD", 1000);
        env.bus.register(TransactionPreEvent.class, "bad", e -> e.setAmount(Money.of("EUR", 1)));
        // l'eccezione è isolata dal bus: il trasferimento procede con l'importo originale
        assertInstanceOf(TransferResult.Success.class, env.txs.transfer(req(a, b, 100, "k1").build()));
        assertEquals(100, bal(b));
    }

    @Test
    void preEventReducingToZeroIsInvalid() {
        env.fund(a, "USD", 1000);
        env.bus.register(TransactionPreEvent.class, "zero", e -> e.setAmount(Money.of("USD", 0)));
        assertInstanceOf(TransferResult.InvalidRequest.class, env.txs.transfer(req(a, b, 100, "k1").build()));
        assertEquals(1000, bal(a));
    }

    @Test
    void postEventCarriesTransaction() {
        env.fund(a, "USD", 1000);
        List<TransactionPostEvent> posts = new ArrayList<>();
        env.bus.register(TransactionPostEvent.class, "t", posts::add);
        var s = (TransferResult.Success) env.txs.transfer(req(a, b, 100, "k1").build());
        assertEquals(List.of(new TransactionPostEvent(s.transaction())), posts);
    }

    @Test
    void failureMidTransactionRollsBackBalances() {
        env.fund(a, "USD", 1000);
        env.db.tx(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TRIGGER fail_insert BEFORE INSERT ON qrm_transaction "
                        + "BEGIN SELECT RAISE(ABORT, 'boom'); END");
            }
            return null;
        });
        assertThrows(QrmStorageException.class, () -> env.txs.transfer(req(a, b, 100, "k1").build()));
        assertEquals(1000, bal(a));
        assertEquals(0, bal(b));
    }

    @Test
    void concurrentTransfersNeverOverdraw() throws Exception {
        env.fund(a, "USD", 100);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        AtomicInteger successes = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            String key = "c-" + i;
            futures.add(pool.submit(() -> {
                if (env.txs.transfer(req(a, b, 1, key).build()) instanceof TransferResult.Success) {
                    successes.incrementAndGet();
                }
            }));
        }
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();
        assertEquals(100, successes.get());
        assertEquals(0, bal(a));
        assertEquals(100, bal(b));
    }

    @Test
    void historyNewestFirstAndLimited() {
        env.fund(a, "USD", 1000);
        env.clock.advance(Duration.ofSeconds(1));
        env.txs.transfer(req(a, b, 1, "h1").build());
        env.clock.advance(Duration.ofSeconds(1));
        env.txs.transfer(req(a, b, 2, "h2").build());
        List<Transaction> history = env.txs.history(a.id(), 2);
        assertEquals(2, history.size());
        assertEquals("h2", history.get(0).idempotencyKey());
        assertEquals("h1", history.get(1).idempotencyKey());
    }
}
