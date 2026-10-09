package dev.qrm.core.economy;

import dev.qrm.api.economy.Account;
import dev.qrm.api.economy.AccountStatus;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.id.AccountId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Accesso SQL condiviso da servizi conti e transazioni. Tutti i metodi usano la connessione della transazione corrente. */
final class AccountRepository {
    static final String COLUMNS = "id, owner_type, owner_id, status, overdraft_allowed, created_at";

    private AccountRepository() {}

    static Optional<Account> find(Connection c, AccountId id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM qrm_account WHERE id=?")) {
            ps.setString(1, id.value().toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    static long balance(Connection c, AccountId id, String currency) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT amount FROM qrm_account_balance WHERE account_id=? AND currency=?")) {
            ps.setString(1, id.value().toString());
            ps.setString(2, currency);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    /** Aggiunge {@code delta} (anche negativo) al saldo, creando la riga se manca. */
    static void add(Connection c, AccountId id, String currency, long delta) throws SQLException {
        int updated;
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE qrm_account_balance SET amount=amount+? WHERE account_id=? AND currency=?")) {
            ps.setLong(1, delta);
            ps.setString(2, id.value().toString());
            ps.setString(3, currency);
            updated = ps.executeUpdate();
        }
        if (updated == 0) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO qrm_account_balance (account_id, currency, amount) VALUES (?,?,?)")) {
                ps.setString(1, id.value().toString());
                ps.setString(2, currency);
                ps.setLong(3, delta);
                ps.executeUpdate();
            }
        }
    }

    static Account map(ResultSet rs) throws SQLException {
        return new Account(
                new AccountId(UUID.fromString(rs.getString(1))),
                new OwnerRef(rs.getString(2), UUID.fromString(rs.getString(3))),
                AccountStatus.valueOf(rs.getString(4)),
                rs.getInt(5) != 0,
                Instant.ofEpochMilli(rs.getLong(6)));
    }
}
