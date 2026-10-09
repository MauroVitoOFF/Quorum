package dev.qrm.core.economy;

import dev.qrm.api.economy.AuditIssue;
import dev.qrm.api.economy.AuditService;
import dev.qrm.api.id.AccountId;
import dev.qrm.core.db.Database;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DefaultAuditService implements AuditService {
    private final Database db;

    public DefaultAuditService(Database db) {
        this.db = db;
    }

    @Override
    public List<AuditIssue> verify() {
        return db.read(c -> {
            Map<String, Long> expected = new HashMap<>();
            Map<String, Long> actual = new HashMap<>();
            try (Statement s = c.createStatement()) {
                try (ResultSet rs = s.executeQuery("SELECT to_account, currency, SUM(amount) FROM qrm_transaction GROUP BY to_account, currency")) {
                    while (rs.next()) expected.merge(rs.getString(1) + "|" + rs.getString(2), rs.getLong(3), Long::sum);
                }
                try (ResultSet rs = s.executeQuery("SELECT from_account, currency, SUM(amount) FROM qrm_transaction GROUP BY from_account, currency")) {
                    while (rs.next()) expected.merge(rs.getString(1) + "|" + rs.getString(2), -rs.getLong(3), Long::sum);
                }
                try (ResultSet rs = s.executeQuery("SELECT account_id, currency, amount FROM qrm_account_balance")) {
                    while (rs.next()) actual.put(rs.getString(1) + "|" + rs.getString(2), rs.getLong(3));
                }
            }
            List<String> keys = new ArrayList<>(expected.keySet());
            actual.keySet().stream().filter(k -> !expected.containsKey(k)).forEach(keys::add);
            keys.sort(String::compareTo);
            List<AuditIssue> issues = new ArrayList<>();
            for (String key : keys) {
                long e = expected.getOrDefault(key, 0L);
                long a = actual.getOrDefault(key, 0L);
                if (e != a) {
                    String[] parts = key.split("\\|");
                    issues.add(new AuditIssue(new AccountId(UUID.fromString(parts[0])), parts[1], e, a));
                }
            }
            return issues;
        });
    }
}
