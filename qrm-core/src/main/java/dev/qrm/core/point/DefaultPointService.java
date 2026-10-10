package dev.qrm.core.point;

import dev.qrm.api.character.CharacterStatus;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.job.Assignment;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.org.OrganizationService;
import dev.qrm.api.org.OrgStatus;
import dev.qrm.api.point.*;
import dev.qrm.core.db.Database;
import dev.qrm.core.job.CharacterQueries;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class DefaultPointService implements PointService {
    private static final String COLUMNS =
            "id, type_id, owner_kind, owner_id, dimension, x, y, z, min_grade_id, public_visible, name, face, created_at";

    private final Database db;
    private final EventBus bus;
    private final PointTypeRegistry types;
    private final JobRegistry jobRegistry;
    private final JobService jobs;
    private final OrganizationService orgs;
    private final Clock clock;

    public DefaultPointService(Database db, EventBus bus, PointTypeRegistry types, JobRegistry jobRegistry,
            JobService jobs, OrganizationService orgs, Clock clock) {
        this.db = db;
        this.bus = bus;
        this.types = types;
        this.jobRegistry = jobRegistry;
        this.jobs = jobs;
        this.orgs = orgs;
        this.clock = clock;
    }

    // ---- scrittura -----------------------------------------------------------------------------------------

    @Override
    public PointResult<InteractionPoint> create(String typeId, PointOwner owner, PointPosition position,
            String minGradeId, String name, PointFace face) {
        PointType type = types.find(typeId).orElse(null);
        if (type == null) return PointResult.fail(new PointError.UnknownType(typeId));
        if (!type.owners().contains(owner.kind())) {
            return PointResult.fail(new PointError.OwnerNotAllowed(typeId, owner.kind()));
        }
        if (!ownerExists(owner)) return PointResult.fail(new PointError.UnknownOwner(owner));
        if (minGradeId != null && levelOf(owner, minGradeId).isEmpty()) {
            return PointResult.fail(new PointError.UnknownGrade(minGradeId));
        }
        Instant now = Instant.ofEpochMilli(clock.millis());
        PointResult<InteractionPoint> result = db.tx(c -> {
            Optional<InteractionPoint> taken = loadAt(c, position);
            if (taken.isPresent()) return PointResult.fail(new PointError.PositionTaken(taken.get().id()));
            int id;
            try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(MAX(id), 0) + 1 FROM qrm_interaction_point");
                    ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getInt(1);
            }
            InteractionPoint point = new InteractionPoint(id, typeId, owner, position, minGradeId, false, name, face, now);
            insert(c, point);
            return PointResult.ok(point);
        });
        if (result instanceof PointResult.Ok<InteractionPoint> ok) bus.post(new PointCreatedEvent(ok.value()));
        return result;
    }

    @Override
    public PointResult<InteractionPoint> setMinGrade(int id, String gradeId) {
        Optional<InteractionPoint> point = find(id);
        if (point.isEmpty()) return PointResult.fail(new PointError.NotFound(id));
        if (gradeId != null && levelOf(point.get().owner(), gradeId).isEmpty()) {
            return PointResult.fail(new PointError.UnknownGrade(gradeId));
        }
        InteractionPoint p = point.get();
        return update(id, new InteractionPoint(id, p.typeId(), p.owner(), p.position(), gradeId, p.publicVisible(),
                p.name(), p.face(), p.createdAt()));
    }

    @Override
    public PointResult<InteractionPoint> setPublicVisible(int id, boolean publicVisible) {
        Optional<InteractionPoint> point = find(id);
        if (point.isEmpty()) return PointResult.fail(new PointError.NotFound(id));
        InteractionPoint p = point.get();
        return update(id, new InteractionPoint(id, p.typeId(), p.owner(), p.position(), p.minGradeId(), publicVisible,
                p.name(), p.face(), p.createdAt()));
    }

    @Override
    public PointResult<InteractionPoint> setName(int id, String name) {
        Optional<InteractionPoint> point = find(id);
        if (point.isEmpty()) return PointResult.fail(new PointError.NotFound(id));
        InteractionPoint p = point.get();
        return update(id, new InteractionPoint(id, p.typeId(), p.owner(), p.position(), p.minGradeId(),
                p.publicVisible(), name, p.face(), p.createdAt()));
    }

    @Override
    public PointResult<InteractionPoint> setFace(int id, PointFace face) {
        Optional<InteractionPoint> point = find(id);
        if (point.isEmpty()) return PointResult.fail(new PointError.NotFound(id));
        InteractionPoint p = point.get();
        return update(id, new InteractionPoint(id, p.typeId(), p.owner(), p.position(), p.minGradeId(),
                p.publicVisible(), p.name(), face, p.createdAt()));
    }

    @Override
    public PointResult<InteractionPoint> move(int id, PointPosition position) {
        Optional<InteractionPoint> point = find(id);
        if (point.isEmpty()) return PointResult.fail(new PointError.NotFound(id));
        InteractionPoint p = point.get();
        if (p.position().equals(position)) return PointResult.ok(p);
        return update(id, new InteractionPoint(id, p.typeId(), p.owner(), position, p.minGradeId(), p.publicVisible(),
                p.name(), p.face(), p.createdAt()));
    }

    @Override
    public boolean remove(int id) {
        Optional<InteractionPoint> point = find(id);
        if (point.isEmpty()) return false;
        boolean removed = db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM qrm_interaction_point WHERE id=?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        });
        if (removed) bus.post(new PointRemovedEvent(point.get()));
        return removed;
    }

    /** Scrive {@code after} al posto del punto esistente; la posizione va ricontrollata nella stessa transazione. */
    private PointResult<InteractionPoint> update(int id, InteractionPoint after) {
        InteractionPoint[] before = new InteractionPoint[1];
        PointResult<InteractionPoint> result = db.tx(c -> {
            Optional<InteractionPoint> current = loadById(c, id);
            if (current.isEmpty()) return PointResult.fail(new PointError.NotFound(id));
            Optional<InteractionPoint> taken = loadAt(c, after.position());
            if (taken.isPresent() && taken.get().id() != id) {
                return PointResult.fail(new PointError.PositionTaken(taken.get().id()));
            }
            before[0] = current.get();
            try (PreparedStatement ps = c.prepareStatement("UPDATE qrm_interaction_point SET dimension=?, x=?, y=?, z=?, "
                    + "min_grade_id=?, public_visible=?, name=?, face=? WHERE id=?")) {
                ps.setString(1, after.position().dimension());
                ps.setInt(2, after.position().x());
                ps.setInt(3, after.position().y());
                ps.setInt(4, after.position().z());
                setNullable(ps, 5, after.minGradeId());
                ps.setInt(6, after.publicVisible() ? 1 : 0);
                setNullable(ps, 7, after.name());
                setNullable(ps, 8, after.face() == null ? null : after.face().name());
                ps.setInt(9, id);
                ps.executeUpdate();
            }
            return PointResult.ok(after);
        });
        if (result instanceof PointResult.Ok<InteractionPoint> && !before[0].equals(after)) {
            bus.post(new PointChangedEvent(before[0], after));
        }
        return result;
    }

    // ---- lettura -------------------------------------------------------------------------------------------

    @Override
    public Optional<InteractionPoint> find(int id) {
        return db.read(c -> loadById(c, id));
    }

    @Override
    public Optional<InteractionPoint> findAt(PointPosition position) {
        return db.read(c -> loadAt(c, position));
    }

    @Override
    public List<InteractionPoint> all() {
        return db.read(c -> list(c, "SELECT " + COLUMNS + " FROM qrm_interaction_point ORDER BY id", null));
    }

    @Override
    public List<InteractionPoint> ownedBy(PointOwner owner) {
        return db.read(c -> list(c, "SELECT " + COLUMNS
                + " FROM qrm_interaction_point WHERE owner_kind=? AND owner_id=? ORDER BY id", owner));
    }

    // ---- regole --------------------------------------------------------------------------------------------

    @Override
    public Optional<InactiveReason> inactiveReason(InteractionPoint point) {
        if (types.find(point.typeId()).isEmpty()) return Optional.of(InactiveReason.UNKNOWN_TYPE);
        if (!ownerExists(point.owner())) return Optional.of(InactiveReason.UNKNOWN_OWNER);
        if (point.minGradeId() != null && levelOf(point.owner(), point.minGradeId()).isEmpty()) {
            return Optional.of(InactiveReason.UNKNOWN_GRADE);
        }
        return Optional.empty();
    }

    @Override
    public boolean canUse(CharacterId character, InteractionPoint point) {
        boolean active = db.read(c -> CharacterQueries.statusOf(c, character)).filter(s -> s == CharacterStatus.ACTIVE)
                .isPresent();
        if (!active) return false;
        PointType type = types.find(point.typeId()).orElse(null);
        if (type == null || inactiveReason(point).isPresent()) return false;
        OptionalInt required = point.minGradeId() == null ? OptionalInt.empty()
                : levelOf(point.owner(), point.minGradeId());
        return switch (point.owner().kind()) {
            case JOB -> {
                Assignment a = jobs.assignmentOf(character).orElse(null);
                if (a == null || !a.known() || !a.jobId().equals(point.owner().id())) yield false;
                if (type.requiresDuty() && !a.onDuty()) yield false;
                OptionalInt own = levelOf(point.owner(), a.gradeId());
                yield own.isPresent() && (required.isEmpty() || own.getAsInt() >= required.getAsInt());
            }
            case ORGANIZATION -> {
                String orgId = point.owner().id();
                // solo membri espliciti: i membri impliciti (job in servizio collegato) non contano
                String rankId = orgs.membersOf(orgId).stream()
                        .filter(m -> m.character().equals(character))
                        .map(m -> m.rankId())
                        .findFirst().orElse(null);
                if (rankId == null) yield false;
                OptionalInt own = levelOf(point.owner(), rankId);
                yield own.isPresent() && (required.isEmpty() || own.getAsInt() >= required.getAsInt());
            }
        };
    }

    private boolean ownerExists(PointOwner owner) {
        return switch (owner.kind()) {
            case JOB -> jobRegistry.find(owner.id()).isPresent();
            case ORGANIZATION -> orgs.find(owner.id()).filter(o -> o.status() == OrgStatus.ACTIVE).isPresent();
        };
    }

    /** Livello del grado/rank nel proprietario; vuoto se il proprietario o il grado non esistono. */
    private OptionalInt levelOf(PointOwner owner, String gradeId) {
        return switch (owner.kind()) {
            case JOB -> jobRegistry.find(owner.id()).flatMap(j -> j.grade(gradeId))
                    .map(g -> OptionalInt.of(g.level())).orElse(OptionalInt.empty());
            case ORGANIZATION -> orgs.ranksOf(owner.id()).stream().filter(r -> r.id().equals(gradeId)).findFirst()
                    .map(r -> OptionalInt.of(r.level())).orElse(OptionalInt.empty());
        };
    }

    // ---- SQL -----------------------------------------------------------------------------------------------

    private static void insert(Connection c, InteractionPoint p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO qrm_interaction_point (" + COLUMNS
                + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, p.id());
            ps.setString(2, p.typeId());
            ps.setString(3, p.owner().kind().name());
            ps.setString(4, p.owner().id());
            ps.setString(5, p.position().dimension());
            ps.setInt(6, p.position().x());
            ps.setInt(7, p.position().y());
            ps.setInt(8, p.position().z());
            setNullable(ps, 9, p.minGradeId());
            ps.setInt(10, p.publicVisible() ? 1 : 0);
            setNullable(ps, 11, p.name());
            setNullable(ps, 12, p.face() == null ? null : p.face().name());
            ps.setLong(13, p.createdAt().toEpochMilli());
            ps.executeUpdate();
        }
    }

    private static void setNullable(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) ps.setNull(index, Types.VARCHAR);
        else ps.setString(index, value);
    }

    private static Optional<InteractionPoint> loadById(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM qrm_interaction_point WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private static Optional<InteractionPoint> loadAt(Connection c, PointPosition pos) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS
                + " FROM qrm_interaction_point WHERE dimension=? AND x=? AND y=? AND z=?")) {
            ps.setString(1, pos.dimension());
            ps.setInt(2, pos.x());
            ps.setInt(3, pos.y());
            ps.setInt(4, pos.z());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private static List<InteractionPoint> list(Connection c, String sql, PointOwner owner) throws SQLException {
        List<InteractionPoint> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (owner != null) {
                ps.setString(1, owner.kind().name());
                ps.setString(2, owner.id());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return List.copyOf(out);
    }

    private static InteractionPoint map(ResultSet rs) throws SQLException {
        return new InteractionPoint(rs.getInt(1), rs.getString(2),
                new PointOwner(PointOwner.Kind.valueOf(rs.getString(3)), rs.getString(4)),
                new PointPosition(rs.getString(5), rs.getInt(6), rs.getInt(7), rs.getInt(8)),
                rs.getString(9), rs.getInt(10) != 0, rs.getString(11),
                rs.getString(12) == null ? null : PointFace.valueOf(rs.getString(12)),
                Instant.ofEpochMilli(rs.getLong(13)));
    }
}
