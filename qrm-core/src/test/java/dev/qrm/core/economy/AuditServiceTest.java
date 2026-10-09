package dev.qrm.core.economy;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.Money;
import dev.qrm.api.economy.Account;
import dev.qrm.api.economy.AuditIssue;
import dev.qrm.api.economy.TransferRequest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuditServiceTest {
    private EconomyEnv env;
    private DefaultAuditService audit;

    @BeforeEach
    void setUp() {
        env = new EconomyEnv();
        audit = new DefaultAuditService(env.db);
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    @Test
    void consistentLedgerHasNoIssues() {
        Account a = env.newAccount();
        Account b = env.newAccount();
        env.fund(a, "USD", 1000);
        env.txs.transfer(TransferRequest.builder().from(a.id()).to(b.id()).amount(Money.of("USD", 250))
                .reason("t").idempotencyKey("k").build());
        assertEquals(List.of(), audit.verify());
    }

    @Test
    void tamperedBalanceIsReported() {
        Account a = env.newAccount();
        env.fund(a, "USD", 1000);
        env.db.tx(c -> {
            try (var ps = c.prepareStatement("UPDATE qrm_account_balance SET amount=amount+5 WHERE account_id=?")) {
                ps.setString(1, a.id().value().toString());
                ps.executeUpdate();
            }
            return null;
        });
        List<AuditIssue> issues = audit.verify();
        assertEquals(List.of(new AuditIssue(a.id(), "USD", 1000, 1005)), issues);
    }
}
