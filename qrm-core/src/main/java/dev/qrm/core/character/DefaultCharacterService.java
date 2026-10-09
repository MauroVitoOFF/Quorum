package dev.qrm.core.character;

import dev.qrm.api.character.*;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.core.CoreConfig;
import dev.qrm.core.db.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class DefaultCharacterService implements CharacterService {
    private static final String COLUMNS = "id, player_id, first_name, last_name, birth_date, status, created_at";

    private final Database db;
    private final EventBus bus;
    private final Clock clock;
    private final CoreConfig config;
    private final Pattern namePattern;

    public DefaultCharacterService(Database db, EventBus bus, Clock clock, CoreConfig config) {
        this.db = db;
        this.bus = bus;
        this.clock = clock;
        this.config = config;
        this.namePattern = Pattern.compile(config.namePattern());
    }

    @Override
    public CreateCharacterResult create(PlayerId player, String firstName, String lastName, LocalDate birthDate) {
        String first = firstName == null ? "" : firstName.strip();
        String last = lastName == null ? "" : lastName.strip();
        String error = nameError(first);
        if (error == null) error = nameError(last);
        if (error != null) return new CreateCharacterResult.InvalidName(error);
        String nameKey = (first + " " + last).toLowerCase(Locale.ROOT);

        CreateCharacterResult result = db.tx(c -> {
            Integer slots = slotsOf(c, player);
            if (slots == null) return new CreateCharacterResult.UnknownPlayer();
            if (count(c, "SELECT COUNT(*) FROM qrm_character WHERE player_id=? AND status<>'ARCHIVED'",
                    player.value().toString()) >= slots) {
                return new CreateCharacterResult.SlotsFull();
            }
            if (config.uniqueNames()
                    && count(c, "SELECT COUNT(*) FROM qrm_character WHERE name_key=? AND status<>'ARCHIVED'", nameKey) > 0) {
                return new CreateCharacterResult.NameTaken();
            }
            PlayerCharacter pc = new PlayerCharacter(
                    CharacterId.random(), player, first, last, birthDate, CharacterStatus.ACTIVE, clock.instant());
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO qrm_character (id, player_id, first_name, last_name, name_key, birth_date, status, created_at)"
                            + " VALUES (?,?,?,?,?,?,?,?)")) {
                ps.setString(1, pc.id().value().toString());
                ps.setString(2, player.value().toString());
                ps.setString(3, first);
                ps.setString(4, last);
                ps.setString(5, nameKey);
                ps.setString(6, birthDate.toString());
                ps.setString(7, CharacterStatus.ACTIVE.name());
                ps.setLong(8, pc.createdAt().toEpochMilli());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE qrm_player SET active_character=? WHERE id=? AND active_character IS NULL")) {
                ps.setString(1, pc.id().value().toString());
                ps.setString(2, player.value().toString());
                ps.executeUpdate();
            }
            return new CreateCharacterResult.Created(pc);
        });
        if (result instanceof CreateCharacterResult.Created created) {
            bus.post(new CharacterCreatedEvent(created.character()));
        }
        return result;
    }

    @Override
    public SelectCharacterResult select(PlayerId player, CharacterId character) {
        SelectCharacterResult result = db.tx(c -> {
            Optional<PlayerCharacter> found = load(c, character);
            if (found.isEmpty()) return new SelectCharacterResult.NotFound();
            PlayerCharacter pc = found.get();
            if (!pc.playerId().equals(player)) return new SelectCharacterResult.NotOwner();
            if (pc.status() != CharacterStatus.ACTIVE) return new SelectCharacterResult.NotUsable(pc.status());
            try (PreparedStatement ps = c.prepareStatement("UPDATE qrm_player SET active_character=? WHERE id=?")) {
                ps.setString(1, character.value().toString());
                ps.setString(2, player.value().toString());
                ps.executeUpdate();
            }
            return new SelectCharacterResult.Selected(pc);
        });
        if (result instanceof SelectCharacterResult.Selected selected) {
            bus.post(new CharacterSelectedEvent(selected.character()));
        }
        return result;
    }

    @Override
    public boolean archive(PlayerId player, CharacterId character) {
        Optional<PlayerCharacter> archived = db.tx(c -> {
            Optional<PlayerCharacter> found = load(c, character);
            if (found.isEmpty()) return Optional.<PlayerCharacter>empty();
            PlayerCharacter pc = found.get();
            if (!pc.playerId().equals(player) || pc.status() == CharacterStatus.ARCHIVED) {
                return Optional.<PlayerCharacter>empty();
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE qrm_character SET status='ARCHIVED' WHERE id=?")) {
                ps.setString(1, character.value().toString());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE qrm_player SET active_character=NULL WHERE id=? AND active_character=?")) {
                ps.setString(1, player.value().toString());
                ps.setString(2, character.value().toString());
                ps.executeUpdate();
            }
            return Optional.of(new PlayerCharacter(pc.id(), pc.playerId(), pc.firstName(), pc.lastName(),
                    pc.birthDate(), CharacterStatus.ARCHIVED, pc.createdAt()));
        });
        archived.ifPresent(pc -> bus.post(new CharacterArchivedEvent(pc)));
        return archived.isPresent();
    }

    @Override
    public Optional<PlayerCharacter> activeOf(PlayerId player) {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNS + " FROM qrm_character WHERE id=(SELECT active_character FROM qrm_player WHERE id=?)")) {
                ps.setString(1, player.value().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.<PlayerCharacter>empty();
                }
            }
        });
    }

    @Override
    public List<PlayerCharacter> listOf(PlayerId player) {
        return db.read(c -> {
            List<PlayerCharacter> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNS + " FROM qrm_character WHERE player_id=? ORDER BY created_at, id")) {
                ps.setString(1, player.value().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(map(rs));
                }
            }
            return out;
        });
    }

    @Override
    public Optional<PlayerCharacter> find(CharacterId id) {
        return db.read(c -> load(c, id));
    }

    @Override
    public List<PlayerCharacter> findByName(String fullName) {
        String key = fullName == null ? "" : fullName.strip().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) return List.of();
        return db.read(c -> {
            List<PlayerCharacter> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS
                    + " FROM qrm_character WHERE name_key=? AND status<>'ARCHIVED' ORDER BY created_at, id")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(map(rs));
                }
            }
            return out;
        });
    }

    private String nameError(String name) {
        if (name.length() < config.nameMinLength() || name.length() > config.nameMaxLength()) {
            return "name must be " + config.nameMinLength() + ".." + config.nameMaxLength() + " characters";
        }
        if (!namePattern.matcher(name).matches()) return "name contains invalid characters";
        return null;
    }

    private static Integer slotsOf(Connection c, PlayerId player) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT character_slots FROM qrm_player WHERE id=?")) {
            ps.setString(1, player.value().toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private static int count(Connection c, String sql, String arg) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, arg);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static Optional<PlayerCharacter> load(Connection c, CharacterId id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM qrm_character WHERE id=?")) {
            ps.setString(1, id.value().toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private static PlayerCharacter map(ResultSet rs) throws SQLException {
        return new PlayerCharacter(
                new CharacterId(UUID.fromString(rs.getString(1))),
                new PlayerId(UUID.fromString(rs.getString(2))),
                rs.getString(3),
                rs.getString(4),
                LocalDate.parse(rs.getString(5)),
                CharacterStatus.valueOf(rs.getString(6)),
                Instant.ofEpochMilli(rs.getLong(7)));
    }
}
