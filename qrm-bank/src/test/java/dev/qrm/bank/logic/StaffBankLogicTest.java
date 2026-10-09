package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.character.PlayerCharacter;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaffBankLogicTest {
    private BankEnv env;
    private StaffBankLogic logic;
    private PlayerCharacter anna;

    @BeforeEach
    void setUp() {
        env = new BankEnv();
        logic = new StaffBankLogic(env.ops, env.accounts, env.txs, env.audit, env.currencies);
        anna = env.newCharacter();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    @Test
    void emptyAccountIsActiveWithNoBalances() {
        StaffBankLogic.State s = logic.state(anna.id());
        assertFalse(s.frozen());
        assertEquals(List.of(), s.balances());
    }

    @Test
    void stateListsFormattedBalances() {
        env.fund(anna.id(), 1250);
        StaffBankLogic.State s = logic.state(anna.id());
        assertEquals(1, s.balances().size());
        assertEquals("USD", s.balances().get(0).currency());
        assertEquals("12.50 $", s.balances().get(0).text());
    }

    @Test
    void freezeAndUnfreeze() {
        logic.setFrozen(anna.id(), true);
        assertTrue(logic.state(anna.id()).frozen());
        // un conto congelato non riceve denaro
        assertThrows(IllegalStateException.class, () -> env.fund(anna.id(), 100));
        logic.setFrozen(anna.id(), false);
        assertFalse(logic.state(anna.id()).frozen());
        env.fund(anna.id(), 100);
        assertEquals(100, env.balance(anna.id()));
    }

    @Test
    void auditIsCleanWhenCashMatchesTheVault() {
        StaffBankLogic.AuditResult r = logic.audit(List.of("USD"), c -> 0);
        assertTrue(r.clean());
        assertEquals(0, r.issues());
    }

    @Test
    void auditReportsCashAboveTheVault() {
        StaffBankLogic.AuditResult r = logic.audit(List.of("USD"), c -> 500);
        assertFalse(r.clean());
        assertEquals(0, r.issues());
        assertEquals(1, r.excess().size());
        assertEquals("USD", r.excess().get(0).currency());
        assertEquals("5.00 $", r.excess().get(0).held());
        assertEquals("0.00 $", r.excess().get(0).vault());
    }

    @Test
    void cashEqualToTheVaultIsNotAnExcess() {
        assertTrue(logic.audit(List.of("USD"), c -> 0).excess().isEmpty());
    }

    // ---- denaro e storico ----------------------------------------------------------------------------------

    @Test
    void grantAndTakeMoveMoneyWithTheCommandReasons() {
        assertEquals(StaffBankLogic.AdjustResult.OK, logic.adjust(anna.id(), true, "12.50", "USD"));
        assertEquals(1250, env.balance(anna.id()));
        assertEquals(StaffBankLogic.AdjustResult.OK, logic.adjust(anna.id(), false, "2.50", "usd"));
        assertEquals(1000, env.balance(anna.id()));
        var history = env.txs.history(env.ops.accountOf(anna.id()), 10);
        assertEquals(java.util.Set.of("admin.grant", "admin.take"),
                history.stream().map(t -> t.reason()).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void takeCannotGoNegative() {
        logic.adjust(anna.id(), true, "1", "USD");
        assertEquals(StaffBankLogic.AdjustResult.INSUFFICIENT_FUNDS, logic.adjust(anna.id(), false, "5", "USD"));
        assertEquals(100, env.balance(anna.id()));
    }

    @Test
    void adjustRejectsBadInput() {
        assertEquals(StaffBankLogic.AdjustResult.UNKNOWN_CURRENCY, logic.adjust(anna.id(), true, "1", "XXX"));
        for (String bad : new String[] {"abc", "-3", "0", "1.234", ""}) {
            assertEquals(StaffBankLogic.AdjustResult.INVALID, logic.adjust(anna.id(), true, bad, "USD"), bad);
        }
        assertEquals(0, env.balance(anna.id()));
    }

    @Test
    void frozenAccountCannotBeGrantedOrTaken() {
        logic.setFrozen(anna.id(), true);
        assertEquals(StaffBankLogic.AdjustResult.FROZEN, logic.adjust(anna.id(), true, "1", "USD"));
    }

    @Test
    void historyShowsSignedFormattedMoves() {
        logic.adjust(anna.id(), true, "3", "USD");
        logic.adjust(anna.id(), false, "1", "USD");
        StaffBankLogic.State s = logic.state(anna.id());
        assertEquals(java.util.Set.of("+3.00 $", "-1.00 $"),
                s.history().stream().map(StaffBankLogic.Move::amount).collect(java.util.stream.Collectors.toSet()));
        assertEquals(java.util.Set.of("admin.grant", "admin.take"),
                s.history().stream().map(StaffBankLogic.Move::reason).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void historyIsLimited() {
        for (int i = 0; i < StaffBankLogic.HISTORY_LIMIT + 5; i++) logic.adjust(anna.id(), true, "1", "USD");
        assertEquals(StaffBankLogic.HISTORY_LIMIT, logic.state(anna.id()).history().size());
    }

    @Test
    void currencyCodesAreSorted() {
        assertEquals(List.of("USD"), logic.currencyCodes());
    }
}
