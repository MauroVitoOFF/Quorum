package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.Money;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.AccountStatus;
import dev.qrm.api.economy.TransactionPreEvent;
import dev.qrm.bank.api.BankDepositEvent;
import dev.qrm.bank.api.BankWithdrawEvent;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BankOperationsTest {
    private BankEnv env;
    private PlayerCharacter anna;

    @BeforeEach
    void setUp() {
        env = new BankEnv();
        anna = env.newCharacter();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private BankResult withdraw(long amount) {
        return env.ops.withdraw(anna.id(), Money.of(BankEnv.USD, amount), env.inventory);
    }

    @Test
    void accountIsOpenedOnceAndReused() {
        assertEquals(env.ops.accountOf(anna.id()), env.ops.accountOf(anna.id()));
    }

    @Test
    void withdrawMovesMoneyToVaultAndGivesNotes() {
        env.fund(anna.id(), 10_000);
        assertEquals(BankResult.OK, withdraw(1700));
        assertEquals(8_300, env.balance(anna.id()));
        assertEquals(1_700, env.vaultBalance());
        assertEquals(Map.of(1000L, 1, 500L, 1, 100L, 2), env.inventory.notes.get(BankEnv.USD));
        assertEquals(1, env.events.stream().filter(BankWithdrawEvent.class::isInstance).count());
    }

    @Test
    void withdrawWithInsufficientFundsGivesNothing() {
        env.fund(anna.id(), 500);
        assertEquals(BankResult.INSUFFICIENT_FUNDS, withdraw(1000));
        assertEquals(500, env.balance(anna.id()));
        assertTrue(env.inventory.notes.isEmpty());
    }

    @Test
    void withdrawRejectsNotRepresentableAndInvalidAmounts() {
        env.fund(anna.id(), 10_000);
        assertEquals(BankResult.NOT_REPRESENTABLE, withdraw(150));
        assertEquals(BankResult.INVALID_AMOUNT, withdraw(0));
        assertEquals(10_000, env.balance(anna.id()));
    }

    @Test
    void withdrawRejectsUnknownCashCurrency() {
        assertEquals(BankResult.UNKNOWN_CURRENCY, env.ops.withdraw(anna.id(), Money.of("EUR", 100), env.inventory));
    }

    @Test
    void withdrawChecksSpaceBeforeDebiting() {
        env.fund(anna.id(), 10_000);
        env.inventory.full = true;
        assertEquals(BankResult.INVENTORY_FULL, withdraw(1000));
        assertEquals(10_000, env.balance(anna.id()));
        assertEquals(0, env.vaultBalance());
    }

    @Test
    void withdrawEnforcesPerOperationLimit() {
        env.fund(anna.id(), 100_000);
        assertEquals(BankResult.LIMIT_EXCEEDED, withdraw(6_000));
        assertEquals(100_000, env.balance(anna.id()));
    }

    @Test
    void withdrawEnforcesDailyLimitWithRollingWindow() {
        env.fund(anna.id(), 100_000);
        assertEquals(BankResult.OK, withdraw(5_000));
        assertEquals(BankResult.LIMIT_EXCEEDED, withdraw(4_000));
        env.clock.advance(Duration.ofHours(25));
        assertEquals(BankResult.OK, withdraw(4_000));
    }

    @Test
    void dailyLimitSurvivesMoreMovementsThanTheHistoryScanWindow() {
        env.newCharacterNamed("Bruno", "Verdi");
        env.fund(anna.id(), 100_000);
        assertEquals(BankResult.OK, withdraw(5_000));
        assertEquals(BankResult.OK, withdraw(3_000));
        // Centinaia di bonifici spingono i ritiri fuori dalle transazioni recenti lette: il limite deve valere lo stesso.
        for (int i = 0; i < BankOperations.HISTORY_SCAN + 10; i++) {
            env.clock.advance(Duration.ofSeconds(1));
            assertEquals(BankResult.OK, env.ops.transfer(anna.id(), "Bruno Verdi", Money.of(BankEnv.USD, 1)));
        }
        assertEquals(BankResult.LIMIT_EXCEEDED, withdraw(100));
    }

    @Test
    void withdrawFromFrozenAccountIsRejected() {
        env.fund(anna.id(), 10_000);
        env.accounts.setStatus(env.ops.accountOf(anna.id()), AccountStatus.FROZEN);
        assertEquals(BankResult.FROZEN, withdraw(1000));
        assertTrue(env.inventory.notes.isEmpty());
    }

    @Test
    void withdrawCancelledByAnotherModGivesNothing() {
        env.fund(anna.id(), 10_000);
        env.bus.register(TransactionPreEvent.class, "t", e -> e.cancel("t", "no"));
        assertEquals(BankResult.CANCELLED, withdraw(1000));
        assertEquals(10_000, env.balance(anna.id()));
        assertTrue(env.inventory.notes.isEmpty());
    }

    @Test
    void withdrawWhoseAmountWasChangedByAnotherModIsReverted() {
        env.fund(anna.id(), 10_000);
        env.bus.register(TransactionPreEvent.class, "t", e -> {
            if (BankOperations.REASON_WITHDRAW.equals(e.request().reason())) {
                e.setAmount(Money.of(BankEnv.USD, e.request().amount().amount() / 2));
            }
        });
        assertEquals(BankResult.CANCELLED, withdraw(1000));
        assertEquals(10_000, env.balance(anna.id()));
        assertEquals(0, env.vaultBalance());
        assertTrue(env.inventory.notes.isEmpty());
    }

    @Test
    void depositRoundTripRestoresBalanceAndEmptiesVault() {
        env.fund(anna.id(), 10_000);
        assertEquals(BankResult.OK, withdraw(1700));
        assertEquals(BankResult.OK, env.ops.deposit(anna.id(), BankEnv.USD, env.inventory));
        assertEquals(10_000, env.balance(anna.id()));
        assertEquals(0, env.vaultBalance());
        assertEquals(0, env.inventory.count(BankEnv.USD));
        assertEquals(1, env.events.stream().filter(BankDepositEvent.class::isInstance).count());
        assertTrue(env.audit.verify().isEmpty());
    }

    @Test
    void depositWithoutCashIsRejected() {
        assertEquals(BankResult.NO_CASH, env.ops.deposit(anna.id(), BankEnv.USD, env.inventory));
    }

    @Test
    void depositOfCashNotBackedByTheVaultIsRejectedAndLeftUntouched() {
        env.inventory.notes.put(BankEnv.USD, new java.util.HashMap<>(Map.of(100L, 5)));
        assertEquals(BankResult.UNBACKED_CASH, env.ops.deposit(anna.id(), BankEnv.USD, env.inventory));
        assertEquals(500, env.inventory.count(BankEnv.USD));
        assertEquals(0, env.balance(anna.id()));
    }

    @Test
    void depositIsRevertedWhenRemovingTheCashFails() {
        env.fund(anna.id(), 10_000);
        assertEquals(BankResult.OK, withdraw(1000));
        env.inventory.failRemove = true;
        assertEquals(BankResult.INTERNAL_ERROR, env.ops.deposit(anna.id(), BankEnv.USD, env.inventory));
        assertEquals(9_000, env.balance(anna.id()));
        assertEquals(1_000, env.vaultBalance());
        assertEquals(1_000, env.inventory.count(BankEnv.USD));
    }

    @Test
    void depositIsRevertedWhenAnotherModChangesTheAmount() {
        env.fund(anna.id(), 10_000);
        assertEquals(BankResult.OK, withdraw(1000));
        env.bus.register(TransactionPreEvent.class, "t", e -> {
            if (BankOperations.REASON_DEPOSIT.equals(e.request().reason())) {
                e.setAmount(Money.of(BankEnv.USD, 100));
            }
        });
        assertEquals(BankResult.CANCELLED, env.ops.deposit(anna.id(), BankEnv.USD, env.inventory));
        assertEquals(9_000, env.balance(anna.id()));
        assertEquals(1_000, env.vaultBalance());
        assertEquals(1_000, env.inventory.count(BankEnv.USD));
    }

    @Test
    void partialDepositCreditsOnlyThatAmountAndReturnsTheChange() {
        env.fund(anna.id(), 10_000);
        assertEquals(BankResult.OK, withdraw(1700));
        assertEquals(BankResult.OK, env.ops.deposit(anna.id(), BankEnv.USD, 500, env.inventory));
        assertEquals(10_000 - 1700 + 500, env.balance(anna.id()));
        assertEquals(1700 - 500, env.vaultBalance());
        assertEquals(1200, env.inventory.count(BankEnv.USD));
        assertTrue(env.audit.verify().isEmpty());
    }

    @Test
    void depositOfMoreThanHeldOrNonPositiveIsRejected() {
        env.fund(anna.id(), 10_000);
        assertEquals(BankResult.OK, withdraw(1000));
        assertEquals(BankResult.NO_CASH, env.ops.deposit(anna.id(), BankEnv.USD, 1001, env.inventory));
        assertEquals(BankResult.INVALID_AMOUNT, env.ops.deposit(anna.id(), BankEnv.USD, 0, env.inventory));
        assertEquals(1000, env.inventory.count(BankEnv.USD));
        assertEquals(9_000, env.balance(anna.id()));
    }
}
