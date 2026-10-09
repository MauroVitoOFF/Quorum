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
import dev.qrm.core.economy.DefaultAccountService;
import dev.qrm.core.event.SimpleEventBus;
import dev.qrm.core.job.DefaultJobService;
import dev.qrm.core.job.DefaultPermissionService;
import dev.qrm.core.job.InMemoryJobRegistry;
import dev.qrm.core.moderation.DefaultModerationService;
import dev.qrm.core.org.DefaultOrganizationService;
import dev.qrm.core.player.DefaultPlayerService;
import dev.qrm.neoforge.net.PanelSnapshot;
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

class PanelSnapshotsTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);

    private Database db;
    private DefaultCharacterService chars;
    private DefaultJobService jobs;
    private DefaultOrganizationService orgs;
    private InMemoryJobRegistry registry;
    private DefaultModerationService moderation;
    private UUID target;
    private PlayerCharacter character;

    @BeforeEach
    void setUp() {
        db = Database.open("jdbc:sqlite::memory:");
        Migrator.migrate(db);
        CoreConfig cfg = CoreConfig.defaults();
        SimpleEventBus bus = new SimpleEventBus();
        DefaultPlayerService players = new DefaultPlayerService(db, CLOCK, cfg);
        chars = new DefaultCharacterService(db, bus, CLOCK, cfg);
        DefaultAccountService accounts = new DefaultAccountService(db, bus, CLOCK);
        registry = new InMemoryJobRegistry();
        moderation = new DefaultModerationService(db, bus, CLOCK);
        jobs = new DefaultJobService(db, bus, registry, CLOCK);
        DefaultPermissionService perms = new DefaultPermissionService(db, bus, registry, jobs);
        orgs = new DefaultOrganizationService(db, bus, accounts, registry, jobs, perms, CLOCK);
        registry.register(new JobDefinition("police", "Polizia",
                List.of(new GradeDefinition("cadet", "Cadetto", 1, Set.of(), null))));
        target = UUID.randomUUID();
        PlayerId pid = new PlayerId(target);
        players.onJoin(pid);
        character = ((CreateCharacterResult.Created) chars.create(pid, "Anna", "Rossi", LocalDate.of(1990, 3, 12))).character();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private PanelSnapshot build(List<PanelSnapshots.Online> online) {
        return PanelSnapshots.build(online, chars, jobs, registry, orgs, moderation);
    }

    @Test
    void listsPlayersSortedByNameWithTheirCharacter() {
        UUID other = UUID.randomUUID();
        PanelSnapshot s = build(List.of(new PanelSnapshots.Online(other, "Zed"),
                new PanelSnapshots.Online(target, "anna_player")));
        assertEquals("anna_player", s.players().get(0).name());
        assertEquals(Optional.of("Anna Rossi"), s.players().get(0).character());
        assertEquals("Zed", s.players().get(1).name());
        assertEquals(Optional.empty(), s.players().get(1).character());
    }

    @Test
    void showsJobAndOrganizationsOfTheActiveCharacter() {
        jobs.assign(character.id(), "police", "cadet");
        orgs.create("club", "Club");
        orgs.defineRank("club", new dev.qrm.api.org.Rank("boss", "Boss", 1, Set.of()));
        orgs.addMember("club", character.id(), "boss");
        PanelSnapshot s = build(List.of(new PanelSnapshots.Online(target, "anna_player"),
                new PanelSnapshots.Online(UUID.randomUUID(), "nobody")));
        PanelSnapshot.PlayerEntry anna = s.players().stream().filter(p -> p.id().equals(target)).findFirst().orElseThrow();
        assertEquals(Optional.of("Polizia"), anna.job());
        assertTrue(anna.organization().orElseThrow().contains("Club"));
        PanelSnapshot.PlayerEntry nobody = s.players().stream().filter(p -> !p.id().equals(target)).findFirst().orElseThrow();
        assertEquals(Optional.empty(), nobody.job());
        assertEquals(Optional.empty(), nobody.organization());
    }

    @Test
    void emptyServerGivesAnEmptySnapshot() {
        assertTrue(build(List.of()).players().isEmpty());
    }

    @Test
    void showsTheModerationStandingOfEachPlayer() {
        UUID other = UUID.randomUUID();
        moderation.warn(target, "anna_player", Optional.empty(), "Server", "uno");
        moderation.warn(target, "anna_player", Optional.empty(), "Server", "due");
        moderation.addNote(target, "anna_player", Optional.empty(), "Server", "nota");
        moderation.mute(target, "anna_player", Optional.empty(), "Server", "spam", Optional.empty());
        moderation.mute(other, "Zed", Optional.empty(), "Server", "x", Optional.of(java.time.Duration.ofHours(1)));
        PanelSnapshot s = build(List.of(new PanelSnapshots.Online(target, "anna_player"),
                new PanelSnapshots.Online(other, "Zed"), new PanelSnapshots.Online(UUID.randomUUID(), "pulito")));
        PanelSnapshot.Standing anna = s.players().stream().filter(p -> p.id().equals(target)).findFirst().orElseThrow().standing();
        assertEquals(PanelSnapshot.Standing.PERMANENT, anna.muteUntil());
        assertEquals(2, anna.warnings());
        assertEquals(1, anna.notes());
        PanelSnapshot.Standing zed = s.players().stream().filter(p -> p.id().equals(other)).findFirst().orElseThrow().standing();
        assertEquals(Instant.parse("2026-06-15T13:00:00Z").toEpochMilli(), zed.muteUntil());
        PanelSnapshot.Standing clean = s.players().stream().filter(p -> p.name().equals("pulito")).findFirst().orElseThrow().standing();
        assertEquals(PanelSnapshot.Standing.NONE, clean);
    }
}
