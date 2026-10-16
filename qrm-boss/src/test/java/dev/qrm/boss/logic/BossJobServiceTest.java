package dev.qrm.boss.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.job.DutyReason;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Gestione dei membri quando l'azienda ha un Job collegato: i "rank" sono i gradi del Job. */
class BossJobServiceTest {
    private static final String JOB = BossEnv.ORG;

    private BossEnv env;
    private BossService s;

    @BeforeEach
    void setUp() {
        env = new BossEnv();
        s = env.service;
        env.registry.register(new JobDefinition(JOB, "Acme Inc", List.of(
                new GradeDefinition("worker", "Operaio", 10, Set.of(), null),
                new GradeDefinition("manager", "Manager", 20, Set.of("boss.menu", "boss.members.manage"), null),
                new GradeDefinition("boss", "Boss", 30, Set.of("boss.*", "org.account.*"), null)), JOB));
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private PlayerCharacter employee(String grade, boolean onDuty) {
        PlayerCharacter c = env.newCharacter();
        env.jobs.assign(c.id(), JOB, grade);
        if (onDuty) env.jobs.setOnDuty(c.id(), true, DutyReason.MANUAL);
        return c;
    }

    private String gradeOf(PlayerCharacter c) {
        return env.jobs.assignmentOf(c.id()).orElseThrow().gradeId();
    }

    @Test
    void bossOnDutyOpensTheMenuWithoutAnyOrgMembership() {
        PlayerCharacter boss = employee("boss", true);
        BossSnapshot snap = s.snapshot(boss.id(), JOB, List.of()).orElseThrow();
        assertTrue(snap.showMembers());
        assertEquals(30, snap.level());
        assertEquals("Boss", snap.rankLabel());
        assertTrue(snap.canNotice());
    }

    @Test
    void offDutyBossCannotOpenTheMenu() {
        PlayerCharacter boss = employee("boss", false);
        assertTrue(s.snapshot(boss.id(), JOB, List.of()).isEmpty());
        assertEquals(BossResult.NO_PERMISSION, s.addMember(boss.id(), JOB, env.newCharacter().id()));
    }

    @Test
    void membersAreTheJobHoldersWithTheirGrades() {
        PlayerCharacter boss = employee("boss", true);
        PlayerCharacter worker = employee("worker", false);
        BossSnapshot snap = s.snapshot(boss.id(), JOB, List.of()).orElseThrow();
        assertEquals(2, snap.memberCount());
        var row = snap.members().stream().filter(m -> m.character().equals(worker.id().value())).findFirst().orElseThrow();
        assertEquals("worker", row.rankId());
        assertEquals("Operaio", row.rankLabel());
        assertTrue(row.editable());
        assertEquals(List.of("worker", "manager", "boss"), snap.ranks().stream().map(r -> r.id()).toList());
    }

    @Test
    void candidatesAreOnlineCharactersWithoutAJob() {
        PlayerCharacter boss = employee("boss", true);
        PlayerCharacter free = env.newCharacter();
        PlayerCharacter busy = env.newCharacter();
        env.registry.register(new JobDefinition("taxi", "Taxi", List.of(new GradeDefinition("driver", "Autista", 1, Set.of(), null))));
        env.jobs.assign(busy.id(), "taxi", "driver");
        BossSnapshot snap = s.snapshot(boss.id(), JOB, List.of(free, busy)).orElseThrow();
        assertEquals(List.of(free.id().value()), snap.candidates().stream().map(c -> c.character()).toList());
    }

    @Test
    void addGivesTheLowestGradeAndTheCharacterIsNotAddedToTheOrg() {
        PlayerCharacter boss = employee("boss", true);
        PlayerCharacter hire = env.newCharacter();
        assertEquals(BossResult.OK, s.addMember(boss.id(), JOB, hire.id()));
        assertEquals("worker", gradeOf(hire));
        assertTrue(env.orgs.membersOf(JOB).isEmpty());
    }

    @Test
    void cannotHireSomeoneWhoAlreadyHasAnotherJobOrIsAlreadyEmployed() {
        PlayerCharacter boss = employee("boss", true);
        PlayerCharacter busy = env.newCharacter();
        env.registry.register(new JobDefinition("taxi", "Taxi", List.of(new GradeDefinition("driver", "Autista", 1, Set.of(), null))));
        env.jobs.assign(busy.id(), "taxi", "driver");
        assertEquals(BossResult.HAS_JOB, s.addMember(boss.id(), JOB, busy.id()));
        assertEquals("taxi", env.jobs.assignmentOf(busy.id()).orElseThrow().jobId());
        PlayerCharacter worker = employee("worker", false);
        assertEquals(BossResult.ALREADY_MEMBER, s.addMember(boss.id(), JOB, worker.id()));
    }

    @Test
    void setRankChangesTheGradeWithinTheBossLevel() {
        PlayerCharacter manager = employee("manager", true);
        PlayerCharacter worker = employee("worker", false);
        assertEquals(BossResult.OK, s.setRank(manager.id(), JOB, worker.id(), "manager"));
        assertEquals("manager", gradeOf(worker));
        assertEquals(BossResult.LEVEL_TOO_HIGH, s.setRank(manager.id(), JOB, worker.id(), "boss"));
        assertEquals("manager", gradeOf(worker));
    }

    @Test
    void removeDismissesFromTheJob() {
        PlayerCharacter boss = employee("boss", true);
        PlayerCharacter worker = employee("worker", false);
        assertEquals(BossResult.OK, s.removeMember(boss.id(), JOB, worker.id()));
        assertTrue(env.jobs.assignmentOf(worker.id()).isEmpty());
    }

    @Test
    void levelAndSelfRulesApplyToGrades() {
        PlayerCharacter manager = employee("manager", true);
        PlayerCharacter boss = employee("boss", false);
        assertEquals(BossResult.LEVEL_TOO_HIGH, s.removeMember(manager.id(), JOB, boss.id()));
        assertEquals(BossResult.SELF, s.removeMember(manager.id(), JOB, manager.id()));
        assertEquals(BossResult.NOT_MEMBER, s.removeMember(manager.id(), JOB, env.newCharacter().id()));
        assertTrue(env.jobs.assignmentOf(boss.id()).isPresent());
    }

    @Test
    void explicitOrgRankWithoutTheJobIsReadOnly() {
        PlayerCharacter viaRank = env.member("boss");
        PlayerCharacter worker = employee("worker", false);
        BossSnapshot snap = s.snapshot(viaRank.id(), JOB, List.of()).orElseThrow();
        assertEquals(-1, snap.level());
        assertTrue(snap.members().stream().noneMatch(BossSnapshot.Member::editable));
        assertEquals(BossResult.READ_ONLY, s.removeMember(viaRank.id(), JOB, worker.id()));
    }
}
