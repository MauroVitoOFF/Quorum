package dev.qrm.core.point;

import static dev.qrm.core.point.PointsEnv.err;
import static dev.qrm.core.point.PointsEnv.pos;
import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.job.DutyReason;
import dev.qrm.api.point.*;
import dev.qrm.core.org.OrgsEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultPointServiceTest {
    private PointsEnv t;

    @BeforeEach
    void setUp() {
        t = new PointsEnv();
    }

    @AfterEach
    void tearDown() {
        t.close();
    }

    // ---- CRUD ----------------------------------------------------------------------------------------------

    @Test
    void createAssignsProgressiveIdsAndEmitsEvent() {
        InteractionPoint a = t.create("duty", PointOwner.job("police"), 1, null);
        InteractionPoint b = t.create("duty", PointOwner.job("police"), 2, "cadet");
        assertEquals(1, a.id());
        assertEquals(2, b.id());
        assertFalse(a.publicVisible());
        assertEquals("cadet", b.minGradeId());
        assertEquals(2, t.points.all().size());
        assertEquals(2, t.events.stream().filter(e -> e instanceof PointCreatedEvent).count());
        assertEquals(b, t.points.findAt(pos(2)).orElseThrow());
    }

    @Test
    void idsAreNotReusedAfterRemovingTheLast() {
        t.create("duty", PointOwner.job("police"), 1, null);
        t.create("duty", PointOwner.job("police"), 2, null);
        assertTrue(t.points.remove(2));
        assertEquals(2, t.create("duty", PointOwner.job("police"), 3, null).id());
    }

    @Test
    void createRejectsInvalidInput() {
        assertInstanceOf(PointError.UnknownType.class,
                err(t.points.create("nope", PointOwner.job("police"), pos(1), null, null)));
        assertInstanceOf(PointError.OwnerNotAllowed.class,
                err(t.points.create("duty", PointOwner.organization("gang"), pos(1), null, null)));
        assertInstanceOf(PointError.UnknownOwner.class,
                err(t.points.create("duty", PointOwner.job("ghost"), pos(1), null, null)));
        assertInstanceOf(PointError.UnknownOwner.class,
                err(t.points.create("locker", PointOwner.organization("ghost"), pos(1), null, null)));
        assertInstanceOf(PointError.UnknownGrade.class,
                err(t.points.create("duty", PointOwner.job("police"), pos(1), "general", null)));
        assertTrue(t.points.all().isEmpty());
    }

    @Test
    void duplicatePositionIsRejectedAcrossOwners() {
        InteractionPoint a = t.create("duty", PointOwner.job("police"), 1, null);
        PointError e = err(t.points.create("duty", PointOwner.job("baker"), pos(1), null, null));
        assertEquals(new PointError.PositionTaken(a.id()), e);
        // stessa x ma dimensione diversa: ammesso
        assertInstanceOf(PointResult.Ok.class,
                t.points.create("duty", PointOwner.job("baker"), new PointPosition("minecraft:the_nether", 1, 64, 0), null, null));
    }

    @Test
    void archivedOrganizationCannotOwnNewPoints() {
        t.gang();
        OrgsEnv.ok(t.env.orgs.archive("gang"));
        assertInstanceOf(PointError.UnknownOwner.class,
                err(t.points.create("locker", PointOwner.organization("gang"), pos(1), null, null)));
    }

    @Test
    void settersChangeOnlyTheirFieldAndEmitChangedEvent() {
        InteractionPoint p = t.create("duty", PointOwner.job("police"), 1, null);
        t.events.clear();
        assertEquals("chief", ((PointResult.Ok<InteractionPoint>) t.points.setMinGrade(p.id(), "chief")).value().minGradeId());
        assertTrue(((PointResult.Ok<InteractionPoint>) t.points.setPublicVisible(p.id(), true)).value().publicVisible());
        InteractionPoint named = ((PointResult.Ok<InteractionPoint>) t.points.setName(p.id(), "Centrale")).value();
        assertEquals("Centrale", named.name());
        assertEquals("chief", named.minGradeId());
        assertTrue(named.publicVisible());
        assertEquals(3, t.events.stream().filter(e -> e instanceof PointChangedEvent).count());

        t.events.clear();
        t.points.setName(p.id(), "Centrale"); // nessun cambiamento: nessun evento
        assertTrue(t.events.isEmpty());

        assertNull(((PointResult.Ok<InteractionPoint>) t.points.setMinGrade(p.id(), null)).value().minGradeId());
        assertNull(((PointResult.Ok<InteractionPoint>) t.points.setName(p.id(), null)).value().name());
        assertInstanceOf(PointError.UnknownGrade.class, err(t.points.setMinGrade(p.id(), "general")));
        assertInstanceOf(PointError.NotFound.class, err(t.points.setName(99, "x")));
    }

    @Test
    void faceDefaultsToAllAndCanBeSetAndCleared() {
        InteractionPoint p = t.create("duty", PointOwner.job("police"), 1, null);
        assertNull(p.face());
        t.events.clear();
        InteractionPoint north = ((PointResult.Ok<InteractionPoint>) t.points.setFace(p.id(), PointFace.NORTH)).value();
        assertEquals(PointFace.NORTH, north.face());
        assertEquals(PointFace.NORTH, t.points.find(p.id()).orElseThrow().face());
        assertEquals(1, t.events.stream().filter(e -> e instanceof PointChangedEvent).count());
        t.points.setName(p.id(), "x"); // gli altri setter non toccano la faccia
        assertEquals(PointFace.NORTH, t.points.find(p.id()).orElseThrow().face());
        assertNull(((PointResult.Ok<InteractionPoint>) t.points.setFace(p.id(), null)).value().face());
        assertInstanceOf(PointError.NotFound.class, err(t.points.setFace(99, PointFace.EAST)));
    }

    @Test
    void moveKeepsFace() {
        InteractionPoint p = t.create("duty", PointOwner.job("police"), 1, null);
        t.points.setFace(p.id(), PointFace.WEST);
        assertEquals(PointFace.WEST, ((PointResult.Ok<InteractionPoint>) t.points.move(p.id(), pos(9))).value().face());
    }

    @Test
    void moveKeepsIdAndDataAndRejectsOccupiedBlock() {
        InteractionPoint a = t.create("duty", PointOwner.job("police"), 1, "cadet");
        InteractionPoint b = t.create("duty", PointOwner.job("police"), 2, null);
        assertEquals(new PointError.PositionTaken(b.id()), err(t.points.move(a.id(), pos(2))));
        InteractionPoint moved = ((PointResult.Ok<InteractionPoint>) t.points.move(a.id(), pos(9))).value();
        assertEquals(a.id(), moved.id());
        assertEquals(pos(9), moved.position());
        assertEquals("cadet", moved.minGradeId());
        assertTrue(t.points.findAt(pos(1)).isEmpty());
        assertInstanceOf(PointError.NotFound.class, err(t.points.move(99, pos(5))));
    }

    @Test
    void removeAndOwnerListing() {
        InteractionPoint a = t.create("duty", PointOwner.job("police"), 1, null);
        t.create("duty", PointOwner.job("baker"), 2, null);
        assertEquals(1, t.points.ownedBy(PointOwner.job("police")).size());
        t.events.clear();
        assertTrue(t.points.remove(a.id()));
        assertFalse(t.points.remove(a.id()));
        assertEquals(1, t.events.stream().filter(e -> e instanceof PointRemovedEvent).count());
        assertTrue(t.points.ownedBy(PointOwner.job("police")).isEmpty());
        assertEquals(1, t.points.all().size());
    }

    // ---- inattività ----------------------------------------------------------------------------------------

    @Test
    void pointsBecomeInactiveWhenTypeOwnerOrGradeDisappear() {
        InteractionPoint p = t.create("duty", PointOwner.job("police"), 1, "cadet");
        assertTrue(t.points.inactiveReason(p).isEmpty());

        InteractionPoint noType = new InteractionPoint(9, "gone", p.owner(), p.position(), null, false, null, null, p.createdAt());
        assertEquals(InactiveReason.UNKNOWN_TYPE, t.points.inactiveReason(noType).orElseThrow());

        InteractionPoint noOwner = new InteractionPoint(9, "duty", PointOwner.job("ghost"), p.position(), null, false, null, null,
                p.createdAt());
        assertEquals(InactiveReason.UNKNOWN_OWNER, t.points.inactiveReason(noOwner).orElseThrow());

        InteractionPoint noGrade = new InteractionPoint(9, "duty", p.owner(), p.position(), "general", false, null, null,
                p.createdAt());
        assertEquals(InactiveReason.UNKNOWN_GRADE, t.points.inactiveReason(noGrade).orElseThrow());

        t.gang();
        InteractionPoint org = t.create("locker", PointOwner.organization("gang"), 2, null);
        OrgsEnv.ok(t.env.orgs.archive("gang"));
        assertEquals(InactiveReason.UNKNOWN_OWNER, t.points.inactiveReason(org).orElseThrow());
        // il punto resta salvato
        assertTrue(t.points.find(org.id()).isPresent());
    }

    // ---- regola di accesso ---------------------------------------------------------------------------------

    @Test
    void jobPointRequiresMatchingJobAndKnownAssignment() {
        InteractionPoint p = t.create("duty", PointOwner.job("police"), 1, null);
        PlayerCharacter cop = t.env.newCharacter();
        PlayerCharacter baker = t.env.newCharacter();
        PlayerCharacter jobless = t.env.newCharacter();
        t.env.jobs.assign(cop.id(), "police", "cadet");
        t.env.jobs.assign(baker.id(), "baker", "apprentice");
        assertTrue(t.points.canUse(cop.id(), p));
        assertFalse(t.points.canUse(baker.id(), p));
        assertFalse(t.points.canUse(jobless.id(), p));
    }

    @Test
    void jobPointRespectsMinimumGradeLevel() {
        InteractionPoint p = t.create("duty", PointOwner.job("police"), 1, "chief");
        PlayerCharacter cadet = t.env.newCharacter();
        PlayerCharacter chief = t.env.newCharacter();
        t.env.jobs.assign(cadet.id(), "police", "cadet");
        t.env.jobs.assign(chief.id(), "police", "chief");
        assertFalse(t.points.canUse(cadet.id(), p));
        assertTrue(t.points.canUse(chief.id(), p));
        t.points.setMinGrade(p.id(), "cadet");
        InteractionPoint relaxed = t.points.find(p.id()).orElseThrow();
        assertTrue(t.points.canUse(cadet.id(), relaxed));
    }

    @Test
    void requiresDutyOnlyAppliesToJobOwners() {
        InteractionPoint jobLocker = t.create("locker", PointOwner.job("police"), 1, null);
        PlayerCharacter cop = t.env.newCharacter();
        t.env.jobs.assign(cop.id(), "police", "cadet");
        assertFalse(t.points.canUse(cop.id(), jobLocker));
        t.env.jobs.setOnDuty(cop.id(), true, DutyReason.MANUAL);
        assertTrue(t.points.canUse(cop.id(), jobLocker));

        t.gang();
        InteractionPoint orgLocker = t.create("locker", PointOwner.organization("gang"), 2, null);
        PlayerCharacter member = t.env.newCharacter();
        OrgsEnv.ok(t.env.orgs.addMember("gang", member.id(), "soldier"));
        assertTrue(t.points.canUse(member.id(), orgLocker));
    }

    @Test
    void dutyTypeWorksWhileOffDuty() {
        InteractionPoint p = t.create("duty", PointOwner.job("police"), 1, null);
        PlayerCharacter cop = t.env.newCharacter();
        t.env.jobs.assign(cop.id(), "police", "cadet");
        assertFalse(t.env.jobs.assignmentOf(cop.id()).orElseThrow().onDuty());
        assertTrue(t.points.canUse(cop.id(), p));
    }

    @Test
    void organizationPointNeedsExplicitMembershipAndRank() {
        t.gang();
        InteractionPoint p = t.create("locker", PointOwner.organization("gang"), 1, "boss");
        PlayerCharacter soldier = t.env.newCharacter();
        PlayerCharacter boss = t.env.newCharacter();
        PlayerCharacter outsider = t.env.newCharacter();
        OrgsEnv.ok(t.env.orgs.addMember("gang", soldier.id(), "soldier"));
        OrgsEnv.ok(t.env.orgs.addMember("gang", boss.id(), "boss"));
        assertFalse(t.points.canUse(soldier.id(), p));
        assertTrue(t.points.canUse(boss.id(), p));
        assertFalse(t.points.canUse(outsider.id(), p));
    }

    @Test
    void implicitMembershipFromLinkedJobDoesNotCount() {
        // "police" è collegato all'organizzazione "pd": un agente in servizio ne è membro implicito
        OrgsEnv.ok(t.env.orgs.create("pd", "Polizia"));
        OrgsEnv.ok(t.env.orgs.defineRank("pd", new dev.qrm.api.org.Rank("member", "Membro", 1, java.util.Set.of())));
        InteractionPoint p = t.create("locker", PointOwner.organization("pd"), 1, null);
        PlayerCharacter cop = t.env.newCharacter();
        t.env.jobs.assign(cop.id(), "police", "cadet");
        t.env.jobs.setOnDuty(cop.id(), true, DutyReason.MANUAL);
        assertTrue(t.env.orgs.organizationsOf(cop.id()).stream().anyMatch(o -> o.id().equals("pd")));
        assertFalse(t.points.canUse(cop.id(), p));
    }

    @Test
    void archivedOrganizationDeniesAccess() {
        t.gang();
        InteractionPoint p = t.create("locker", PointOwner.organization("gang"), 1, null);
        PlayerCharacter member = t.env.newCharacter();
        OrgsEnv.ok(t.env.orgs.addMember("gang", member.id(), "soldier"));
        assertTrue(t.points.canUse(member.id(), p));
        OrgsEnv.ok(t.env.orgs.archive("gang"));
        assertFalse(t.points.canUse(member.id(), p));
    }

    @Test
    void nonActiveCharacterIsDenied() {
        InteractionPoint p = t.create("duty", PointOwner.job("police"), 1, null);
        PlayerCharacter cop = t.env.newCharacter();
        t.env.jobs.assign(cop.id(), "police", "cadet");
        assertTrue(t.points.canUse(cop.id(), p));
        t.env.chars.archive(cop.playerId(), cop.id());
        assertFalse(t.points.canUse(cop.id(), p));
        assertFalse(t.points.canUse(dev.qrm.api.id.CharacterId.random(), p));
    }

    @Test
    void inactivePointDeniesAccess() {
        PlayerCharacter cop = t.env.newCharacter();
        t.env.jobs.assign(cop.id(), "police", "cadet");
        InteractionPoint orphanType = new InteractionPoint(9, "gone", PointOwner.job("police"), pos(1), null, false, null, null,
                java.time.Instant.EPOCH);
        assertFalse(t.points.canUse(cop.id(), orphanType));
    }

    @Test
    void unknownAssignmentDeniesAccess() {
        InteractionPoint p = t.create("duty", PointOwner.job("police"), 1, null);
        PlayerCharacter cop = t.env.newCharacter();
        t.env.jobs.assign(cop.id(), "police", "cadet");
        t.env.registry.remove("police");
        assertFalse(t.points.canUse(cop.id(), p));
    }
}
