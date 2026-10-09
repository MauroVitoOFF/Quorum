package dev.qrm.core.economy;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.Money;
import dev.qrm.api.economy.Account;
import dev.qrm.api.economy.TransferRequest;
import dev.qrm.api.economy.TransferResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionOverflowTest {
    private EconomyEnv env;
    private Account a;

    @BeforeEach
    void setUp() {
        env = new EconomyEnv();
        a = env.newAccount();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private TransferResult mint(Account to, long amount, String key) {
        return env.txs.transfer(TransferRequest.builder().from(env.accounts.systemAccount().id()).to(to.id())
                .amount(Money.of("USD", amount)).reason("test.mint").idempotencyKey(key).build());
    }

    @Test
    void creditBeyondLongMaxIsRejectedAndBalancesUnchanged() {
        assertInstanceOf(TransferResult.Success.class, mint(a, Long.MAX_VALUE - 5, "k1"));
        var r = assertInstanceOf(TransferResult.InvalidRequest.class, mint(a, 10, "k2"));
        assertEquals("amount overflow", r.reason());
        assertEquals(Long.MAX_VALUE - 5, env.accounts.balance(a.id(), "USD").amount());
        assertEquals(1, env.txs.history(a.id(), 10).size());
    }

    @Test
    void systemDebitBelowLongMinIsRejectedAndBalancesUnchanged() {
        Account b = env.newAccount();
        assertInstanceOf(TransferResult.Success.class, mint(a, Long.MAX_VALUE, "k1"));
        // system = -MAX; debiting 2 more would go below Long.MIN_VALUE
        var r = assertInstanceOf(TransferResult.InvalidRequest.class, mint(b, 2, "k2"));
        assertEquals("amount overflow", r.reason());
        assertEquals(0, env.accounts.balance(b.id(), "USD").amount());
        assertEquals(Long.MAX_VALUE, env.accounts.balance(a.id(), "USD").amount());
        assertEquals(java.util.List.of(), env.txs.history(b.id(), 10));
        // exactly Long.MIN_VALUE is still representable
        assertInstanceOf(TransferResult.Success.class, mint(b, 1, "k3"));
    }
}
