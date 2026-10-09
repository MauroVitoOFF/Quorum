package dev.qrm.core.economy;

import dev.qrm.api.Money;
import dev.qrm.api.economy.*;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.id.AccountId;
import dev.qrm.core.QrmStorageException;
import dev.qrm.core.db.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DefaultTransactionService implements TransactionService {
    private static final String COLUMNS =
            "id, group_id, from_account, to_account, amount, currency, reason, created_at, metadata, idempotency_key";

    private static final System.Logger LOG = System.getLogger("qrm.economy");

    private final Database db;
    private final EventBus bus;
    private final Clock clock;
    private final CurrencyRegistry currencies;

    public DefaultTransactionService(Database db, EventBus bus, Clock clock, CurrencyRegistry currencies) {
        this.db = db;
        this.bus = bus;
        this.clock = clock;
        this.currencies = currencies;
    }

    @Override
    public TransferResult transfer(TransferRequest request) {
        Optional<Transaction> duplicate = findByKey(request.idempotencyKey());
        if (duplicate.isPresent()) return new TransferResult.Duplicate(duplicate.get());
        if (request.amount().amount() <= 0) return new TransferResult.InvalidRequest("amount must be positive");
        if (request.from().equals(request.to())) return new TransferResult.InvalidRequest("from and to must differ");
        if (currencies.find(request.amount().currency()).isEmpty()) {
            return new TransferResult.UnknownCurrency(request.amount().currency());
        }

        TransactionPreEvent pre = bus.post(new TransactionPreEvent(request));
        if (pre.isCancelled()) return new TransferResult.Cancelled(pre.cancelledBy(), pre.cancelReason());
        TransferRequest finalRequest = pre.request();
        if (finalRequest.amount().amount() <= 0) {
            return new TransferResult.InvalidRequest("amount must be positive after pre-event handlers");
        }

        TransferResult result;
        try {
            result = db.tx(c -> apply(c, finalRequest));
        } catch (QrmStorageException e) {
            LOG.log(System.Logger.Level.ERROR,
                    "Storage failure during transfer (idempotencyKey=" + finalRequest.idempotencyKey() + ")", e);
            throw e;
        }
        if (result instanceof TransferResult.Success success) {
            bus.post(new TransactionPostEvent(success.transaction()));
        }
        return result;
    }

    private TransferResult apply(Connection c, TransferRequest r) throws SQLException {
        Optional<Transaction> duplicate = findByKey(c, r.idempotencyKey());
        if (duplicate.isPresent()) return new TransferResult.Duplicate(duplicate.get());

        Optional<Account> from = AccountRepository.find(c, r.from());
        if (from.isEmpty()) return new TransferResult.AccountNotFound(r.from());
        Optional<Account> to = AccountRepository.find(c, r.to());
        if (to.isEmpty()) return new TransferResult.AccountNotFound(r.to());
        if (from.get().status() == AccountStatus.FROZEN) return new TransferResult.AccountFrozen(r.from());
        if (to.get().status() == AccountStatus.FROZEN) return new TransferResult.AccountFrozen(r.to());

        String currency = r.amount().currency();
        long amount = r.amount().amount();
        long available = AccountRepository.balance(c, r.from(), currency);
        if (available < amount && !from.get().overdraftAllowed()) {
            return new TransferResult.InsufficientFunds(r.amount(), Money.of(currency, Math.max(available, 0)));
        }
        long toBalance = AccountRepository.balance(c, r.to(), currency);
        try {
            // SQLite trasformerebbe in silenzio l'overflow int64 in REAL: si calcola in Java.
            Math.subtractExact(available, amount);
            Math.addExact(toBalance, amount);
        } catch (ArithmeticException e) {
            return new TransferResult.InvalidRequest("amount overflow");
        }

        AccountRepository.add(c, r.from(), currency, -amount);
        AccountRepository.add(c, r.to(), currency, amount);

        UUID id = UUID.randomUUID();
        Transaction tx = new Transaction(id, r.groupId() != null ? r.groupId() : id, r.from(), r.to(), r.amount(),
                r.reason(), clock.instant(), r.metadataJson(), r.idempotencyKey());
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO qrm_transaction (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, id.toString());
            ps.setString(2, tx.groupId().toString());
            ps.setString(3, r.from().value().toString());
            ps.setString(4, r.to().value().toString());
            ps.setLong(5, amount);
            ps.setString(6, currency);
            ps.setString(7, r.reason());
            ps.setLong(8, tx.createdAt().toEpochMilli());
            ps.setString(9, r.metadataJson());
            ps.setString(10, r.idempotencyKey());
            ps.executeUpdate();
        }
        return new TransferResult.Success(tx);
    }

    @Override
    public Optional<Transaction> findByKey(String idempotencyKey) {
        return db.read(c -> findByKey(c, idempotencyKey));
    }

    @Override
    public List<Transaction> history(AccountId account, int limit) {
        return db.read(c -> {
            List<Transaction> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS
                    + " FROM qrm_transaction WHERE from_account=? OR to_account=? ORDER BY created_at DESC, id LIMIT ?")) {
                ps.setString(1, account.value().toString());
                ps.setString(2, account.value().toString());
                ps.setInt(3, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(map(rs));
                }
            }
            return out;
        });
    }

    private static Optional<Transaction> findByKey(Connection c, String key) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM qrm_transaction WHERE idempotency_key=?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private static Transaction map(ResultSet rs) throws SQLException {
        return new Transaction(
                UUID.fromString(rs.getString(1)),
                UUID.fromString(rs.getString(2)),
                new AccountId(UUID.fromString(rs.getString(3))),
                new AccountId(UUID.fromString(rs.getString(4))),
                Money.of(rs.getString(6), rs.getLong(5)),
                rs.getString(7),
                Instant.ofEpochMilli(rs.getLong(8)),
                rs.getString(9),
                rs.getString(10));
    }
}
