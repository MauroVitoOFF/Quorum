package dev.qrm.core.job;

import dev.qrm.api.character.CharacterStatus;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.*;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultJobService implements JobService {
    private static final System.Logger LOG = System.getLogger("qrm.jobs");

    private record Row(CharacterId id, String jobId, String gradeId, boolean onDuty, Instant since) {}

    private final Database db;
    private final EventBus bus;
    private final JobRegistry registry;
    private final Clock clock;
    private final Set<CharacterId> warned = ConcurrentHashMap.newKeySet();

    public DefaultJobService(Database db, EventBus bus, JobRegistry registry, Clock clock) {
        this.db = db;
        this.bus = bus;
        this.registry = registry;
        this.clock = clock;
    }

    @Override
    public Optional<Assignment> assignmentOf(CharacterId id) {
        return db.read(c -> loadRow(c, id)).map(this::toAssignment);
    }

    @Override
    public AssignResult assign(CharacterId id, String jobId, String gradeId) {
        Optional<CharacterStatus> status = db.read(c -> CharacterQueries.statusOf(c, id));
        if (status.isEmpty()) return new AssignResult.UnknownCharacter();
        if (status.get() != CharacterStatus.ACTIVE) return new AssignResult.NotUsable(status.get());
        Optional<JobDefinition> job = registry.find(jobId);
        if (job.isEmpty()) return new AssignResult.UnknownJob(jobId);
        if (job.get().grade(gradeId).isEmpty()) return new AssignResult.UnknownGrade(gradeId);

        Optional<Assignment> before = assignmentOf(id);
        if (before.isPresent() && before.get().jobId().equals(jobId) && before.get().gradeId().equals(gradeId)) {
            return new AssignResult.Assigned(before.get());
        }
        JobChangePreEvent pre = bus.post(new JobChangePreEvent(id, before.orElse(null), jobId, gradeId));
        if (pre.isCancelled()) return new AssignResult.Cancelled(pre.cancelledBy(), pre.cancelReason());

        boolean[] dutyReset = {false};
        db.tx(c -> {
            Optional<Row> existing = loadRow(c, id);
            long now = clock.millis();
            if (existing.isEmpty()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO qrm_character_job (character_id, job_id, grade_id, on_duty, since) VALUES (?,?,?,0,?)")) {
                    ps.setString(1, id.value().toString());
                    ps.setString(2, jobId);
                    ps.setString(3, gradeId);
                    ps.setLong(4, now);
                    ps.executeUpdate();
                }
            } else {
                Row r = existing.get();
                boolean sameJob = r.jobId().equals(jobId);
                dutyReset[0] = r.onDuty() && !sameJob;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE qrm_character_job SET job_id=?, grade_id=?, on_duty=?, since=? WHERE character_id=?")) {
                    ps.setString(1, jobId);
                    ps.setString(2, gradeId);
                    ps.setInt(3, sameJob && r.onDuty() ? 1 : 0);
                    ps.setLong(4, sameJob ? r.since().toEpochMilli() : now);
                    ps.setString(5, id.value().toString());
                    ps.executeUpdate();
                }
            }
            return null;
        });

        Assignment after = assignmentOf(id).orElseThrow();
        bus.post(new JobChangedEvent(id, before.orElse(null), after));
        if (dutyReset[0]) bus.post(new DutyChangedEvent(id, false, DutyReason.JOB_CHANGE));
        return new AssignResult.Assigned(after);
    }

    @Override
    public AssignResult setGrade(CharacterId id, String gradeId) {
        Optional<Assignment> current = assignmentOf(id);
        if (current.isEmpty()) return new AssignResult.NoAssignment();
        return assign(id, current.get().jobId(), gradeId);
    }

    @Override
    public boolean dismiss(CharacterId id) {
        Optional<Assignment> before = assignmentOf(id);
        if (before.isEmpty()) return false;
        JobChangePreEvent pre = bus.post(new JobChangePreEvent(id, before.get(), null, null));
        if (pre.isCancelled()) return false;
        boolean removed = db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM qrm_character_job WHERE character_id=?")) {
                ps.setString(1, id.value().toString());
                return ps.executeUpdate() > 0;
            }
        });
        if (!removed) return false;
        bus.post(new JobChangedEvent(id, before.get(), null));
        if (before.get().onDuty()) bus.post(new DutyChangedEvent(id, false, DutyReason.JOB_CHANGE));
        return true;
    }

    @Override
    public boolean setOnDuty(CharacterId id, boolean onDuty, DutyReason reason) {
        Optional<Assignment> current = assignmentOf(id);
        if (current.isEmpty() || current.get().onDuty() == onDuty) return false;
        if (onDuty && !current.get().known()) return false;
        boolean changed = db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE qrm_character_job SET on_duty=? WHERE character_id=? AND on_duty<>?")) {
                ps.setInt(1, onDuty ? 1 : 0);
                ps.setString(2, id.value().toString());
                ps.setInt(3, onDuty ? 1 : 0);
                return ps.executeUpdate() > 0;
            }
        });
        if (changed) bus.post(new DutyChangedEvent(id, onDuty, reason));
        return changed;
    }

    @Override
    public List<CharacterId> membersOf(String jobId) {
        return db.read(c -> {
            List<CharacterId> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT j.character_id FROM qrm_character_job j JOIN qrm_character ch ON ch.id=j.character_id "
                            + "WHERE j.job_id=? AND ch.status='ACTIVE' ORDER BY j.since, j.character_id")) {
                ps.setString(1, jobId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(new CharacterId(UUID.fromString(rs.getString(1))));
                }
            }
            return out;
        });
    }

    /**
     * Uso interno del cablaggio di QrmCore: mette fuori servizio tutti gli altri personaggi del giocatore.
     *
     * @return quanti personaggi sono stati messi fuori servizio
     */
    public int setOthersOffDuty(PlayerId player, CharacterId keep, DutyReason reason) {
        List<CharacterId> changed = db.tx(c -> {
            List<CharacterId> ids = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT j.character_id FROM qrm_character_job j JOIN qrm_character ch ON ch.id=j.character_id "
                            + "WHERE ch.player_id=? AND j.character_id<>? AND j.on_duty=1")) {
                ps.setString(1, player.value().toString());
                ps.setString(2, keep.value().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) ids.add(new CharacterId(UUID.fromString(rs.getString(1))));
                }
            }
            for (CharacterId id : ids) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE qrm_character_job SET on_duty=0 WHERE character_id=?")) {
                    ps.setString(1, id.value().toString());
                    ps.executeUpdate();
                }
            }
            return ids;
        });
        changed.forEach(id -> bus.post(new DutyChangedEvent(id, false, reason)));
        return changed.size();
    }

    private Assignment toAssignment(Row r) {
        boolean known = registry.find(r.jobId()).flatMap(j -> j.grade(r.gradeId())).isPresent();
        if (!known && warned.add(r.id())) {
            LOG.log(System.Logger.Level.WARNING, "Character {0} has job {1}/{2} which is not defined: treated as unemployed",
                    r.id().value(), r.jobId(), r.gradeId());
        }
        return new Assignment(r.id(), r.jobId(), r.gradeId(), r.onDuty(), r.since(), known);
    }

    private static Optional<Row> loadRow(Connection c, CharacterId id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT character_id, job_id, grade_id, on_duty, since FROM qrm_character_job WHERE character_id=?")) {
            ps.setString(1, id.value().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Row(new CharacterId(UUID.fromString(rs.getString(1))), rs.getString(2),
                        rs.getString(3), rs.getInt(4) != 0, Instant.ofEpochMilli(rs.getLong(5))));
            }
        }
    }
}
