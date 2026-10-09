package dev.qrm.neoforge.server;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.character.CreateCharacterResult;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.core.CoreConfig;
import dev.qrm.core.character.DefaultCharacterService;
import dev.qrm.core.db.Database;
import dev.qrm.core.db.Migrator;
import dev.qrm.core.event.SimpleEventBus;
import dev.qrm.core.job.DefaultJobService;
import dev.qrm.core.job.InMemoryJobRegistry;
import dev.qrm.core.player.DefaultPlayerService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DutyActionsTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);

    private Database db;
    private DefaultCharacterService chars;
    private DefaultJobService jobs;
    private InMemoryJobRegistry registry;
    private PlayerId pid;

    @BeforeEach
    void setUp() {
        db = Database.open("jdbc:sqlite::memory:");
        Migrator.migrate(db);
        SimpleEventBus bus = new SimpleEventBus();
        CoreConfig cfg = CoreConfig.defaults();
        DefaultPlayerService players = new DefaultPlayerService(db, CLOCK, cfg);
        chars = new DefaultCharacterService(db, bus, CLOCK, cfg);
        registry = new InMemoryJobRegistry();
        jobs = new DefaultJobService(db, bus, registry, CLOCK);
        pid = new PlayerId(UUID.randomUUID());
        players.onJoin(pid);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private PlayerCharacter character() {
        return ((CreateCharacterResult.Created) chars.create(pid, "Anna", "Rossi", LocalDate.of(1990, 1, 1)))
                .character();
    }

    private void hire(PlayerCharacter pc) {
        registry.register(new JobDefinition("police", "Polizia",
                List.of(new GradeDefinition("cadet", "Recluta", 1, Set.of(), null))));
        jobs.assign(pc.id(), "police", "cadet");
    }

    private DutyActions.Result set(boolean on, boolean allowed) {
        return DutyActions.setDuty(chars, jobs, pid, on, allowed);
    }

    @Test
    void disabledWinsOverEverythingElse() {
        PlayerCharacter pc = character();
        hire(pc);
        assertEquals(DutyActions.Result.DISABLED, set(true, false));
        assertFalse(jobs.assignmentOf(pc.id()).orElseThrow().onDuty());
    }

    @Test
    void noActiveCharacter() {
        assertEquals(DutyActions.Result.NO_CHARACTER, set(true, true));
    }

    @Test
    void noJob() {
        character();
        assertEquals(DutyActions.Result.NO_JOB, set(true, true));
    }

    @Test
    void goingOnAndOffDuty() {
        PlayerCharacter pc = character();
        hire(pc);
        assertEquals(DutyActions.Result.OK, set(true, true));
        assertTrue(jobs.assignmentOf(pc.id()).orElseThrow().onDuty());
        assertEquals(DutyActions.Result.OK, set(false, true));
        assertFalse(jobs.assignmentOf(pc.id()).orElseThrow().onDuty());
    }

    @Test
    void alreadyInTheRequestedState() {
        PlayerCharacter pc = character();
        hire(pc);
        assertEquals(DutyActions.Result.ALREADY, set(false, true));
        set(true, true);
        assertEquals(DutyActions.Result.ALREADY, set(true, true));
    }

    @Test
    void toggleFlipsTheCurrentState() {
        PlayerCharacter pc = character();
        hire(pc);
        DutyActions.Toggle first = DutyActions.toggle(chars, jobs, pid, true);
        assertEquals(DutyActions.Result.OK, first.result());
        assertTrue(first.on());
        assertTrue(jobs.assignmentOf(pc.id()).orElseThrow().onDuty());
        DutyActions.Toggle second = DutyActions.toggle(chars, jobs, pid, true);
        assertEquals(DutyActions.Result.OK, second.result());
        assertFalse(second.on());
        assertFalse(jobs.assignmentOf(pc.id()).orElseThrow().onDuty());
    }

    @Test
    void toggleReportsTheSameProblemsAsSetDuty() {
        assertEquals(DutyActions.Result.NO_CHARACTER, DutyActions.toggle(chars, jobs, pid, true).result());
        PlayerCharacter pc = character();
        assertEquals(DutyActions.Result.NO_JOB, DutyActions.toggle(chars, jobs, pid, true).result());
        hire(pc);
        assertEquals(DutyActions.Result.DISABLED, DutyActions.toggle(chars, jobs, pid, false).result());
        assertFalse(jobs.assignmentOf(pc.id()).orElseThrow().onDuty());
    }

    @Test
    void jobNoLongerDefined() {
        PlayerCharacter pc = character();
        hire(pc);
        registry.remove("police");
        assertEquals(DutyActions.Result.UNKNOWN_JOB, set(true, true));
    }
}
