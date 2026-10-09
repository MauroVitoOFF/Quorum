package dev.qrm.core.job;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.character.CharacterStatus;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultJobServiceTest {
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private JobsEnv env;

    @BeforeEach
    void setUp() {
        env = new JobsEnv();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private Assignment assigned(AssignResult r) {
        return assertInstanceOf(AssignResult.Assigned.class, r).assignment();
    }

    @Test
    void assignCreatesOffDutyAssignmentAndFiresEvent() {
        PlayerCharacter c = env.newCharacter();
        Assignment a = assigned(env.jobs.assign(c.id(), "police", "cadet"));
        assertEquals("police", a.jobId());
        assertEquals("cadet", a.gradeId());
        assertFalse(a.onDuty());
        assertTrue(a.known());
        assertEquals(T0, a.since());
        assertEquals(a, env.jobs.assignmentOf(c.id()).orElseThrow());
        assertEquals(List.of(new JobChangedEvent(c.id(), null, a)), env.events);
    }

    @Test
    void rejectsUnknownCharacterJobAndGrade() {
        PlayerCharacter c = env.newCharacter();
        assertInstanceOf(AssignResult.UnknownCharacter.class, env.jobs.assign(CharacterId.random(), "police", "cadet"));
        assertEquals(new AssignResult.UnknownJob("nope"), env.jobs.assign(c.id(), "nope", "cadet"));
        assertEquals(new AssignResult.UnknownGrade("nope"), env.jobs.assign(c.id(), "police", "nope"));
        assertTrue(env.jobs.assignmentOf(c.id()).isEmpty());
    }

    @Test
    void archivedCharacterIsNotUsable() {
        PlayerCharacter c = env.newCharacter();
        env.chars.archive(c.playerId(), c.id());
        assertEquals(new AssignResult.NotUsable(CharacterStatus.ARCHIVED), env.jobs.assign(c.id(), "police", "cadet"));
    }

    @Test
    void sameJobAndGradeIsNoOp() {
        PlayerCharacter c = env.newCharacter();
        env.jobs.assign(c.id(), "police", "cadet");
        env.events.clear();
        env.clock.advance(Duration.ofHours(1));
        Assignment again = assigned(env.jobs.assign(c.id(), "police", "cadet"));
        assertEquals(T0, again.since());
        assertTrue(env.events.isEmpty());
    }

    @Test
    void setGradeKeepsDutyAndSince() {
        PlayerCharacter c = env.newCharacter();
        env.jobs.assign(c.id(), "police", "cadet");
        env.jobs.setOnDuty(c.id(), true, DutyReason.MANUAL);
        env.clock.advance(Duration.ofHours(1));
        env.events.clear();
        Assignment a = assigned(env.jobs.setGrade(c.id(), "officer"));
        assertEquals("officer", a.gradeId());
        assertTrue(a.onDuty());
        assertEquals(T0, a.since());
        assertEquals(1, env.events.size());
        assertInstanceOf(JobChangedEvent.class, env.events.get(0));
    }

    @Test
    void changingJobResetsDutyAndSince() {
        PlayerCharacter c = env.newCharacter();
        env.jobs.assign(c.id(), "police", "cadet");
        env.jobs.setOnDuty(c.id(), true, DutyReason.MANUAL);
        env.clock.advance(Duration.ofHours(1));
        env.events.clear();
        Assignment a = assigned(env.jobs.assign(c.id(), "mechanic", "apprentice"));
        assertFalse(a.onDuty());
        assertEquals(T0.plusSeconds(3600), a.since());
        assertEquals(2, env.events.size());
        assertInstanceOf(JobChangedEvent.class, env.events.get(0));
        assertEquals(new DutyChangedEvent(c.id(), false, DutyReason.JOB_CHANGE), env.events.get(1));
    }

    @Test
    void setGradeErrors() {
        PlayerCharacter c = env.newCharacter();
        assertInstanceOf(AssignResult.NoAssignment.class, env.jobs.setGrade(c.id(), "officer"));
        env.jobs.assign(c.id(), "police", "cadet");
        assertEquals(new AssignResult.UnknownGrade("zzz"), env.jobs.setGrade(c.id(), "zzz"));
    }

    @Test
    void preEventCanCancelAndSeesTheChange() {
        List<JobChangePreEvent> seen = new ArrayList<>();
        env.bus.register(JobChangePreEvent.class, "gate", e -> {
            seen.add(e);
            e.cancel("gate", "not allowed");
        });
        PlayerCharacter c = env.newCharacter();
        assertEquals(new AssignResult.Cancelled("gate", "not allowed"), env.jobs.assign(c.id(), "police", "cadet"));
        assertTrue(env.jobs.assignmentOf(c.id()).isEmpty());
        assertEquals(c.id(), seen.get(0).character());
        assertNull(seen.get(0).from());
        assertEquals("police", seen.get(0).toJobId());
        assertEquals("cadet", seen.get(0).toGradeId());
        assertTrue(env.events.isEmpty());
    }

    @Test
    void dismissRemovesAssignmentAndFiresEvents() {
        PlayerCharacter c = env.newCharacter();
        assertFalse(env.jobs.dismiss(c.id()));
        env.jobs.assign(c.id(), "police", "officer");
        env.jobs.setOnDuty(c.id(), true, DutyReason.MANUAL);
        Assignment before = env.jobs.assignmentOf(c.id()).orElseThrow();
        env.events.clear();
        assertTrue(env.jobs.dismiss(c.id()));
        assertTrue(env.jobs.assignmentOf(c.id()).isEmpty());
        assertEquals(List.of(new JobChangedEvent(c.id(), before, null),
                new DutyChangedEvent(c.id(), false, DutyReason.JOB_CHANGE)), env.events);
    }

    @Test
    void dismissCancelledKeepsAssignment() {
        PlayerCharacter c = env.newCharacter();
        env.jobs.assign(c.id(), "police", "cadet");
        env.bus.register(JobChangePreEvent.class, "gate", e -> e.cancel("gate", "no"));
        assertFalse(env.jobs.dismiss(c.id()));
        assertTrue(env.jobs.assignmentOf(c.id()).isPresent());
    }

    @Test
    void setOnDutyTogglesAndIsIdempotent() {
        PlayerCharacter c = env.newCharacter();
        assertFalse(env.jobs.setOnDuty(c.id(), true, DutyReason.MANUAL));
        env.jobs.assign(c.id(), "police", "cadet");
        assertTrue(env.jobs.setOnDuty(c.id(), true, DutyReason.MANUAL));
        assertFalse(env.jobs.setOnDuty(c.id(), true, DutyReason.MANUAL));
        assertTrue(env.jobs.assignmentOf(c.id()).orElseThrow().onDuty());
        assertTrue(env.jobs.setOnDuty(c.id(), false, DutyReason.LOGOUT));
        assertEquals(new DutyChangedEvent(c.id(), true, DutyReason.MANUAL), env.events.get(1));
        assertEquals(new DutyChangedEvent(c.id(), false, DutyReason.LOGOUT), env.events.get(2));
        assertEquals(3, env.events.size());
    }

    @Test
    void orphanAssignmentIsKeptButNotKnown() {
        PlayerCharacter c = env.newCharacter();
        env.jobs.assign(c.id(), "police", "officer");
        env.jobs.setOnDuty(c.id(), true, DutyReason.MANUAL);
        env.registry.remove("police");

        Assignment a = env.jobs.assignmentOf(c.id()).orElseThrow();
        assertFalse(a.known());
        assertEquals("police", a.jobId());
        assertTrue(env.jobs.membersOf("police").contains(c.id()));
        assertTrue(env.jobs.setOnDuty(c.id(), false, DutyReason.MANUAL));
        assertFalse(env.jobs.setOnDuty(c.id(), true, DutyReason.MANUAL));

        env.registry.register(JobsEnv.police());
        assertTrue(env.jobs.assignmentOf(c.id()).orElseThrow().known());
        assertTrue(env.jobs.setOnDuty(c.id(), true, DutyReason.MANUAL));
    }

    @Test
    void membersOfExcludesArchivedCharacters() {
        PlayerCharacter a = env.newCharacter();
        PlayerCharacter b = env.newCharacter();
        env.jobs.assign(a.id(), "police", "cadet");
        env.jobs.assign(b.id(), "police", "cadet");
        env.chars.archive(b.playerId(), b.id());
        assertEquals(List.of(a.id()), env.jobs.membersOf("police"));
        assertEquals(List.of(), env.jobs.membersOf("mechanic"));
    }

    @Test
    void setOthersOffDutyOnlyAffectsTheSamePlayersOtherCharacters() {
        PlayerId pid = new PlayerId(UUID.randomUUID());
        PlayerCharacter c1 = env.newCharacter(pid);
        PlayerCharacter c2 = env.newCharacter(pid);
        PlayerCharacter stranger = env.newCharacter();
        for (PlayerCharacter c : List.of(c1, c2, stranger)) {
            env.jobs.assign(c.id(), "police", "cadet");
            env.jobs.setOnDuty(c.id(), true, DutyReason.MANUAL);
        }
        env.events.clear();

        assertEquals(1, env.jobs.setOthersOffDuty(pid, c1.id(), DutyReason.SWITCH));

        assertTrue(env.jobs.assignmentOf(c1.id()).orElseThrow().onDuty());
        assertFalse(env.jobs.assignmentOf(c2.id()).orElseThrow().onDuty());
        assertTrue(env.jobs.assignmentOf(stranger.id()).orElseThrow().onDuty());
        assertEquals(List.of(new DutyChangedEvent(c2.id(), false, DutyReason.SWITCH)), env.events);
    }
}
