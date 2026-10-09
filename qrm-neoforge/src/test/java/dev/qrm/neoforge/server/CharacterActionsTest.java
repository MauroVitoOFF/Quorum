package dev.qrm.neoforge.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.character.CreateCharacterResult;
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
import dev.qrm.neoforge.net.CharacterCard;
import dev.qrm.neoforge.net.ResultCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CharacterActionsTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);

    private Database db;
    private CoreConfig cfg;
    private DefaultPlayerService players;
    private DefaultCharacterService chars;
    private DefaultJobService jobs;
    private InMemoryJobRegistry registry;
    private CharacterActions actions;
    private PlayerId pid;

    @BeforeEach
    void setUp() {
        db = Database.open("jdbc:sqlite::memory:");
        Migrator.migrate(db);
        build(CoreConfig.defaults().withCharacterSlots(2));
    }

    private void build(CoreConfig config) {
        cfg = config;
        SimpleEventBus bus = new SimpleEventBus();
        players = new DefaultPlayerService(db, CLOCK, cfg);
        chars = new DefaultCharacterService(db, bus, CLOCK, cfg);
        registry = new InMemoryJobRegistry();
        jobs = new DefaultJobService(db, bus, registry, CLOCK);
        actions = new CharacterActions(chars, players, jobs, registry, CLOCK);
        pid = new PlayerId(UUID.randomUUID());
        players.onJoin(pid);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private PlayerCharacter make(String first) {
        return ((CreateCharacterResult.Created) chars.create(pid, first, "Rossi", LocalDate.of(1990, 3, 12))).character();
    }

    @Test
    void snapshotIsLimitedToMaxRosterCards() {
        build(CoreConfig.defaults().withCharacterSlots(70));
        for (int i = 0; i < 70; i++) {
            String name = "Anna" + (char) ('a' + i / 26) + (char) ('a' + i % 26);
            make(name);
        }
        CharacterActions.Snapshot s = actions.snapshot(pid);
        assertEquals(CharacterCard.MAX_ROSTER_CARDS, s.cards().size());
        assertEquals(70, s.slots());
    }

    @Test
    void newPlayerHasEmptyRosterAndSlots() {
        CharacterActions.Snapshot s = actions.snapshot(pid);
        assertEquals(List.of(), s.cards());
        assertEquals(2, s.slots());
        assertEquals(Optional.empty(), s.activeId());
    }

    @Test
    void createOkAutoSelectsTheFirstAndBuildsACard() {
        CharacterActions.Outcome o = actions.create(pid, "Anna", "Rossi", 1990, 3, 12);
        assertEquals(ResultCode.OK, o.code());
        assertEquals(1, o.snapshot().cards().size());
        CharacterCard c = o.snapshot().cards().get(0);
        assertEquals("Anna", c.firstName());
        assertEquals(LocalDate.of(1990, 3, 12), c.birthDate());
        assertEquals(CharacterCard.documentNumberOf(c.id()), c.documentNumber());
        assertEquals(Optional.empty(), c.jobLabel());
        assertEquals(Optional.of(c.id()), o.snapshot().activeId());
    }

    @Test
    void invalidDatesAreRejected() {
        assertEquals(ResultCode.INVALID_DATE, actions.create(pid, "Anna", "Rossi", 1990, 2, 30).code());
        assertEquals(ResultCode.INVALID_DATE, actions.create(pid, "Anna", "Rossi", 1990, 13, 1).code());
        assertEquals(ResultCode.INVALID_DATE, actions.create(pid, "Anna", "Rossi", 2026, 6, 16).code());   // domani
        assertEquals(ResultCode.INVALID_DATE, actions.create(pid, "Anna", "Rossi", 1899, 12, 31).code());
        assertEquals(List.of(), actions.snapshot(pid).cards());
    }

    @Test
    void dateBoundsAreInclusive() {
        assertEquals(ResultCode.OK, actions.create(pid, "Anna", "Rossi", 1900, 1, 1).code());
        assertEquals(ResultCode.OK, actions.create(pid, "Bruno", "Verdi", 2026, 6, 15).code());   // oggi
    }

    @Test
    void invalidNameAndSlotsFull() {
        assertEquals(ResultCode.INVALID_NAME, actions.create(pid, "", "Rossi", 1990, 3, 12).code());
        assertEquals(ResultCode.OK, actions.create(pid, "Anna", "Rossi", 1990, 3, 12).code());
        assertEquals(ResultCode.OK, actions.create(pid, "Bruno", "Verdi", 1991, 4, 1).code());
        assertEquals(ResultCode.SLOTS_FULL, actions.create(pid, "Carla", "Neri", 1992, 5, 2).code());
    }

    @Test
    void nameTakenWhenNamesAreUnique() {
        assumeTrue(cfg.uniqueNames());
        assertEquals(ResultCode.OK, actions.create(pid, "Anna", "Rossi", 1990, 3, 12).code());
        PlayerId other = new PlayerId(UUID.randomUUID());
        players.onJoin(other);
        assertEquals(ResultCode.NAME_TAKEN, actions.create(other, "Anna", "Rossi", 1990, 3, 12).code());
    }

    @Test
    void selectOkNotFoundNotOwner() {
        PlayerCharacter a = make("Anna");
        PlayerCharacter b = make("Bruno");
        CharacterActions.Outcome o = actions.select(pid, b.id().value());
        assertEquals(ResultCode.OK, o.code());
        assertEquals(Optional.of(b.id().value()), o.snapshot().activeId());
        assertEquals(ResultCode.NOT_FOUND, actions.select(pid, UUID.randomUUID()).code());
        PlayerId other = new PlayerId(UUID.randomUUID());
        players.onJoin(other);
        assertEquals(ResultCode.NOT_OWNER, actions.select(other, a.id().value()).code());
    }

    @Test
    void archiveRemovesFromTheRosterAndMissingIsNotFound() {
        PlayerCharacter a = make("Anna");
        make("Bruno");
        CharacterActions.Outcome o = actions.archive(pid, a.id().value());
        assertEquals(ResultCode.OK, o.code());
        assertEquals(1, o.snapshot().cards().size());
        assertEquals("Bruno", o.snapshot().cards().get(0).firstName());
        assertEquals(ResultCode.NOT_FOUND, actions.archive(pid, a.id().value()).code());
        assertEquals(ResultCode.NOT_FOUND, actions.archive(pid, UUID.randomUUID()).code());
    }

    @Test
    void jobLabelComesFromTheKnownAssignmentOnly() {
        registry.register(new JobDefinition("police", "Polizia", List.of(
                new GradeDefinition("cadet", "Recluta", 1, Set.of("police.duty"), null))));
        PlayerCharacter a = make("Anna");
        jobs.assign(a.id(), "police", "cadet");
        assertEquals(Optional.of("Polizia"), actions.snapshot(pid).cards().get(0).jobLabel());
        registry.remove("police");
        assertEquals(Optional.empty(), actions.snapshot(pid).cards().get(0).jobLabel());
    }
}
