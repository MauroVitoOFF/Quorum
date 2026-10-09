package dev.qrm.core.player;

import dev.qrm.api.id.PlayerId;
import dev.qrm.api.player.PlayerService;
import dev.qrm.api.player.QrmPlayer;
import dev.qrm.core.CoreConfig;
import dev.qrm.core.db.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class DefaultPlayerService implements PlayerService {
    private final Database db;
    private final Clock clock;
    private final CoreConfig config;

    public DefaultPlayerService(Database db, Clock clock, CoreConfig config) {
        this.db = db;
        this.clock = clock;
        this.config = config;
    }

    @Override
    public QrmPlayer onJoin(PlayerId id) {
        long now = clock.millis();
        return db.tx(c -> {
            int updated;
            try (PreparedStatement ps = c.prepareStatement("UPDATE qrm_player SET last_seen=? WHERE id=?")) {
                ps.setLong(1, now);
                ps.setString(2, id.value().toString());
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO qrm_player (id, first_seen, last_seen, character_slots) VALUES (?,?,?,?)")) {
                    ps.setString(1, id.value().toString());
                    ps.setLong(2, now);
                    ps.setLong(3, now);
                    ps.setInt(4, config.defaultCharacterSlots());
                    ps.executeUpdate();
                }
            }
            return load(c, id).orElseThrow();
        });
    }

    @Override
    public Optional<QrmPlayer> find(PlayerId id) {
        return db.read(c -> load(c, id));
    }

    private static Optional<QrmPlayer> load(Connection c, PlayerId id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, first_seen, last_seen, character_slots FROM qrm_player WHERE id=?")) {
            ps.setString(1, id.value().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new QrmPlayer(
                        new PlayerId(UUID.fromString(rs.getString(1))),
                        Instant.ofEpochMilli(rs.getLong(2)),
                        Instant.ofEpochMilli(rs.getLong(3)),
                        rs.getInt(4)));
            }
        }
    }
}
