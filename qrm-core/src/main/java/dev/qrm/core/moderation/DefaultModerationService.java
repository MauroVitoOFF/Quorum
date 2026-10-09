package dev.qrm.core.moderation;

import dev.qrm.api.event.EventBus;
import dev.qrm.api.moderation.ModerationService;
import dev.qrm.api.moderation.MuteChangedEvent;
import dev.qrm.api.moderation.Mute;
import dev.qrm.api.moderation.StaffNote;
import dev.qrm.api.moderation.Warning;
import dev.qrm.core.db.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class DefaultModerationService implements ModerationService {
    private static final int MAX_NAME = 64;
    private static final int MAX_REASON = 128;
    private static final int MAX_NOTE = 200;
    private static final int MAX_LIMIT = 200;

    private final Database db;
    private final Clock clock;
    private final EventBus bus;
    private final AtomicLong warningSeq;
    private final AtomicLong noteSeq;

    public DefaultModerationService(Database db, EventBus bus, Clock clock) {
        this.db = db;
        this.bus = bus;
        this.clock = clock;
        this.warningSeq = new AtomicLong(maxSeq("qrm_warning"));
        this.noteSeq = new AtomicLong(maxSeq("qrm_staff_note"));
    }

    private long maxSeq(String table) {
        return db.read(c -> {
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COALESCE(MAX(seq), 0) FROM " + table)) {
                rs.next();
                return rs.getLong(1);
            }
        });
    }

    // ---- mute ----------------------------------------------------------------------------------------------

    @Override
    public void mute(UUID player, String playerName, Optional<UUID> staffId, String staffName, String reason,
            Optional<Duration> duration) {
        long now = clock.millis();
        Long expires = duration.map(d -> now + d.toMillis()).orElse(null);
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM qrm_mute WHERE player_id=?")) {
                ps.setString(1, player.toString());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO qrm_mute "
                    + "(player_id, player_name, staff_id, staff_name, reason, at, expires_at) VALUES (?,?,?,?,?,?,?)")) {
                ps.setString(1, player.toString());
                ps.setString(2, cap(playerName, MAX_NAME));
                ps.setString(3, staffId.map(UUID::toString).orElse(null));
                ps.setString(4, cap(staffName, MAX_NAME));
                ps.setString(5, cap(reason, MAX_REASON));
                ps.setLong(6, now);
                if (expires == null) ps.setNull(7, java.sql.Types.BIGINT);
                else ps.setLong(7, expires);
                ps.executeUpdate();
            }
            return null;
        });
        bus.post(new MuteChangedEvent(player));
    }

    @Override
    public boolean unmute(UUID player) {
        Optional<Mute> active = activeMute(player);
        if (active.isEmpty()) return false;
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM qrm_mute WHERE player_id=?")) {
                ps.setString(1, player.toString());
                return ps.executeUpdate();
            }
        });
        bus.post(new MuteChangedEvent(player));
        return true;
    }

    @Override
    public Optional<Mute> activeMute(UUID player) {
        Optional<Mute> found = db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM qrm_mute WHERE player_id=?")) {
                ps.setString(1, player.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(readMute(rs)) : Optional.<Mute>empty();
                }
            }
        });
        if (found.isPresent() && !found.get().activeAt(clock.instant())) {
            db.tx(c -> {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM qrm_mute WHERE player_id=?")) {
                    ps.setString(1, player.toString());
                    return ps.executeUpdate();
                }
            });
            return Optional.empty();
        }
        return found;
    }

    @Override
    public List<Mute> activeMutes(int limit) {
        long now = clock.millis();
        return db.read(c -> {
            List<Mute> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM qrm_mute WHERE expires_at IS NULL OR expires_at > ? ORDER BY at DESC LIMIT ?")) {
                ps.setLong(1, now);
                ps.setInt(2, clamp(limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(readMute(rs));
                }
            }
            return out;
        });
    }

    // ---- avvertimenti --------------------------------------------------------------------------------------

    @Override
    public int warn(UUID player, String playerName, Optional<UUID> staffId, String staffName, String reason) {
        long seq = warningSeq.incrementAndGet();
        long now = clock.millis();
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO qrm_warning "
                    + "(seq, player_id, player_name, staff_id, staff_name, reason, at) VALUES (?,?,?,?,?,?,?)")) {
                ps.setLong(1, seq);
                ps.setString(2, player.toString());
                ps.setString(3, cap(playerName, MAX_NAME));
                ps.setString(4, staffId.map(UUID::toString).orElse(null));
                ps.setString(5, cap(staffName, MAX_NAME));
                ps.setString(6, cap(reason, MAX_REASON));
                ps.setLong(7, now);
                ps.executeUpdate();
            }
            return null;
        });
        return warningCount(player);
    }

    @Override
    public List<Warning> warningsOf(UUID player, int limit) {
        return queryWarnings("SELECT * FROM qrm_warning WHERE player_id=? ORDER BY seq DESC LIMIT ?", player, limit);
    }

    @Override
    public int warningCount(UUID player) {
        return count("qrm_warning", player);
    }

    @Override
    public List<Warning> recentWarnings(int limit) {
        return queryWarnings("SELECT * FROM qrm_warning ORDER BY seq DESC LIMIT ?", null, limit);
    }

    // ---- note ----------------------------------------------------------------------------------------------

    @Override
    public void addNote(UUID player, String playerName, Optional<UUID> staffId, String staffName, String text) {
        long seq = noteSeq.incrementAndGet();
        long now = clock.millis();
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO qrm_staff_note "
                    + "(seq, player_id, player_name, staff_id, staff_name, note, at) VALUES (?,?,?,?,?,?,?)")) {
                ps.setLong(1, seq);
                ps.setString(2, player.toString());
                ps.setString(3, cap(playerName, MAX_NAME));
                ps.setString(4, staffId.map(UUID::toString).orElse(null));
                ps.setString(5, cap(staffName, MAX_NAME));
                ps.setString(6, cap(text, MAX_NOTE));
                ps.setLong(7, now);
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public List<StaffNote> notesOf(UUID player, int limit) {
        return db.read(c -> {
            List<StaffNote> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM qrm_staff_note WHERE player_id=? ORDER BY seq DESC LIMIT ?")) {
                ps.setString(1, player.toString());
                ps.setInt(2, clamp(limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(readNote(rs));
                }
            }
            return out;
        });
    }

    @Override
    public int noteCount(UUID player) {
        return count("qrm_staff_note", player);
    }

    // ---- interni -------------------------------------------------------------------------------------------

    private int count(String table, UUID player) {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM " + table + " WHERE player_id=?")) {
                ps.setString(1, player.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }

    private List<Warning> queryWarnings(String sql, UUID player, int limit) {
        return db.read(c -> {
            List<Warning> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                int i = 1;
                if (player != null) ps.setString(i++, player.toString());
                ps.setInt(i, clamp(limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(readWarning(rs));
                }
            }
            return out;
        });
    }

    private static Mute readMute(ResultSet rs) throws SQLException {
        long exp = rs.getLong("expires_at");
        Optional<Instant> expires = rs.wasNull() ? Optional.empty() : Optional.of(Instant.ofEpochMilli(exp));
        return new Mute(UUID.fromString(rs.getString("player_id")), rs.getString("player_name"),
                Optional.ofNullable(rs.getString("staff_id")).map(UUID::fromString), rs.getString("staff_name"),
                rs.getString("reason"), Instant.ofEpochMilli(rs.getLong("at")), expires);
    }

    private static Warning readWarning(ResultSet rs) throws SQLException {
        return new Warning(rs.getLong("seq"), UUID.fromString(rs.getString("player_id")), rs.getString("player_name"),
                Optional.ofNullable(rs.getString("staff_id")).map(UUID::fromString), rs.getString("staff_name"),
                rs.getString("reason"), Instant.ofEpochMilli(rs.getLong("at")));
    }

    private static StaffNote readNote(ResultSet rs) throws SQLException {
        return new StaffNote(rs.getLong("seq"), UUID.fromString(rs.getString("player_id")), rs.getString("player_name"),
                Optional.ofNullable(rs.getString("staff_id")).map(UUID::fromString), rs.getString("staff_name"),
                rs.getString("note"), Instant.ofEpochMilli(rs.getLong("at")));
    }

    private static int clamp(int limit) {
        return Math.max(1, Math.min(MAX_LIMIT, limit));
    }

    private static String cap(String s, int max) {
        String v = s == null ? "" : s;
        return v.length() <= max ? v : v.substring(0, max);
    }
}
