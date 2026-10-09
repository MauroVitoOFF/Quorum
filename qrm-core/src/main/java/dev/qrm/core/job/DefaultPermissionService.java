package dev.qrm.core.job;

import dev.qrm.api.character.CharacterStatus;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.job.Assignment;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.permission.PermissionChangedEvent;
import dev.qrm.api.permission.PermissionEffect;
import dev.qrm.api.permission.PermissionNodes;
import dev.qrm.api.permission.PermissionService;
import dev.qrm.core.db.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public final class DefaultPermissionService implements PermissionService {
    private record State(boolean active, boolean jobApplies, Set<String> jobPatterns,
            Map<String, PermissionEffect> overrides) {}

    private final Database db;
    private final EventBus bus;
    private final JobRegistry registry;
    private final JobService jobs;

    public DefaultPermissionService(Database db, EventBus bus, JobRegistry registry, JobService jobs) {
        this.db = db;
        this.bus = bus;
        this.registry = registry;
        this.jobs = jobs;
    }

    @Override
    public boolean has(CharacterId id, String node) {
        if (!PermissionNodes.isExactNode(node)) {
            throw new IllegalArgumentException("not an exact permission node: " + node);
        }
        State s = state(id);
        return s.active() && PermissionMatcher.has(node, s.jobApplies(), s.jobPatterns(), s.overrides());
    }

    @Override
    public Set<String> effective(CharacterId id) {
        State s = state(id);
        if (!s.active()) return Set.of();
        return PermissionMatcher.effective(s.jobApplies(), s.jobPatterns(), s.overrides());
    }

    @Override
    public void grant(CharacterId id, String node) {
        set(id, node, PermissionEffect.GRANT);
    }

    @Override
    public void deny(CharacterId id, String node) {
        set(id, node, PermissionEffect.DENY);
    }

    @Override
    public void clear(CharacterId id, String node) {
        PermissionNodes.requirePattern(node);
        boolean removed = db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM qrm_permission_override WHERE character_id=? AND node=?")) {
                ps.setString(1, id.value().toString());
                ps.setString(2, node);
                return ps.executeUpdate() > 0;
            }
        });
        if (removed) bus.post(new PermissionChangedEvent(id, node, PermissionEffect.CLEARED));
    }

    @Override
    public Map<String, PermissionEffect> overridesOf(CharacterId id) {
        return db.read(c -> {
            Map<String, PermissionEffect> out = new TreeMap<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT node, effect FROM qrm_permission_override WHERE character_id=?")) {
                ps.setString(1, id.value().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.put(rs.getString(1), PermissionEffect.valueOf(rs.getString(2)));
                }
            }
            return out;
        });
    }

    private void set(CharacterId id, String node, PermissionEffect effect) {
        PermissionNodes.requirePattern(node);
        boolean changed = db.tx(c -> {
            if (CharacterQueries.statusOf(c, id).isEmpty()) return false;
            String existing = null;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT effect FROM qrm_permission_override WHERE character_id=? AND node=?")) {
                ps.setString(1, id.value().toString());
                ps.setString(2, node);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) existing = rs.getString(1);
                }
            }
            if (effect.name().equals(existing)) return false;
            String sql = existing == null
                    ? "INSERT INTO qrm_permission_override (effect, character_id, node) VALUES (?,?,?)"
                    : "UPDATE qrm_permission_override SET effect=? WHERE character_id=? AND node=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, effect.name());
                ps.setString(2, id.value().toString());
                ps.setString(3, node);
                ps.executeUpdate();
            }
            return true;
        });
        if (changed) bus.post(new PermissionChangedEvent(id, node, effect));
    }

    private State state(CharacterId id) {
        Optional<CharacterStatus> status = db.read(c -> CharacterQueries.statusOf(c, id));
        if (status.isEmpty() || status.get() != CharacterStatus.ACTIVE) {
            return new State(false, false, Set.of(), Map.of());
        }
        Map<String, PermissionEffect> overrides = overridesOf(id);
        Optional<Assignment> assignment = jobs.assignmentOf(id);
        boolean applies = assignment.isPresent() && assignment.get().known() && assignment.get().onDuty();
        return new State(true, applies, applies ? patternsOf(assignment.get()) : Set.of(), overrides);
    }

    /** Unione dei permessi di tutti i grade del job con level &lt;= a quello dell'assegnazione. */
    private Set<String> patternsOf(Assignment a) {
        Optional<JobDefinition> job = registry.find(a.jobId());
        Optional<GradeDefinition> grade = job.flatMap(j -> j.grade(a.gradeId()));
        if (grade.isEmpty()) return Set.of();
        Set<String> out = new HashSet<>();
        for (GradeDefinition g : job.get().grades()) {
            if (g.level() <= grade.get().level()) out.addAll(g.permissions());
        }
        return out;
    }
}
