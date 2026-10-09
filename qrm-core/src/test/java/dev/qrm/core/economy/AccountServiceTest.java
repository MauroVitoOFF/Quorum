package dev.qrm.core.economy;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.Money;
import dev.qrm.api.economy.*;
import dev.qrm.api.id.AccountId;
import dev.qrm.api.id.CharacterId;
import dev.qrm.core.TestClock;
import dev.qrm.core.db.Database;
import dev.qrm.core.db.Migrator;
import dev.qrm.core.event.SimpleEventBus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountServiceTest {
    private Database db;
    private SimpleEventBus bus;
    private DefaultAccountService accounts;
    private final List<AccountCreatedEvent> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        db = Database.open("jdbc:sqlite::memory:");
        Migrator.migrate(db);
        bus = new SimpleEventBus();
        accounts = new DefaultAccountService(db, bus, new TestClock(Instant.parse("2026-01-01T00:00:00Z")));
        bus.register(AccountCreatedEvent.class, "t", created::add);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void registryRegistersFindsAndRejectsConflicts() {
        InMemoryCurrencyRegistry reg = new InMemoryCurrencyRegistry();
        reg.register(new Currency("USD", "$", 2));
        reg.register(new Currency("USD", "$", 2));
        assertEquals(1, reg.all().size());
        assertTrue(reg.find("USD").isPresent());
        assertTrue(reg.find("EUR").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> reg.register(new Currency("USD", "$", 3)));
    }

    @Test
    void createAndFind() {
        OwnerRef owner = OwnerRef.character(CharacterId.random());
        Account a = accounts.create(owner, false);
        assertEquals(a, accounts.find(a.id()).orElseThrow());
        assertEquals(AccountStatus.ACTIVE, a.status());
        assertFalse(a.overdraftAllowed());
        assertEquals(List.of(new AccountCreatedEvent(a)), created);
    }

    @Test
    void findMissingIsEmpty() {
        assertTrue(accounts.find(AccountId.random()).isEmpty());
    }

    @Test
    void accountsOfReturnsOnlyOwnersAccounts() {
        OwnerRef o1 = OwnerRef.character(CharacterId.random());
        OwnerRef o2 = OwnerRef.character(CharacterId.random());
        Account a1 = accounts.create(o1, false);
        Account a2 = accounts.create(o1, false);
        accounts.create(o2, false);
        assertEquals(List.of(a1, a2), accounts.accountsOf(o1));
    }

    @Test
    void systemAccountIsCreatedOnceWithOverdraft() {
        Account s1 = accounts.systemAccount();
        Account s2 = accounts.systemAccount();
        assertEquals(s1.id(), s2.id());
        assertTrue(s1.overdraftAllowed());
        assertEquals(OwnerRef.system(), s1.owner());
        assertEquals(1, created.size());
    }

    @Test
    void balanceDefaultsToZeroAndBalancesListsNonZero() {
        Account a = accounts.create(OwnerRef.character(CharacterId.random()), false);
        assertEquals(Money.of("USD", 0), accounts.balance(a.id(), "USD"));
        db.tx(c -> {
            AccountRepository.add(c, a.id(), "USD", 500);
            AccountRepository.add(c, a.id(), "EUR", 0);
            return null;
        });
        assertEquals(Money.of("USD", 500), accounts.balance(a.id(), "USD"));
        assertEquals(List.of(Money.of("USD", 500)), accounts.balances(a.id()));
    }

    @Test
    void setStatusFreezes() {
        Account a = accounts.create(OwnerRef.character(CharacterId.random()), false);
        assertTrue(accounts.setStatus(a.id(), AccountStatus.FROZEN));
        assertEquals(AccountStatus.FROZEN, accounts.find(a.id()).orElseThrow().status());
        assertFalse(accounts.setStatus(AccountId.random(), AccountStatus.FROZEN));
    }

    @Test
    void overdraftIsReservedForSystemAccounts() {
        var owner = OwnerRef.character(CharacterId.random());
        var e = assertThrows(IllegalArgumentException.class, () -> accounts.create(owner, true));
        assertEquals("overdraft is reserved for SYSTEM accounts in v0.1", e.getMessage());
        assertTrue(accounts.accountsOf(owner).isEmpty());
        assertTrue(accounts.systemAccount().overdraftAllowed());
    }

    @Test
    void negativeSystemBalanceIsClampedToZero() {
        var sys = accounts.systemAccount();
        var other = accounts.create(OwnerRef.character(CharacterId.random()), false);
        db.tx(c -> {
            AccountRepository.add(c, sys.id(), "USD", -500);
            AccountRepository.add(c, other.id(), "USD", 500);
            return null;
        });
        assertEquals(Money.of("USD", 0), accounts.balance(sys.id(), "USD"));
        assertEquals(List.of(), accounts.balances(sys.id()));
        assertEquals(Money.of("USD", 500), accounts.balance(other.id(), "USD"));
    }

    @Test
    void registryRemoveDropsOnlyThatCode() {
        InMemoryCurrencyRegistry reg = new InMemoryCurrencyRegistry();
        reg.register(new Currency("USD", "$", 2));
        reg.register(new Currency("EUR", "E", 2));
        reg.remove("USD");
        reg.remove("XXX");
        assertTrue(reg.find("USD").isEmpty());
        assertTrue(reg.find("EUR").isPresent());
    }
}
