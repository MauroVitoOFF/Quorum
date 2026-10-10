package dev.qrm.core.point;

import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.org.Rank;
import dev.qrm.api.point.*;
import dev.qrm.core.org.OrgsEnv;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Fixture dei punti: parte da {@link OrgsEnv} (job "police" collegato a "pd") e aggiunge registro, servizio e tipi. */
public final class PointsEnv implements AutoCloseable {
    public final OrgsEnv env = new OrgsEnv();
    public final InMemoryPointTypeRegistry types = new InMemoryPointTypeRegistry();
    public final DefaultPointService points;
    public final List<Object> events = new ArrayList<>();
    public final List<PointUse> uses = new ArrayList<>();

    public PointsEnv() {
        points = new DefaultPointService(env.db, env.bus, types, env.registry, env.jobs, env.orgs, env.clock);
        types.register(new PointType("duty", "qrm.point.duty", Set.of(PointOwner.Kind.JOB), false, uses::add));
        types.register(new PointType("locker", "qrm.point.locker",
                Set.of(PointOwner.Kind.JOB, PointOwner.Kind.ORGANIZATION), true, uses::add));
        env.registry.register(new JobDefinition("baker", "Fornaio", List.of(
                new GradeDefinition("apprentice", "Apprendista", 1, Set.of(), null),
                new GradeDefinition("master", "Mastro", 4, Set.of(), null))));
        env.bus.register(PointCreatedEvent.class, "t", events::add);
        env.bus.register(PointChangedEvent.class, "t", events::add);
        env.bus.register(PointRemovedEvent.class, "t", events::add);
    }

    public static PointPosition pos(int x) {
        return new PointPosition("minecraft:overworld", x, 64, 0);
    }

    public InteractionPoint create(String type, PointOwner owner, int x, String minGrade) {
        PointResult<InteractionPoint> r = points.create(type, owner, pos(x), minGrade, null);
        if (r instanceof PointResult.Ok<InteractionPoint> ok) return ok.value();
        throw new AssertionError("expected Ok but was " + r);
    }

    public static PointError err(PointResult<?> r) {
        if (r instanceof PointResult.Failed<?> f) return f.error();
        throw new AssertionError("expected Failed but was " + r);
    }

    /** Organizzazione "gang" con rank soldier(1) e boss(5). */
    public void gang() {
        OrgsEnv.ok(env.orgs.create("gang", "Gang"));
        OrgsEnv.ok(env.orgs.defineRank("gang", new Rank("soldier", "Soldato", 1, Set.of())));
        OrgsEnv.ok(env.orgs.defineRank("gang", new Rank("boss", "Boss", 5, Set.of())));
    }

    @Override
    public void close() {
        env.close();
    }
}
