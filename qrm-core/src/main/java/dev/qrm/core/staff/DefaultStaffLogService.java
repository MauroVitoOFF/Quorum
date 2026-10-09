package dev.qrm.core.staff;

import dev.qrm.api.staff.StaffLogEntry;
import dev.qrm.api.staff.StaffLogService;
import dev.qrm.core.db.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class DefaultStaffLogService implements StaffLogService {
    private static final int MAX_DETAIL = 256;
    private static final int MAX_NAME = 64;
    private static final int MAX_ACTION = 64;
    private static final int MAX_LIMIT = 200;

    private final Database db;
    private final Clock clock;
    private final AtomicLong sequence;

    public DefaultStaffLogService(Database db, Clock clock) {
        this.db = db;
        this.clock = clock;
        this.sequence = new AtomicLong(db.read(c -> {
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COALESCE(MAX(seq), 0) FROM qrm_staff_log")) {
                rs.next();
                return rs.getLong(1);
            }
        }));
    }

    @Override
    public void record(Optional<UUID> staffId, String staffName, String action, Optional<UUID> targetId,
            Optional<String> targetName, String detail, StaffLogEntry.Outcome outcome) {
        long seq = sequence.incrementAndGet();
        long at = clock.millis();
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO qrm_staff_log "
                    + "(seq, at, staff_id, staff_name, action, target_id, target_name, detail, outcome) VALUES (?,?,?,?,?,?,?,?,?)")) {
                ps.setLong(1, seq);
                ps.setLong(2, at);
                ps.setString(3, staffId.map(UUID::toString).orElse(null));
                ps.setString(4, cap(staffName, MAX_NAME));
                ps.setString(5, cap(action, MAX_ACTION));
                ps.setString(6, targetId.map(UUID::toString).orElse(null));
                ps.setString(7, targetName.map(n -> cap(n, MAX_NAME)).orElse(null));
                ps.setString(8, cap(detail, MAX_DETAIL));
                ps.setString(9, outcome.name());
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public List<StaffLogEntry> recent(int limit) {
        return query("SELECT * FROM qrm_staff_log ORDER BY seq DESC LIMIT ?", null, limit);
    }

    @Override
    public List<StaffLogEntry> about(UUID player, int limit) {
        return query("SELECT * FROM qrm_staff_log WHERE staff_id=? OR target_id=? ORDER BY seq DESC LIMIT ?",
                player.toString(), limit);
    }

    @Override
    public int pruneOlderThan(Instant cutoff) {
        return db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM qrm_staff_log WHERE at < ?")) {
                ps.setLong(1, cutoff.toEpochMilli());
                return ps.executeUpdate();
            }
        });
    }

    private List<StaffLogEntry> query(String sql, String player, int limit) {
        int n = Math.max(1, Math.min(MAX_LIMIT, limit));
        return db.read(c -> {
            List<StaffLogEntry> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                int i = 1;
                if (player != null) {
                    ps.setString(i++, player);
                    ps.setString(i++, player);
                }
                ps.setInt(i, n);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(read(rs));
                }
            }
            return out;
        });
    }

    private static StaffLogEntry read(ResultSet rs) throws java.sql.SQLException {
        return new StaffLogEntry(rs.getLong("seq"), Instant.ofEpochMilli(rs.getLong("at")),
                Optional.ofNullable(rs.getString("staff_id")).map(UUID::fromString), rs.getString("staff_name"),
                rs.getString("action"), Optional.ofNullable(rs.getString("target_id")).map(UUID::fromString),
                Optional.ofNullable(rs.getString("target_name")), rs.getString("detail"),
                StaffLogEntry.Outcome.valueOf(rs.getString("outcome")));
    }

    private static String cap(String s, int max) {
        String v = s == null ? "" : s;
        return v.length() <= max ? v : v.substring(0, max);
    }
}
