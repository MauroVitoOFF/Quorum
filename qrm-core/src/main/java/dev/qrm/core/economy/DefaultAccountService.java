package dev.qrm.core.economy;

import dev.qrm.api.Money;
import dev.qrm.api.economy.*;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.id.AccountId;
import dev.qrm.core.db.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DefaultAccountService implements AccountService {
    private final Database db;
    private final EventBus bus;
    private final Clock clock;

    public DefaultAccountService(Database db, EventBus bus, Clock clock) {
        this.db = db;
        this.bus = bus;
        this.clock = clock;
    }

    @Override
    public Account create(OwnerRef owner, boolean overdraftAllowed) {
        if (overdraftAllowed && !OwnerRef.SYSTEM.equals(owner.type())) {
            throw new IllegalArgumentException("overdraft is reserved for SYSTEM accounts in v0.1");
        }
        Account account = new Account(AccountId.random(), owner, AccountStatus.ACTIVE, overdraftAllowed, clock.instant());
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO qrm_account (" + AccountRepository.COLUMNS + ") VALUES (?,?,?,?,?,?)")) {
                ps.setString(1, account.id().value().toString());
                ps.setString(2, owner.type());
                ps.setString(3, owner.id().toString());
                ps.setString(4, account.status().name());
                ps.setInt(5, overdraftAllowed ? 1 : 0);
                ps.setLong(6, account.createdAt().toEpochMilli());
                ps.executeUpdate();
            }
            return null;
        });
        bus.post(new AccountCreatedEvent(account));
        return account;
    }

    @Override
    public Optional<Account> find(AccountId id) {
        return db.read(c -> AccountRepository.find(c, id));
    }

    @Override
    public List<Account> accountsOf(OwnerRef owner) {
        return db.read(c -> {
            List<Account> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT " + AccountRepository.COLUMNS
                    + " FROM qrm_account WHERE owner_type=? AND owner_id=? ORDER BY created_at, rowid")) {
                ps.setString(1, owner.type());
                ps.setString(2, owner.id().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(AccountRepository.map(rs));
                }
            }
            return out;
        });
    }

    @Override
    public synchronized Account systemAccount() {
        List<Account> existing = accountsOf(OwnerRef.system());
        return existing.isEmpty() ? create(OwnerRef.system(), true) : existing.get(0);
    }

    @Override
    public Money balance(AccountId id, String currency) {
        return Money.of(currency, Math.max(0L, db.read(c -> AccountRepository.balance(c, id, currency))));
    }

    @Override
    public List<Money> balances(AccountId id) {
        return db.read(c -> {
            List<Money> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT currency, amount FROM qrm_account_balance WHERE account_id=? AND amount>0 ORDER BY currency")) {
                ps.setString(1, id.value().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(Money.of(rs.getString(1), rs.getLong(2)));
                }
            }
            return out;
        });
    }

    @Override
    public boolean setStatus(AccountId id, AccountStatus status) {
        return db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE qrm_account SET status=? WHERE id=?")) {
                ps.setString(1, status.name());
                ps.setString(2, id.value().toString());
                return ps.executeUpdate() > 0;
            }
        });
    }
}
