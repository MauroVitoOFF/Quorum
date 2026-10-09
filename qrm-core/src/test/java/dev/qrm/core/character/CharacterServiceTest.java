package dev.qrm.core.character;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.character.*;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.player.QrmPlayer;
import dev.qrm.core.CoreConfig;
import dev.qrm.core.TestClock;
import dev.qrm.core.db.Database;
import dev.qrm.core.db.Migrator;
import dev.qrm.core.event.SimpleEventBus;
import dev.qrm.core.player.DefaultPlayerService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CharacterServiceTest {
    private static final LocalDate BIRTH = LocalDate.of(1990, 5, 17);

    private Database db;
    private SimpleEventBus bus;
    private TestClock clock;
    private DefaultPlayerService players;
    private DefaultCharacterService chars;
    private final PlayerId pid = new PlayerId(UUID.randomUUID());
    private final List<Object> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        db = Database.open("jdbc:sqlite::memory:");
        Migrator.migrate(db);
        bus = new SimpleEventBus();
        clock = new TestClock(Instant.parse("2026-01-01T00:00:00Z"));
        CoreConfig cfg = CoreConfig.defaults().withCharacterSlots(2);
        players = new DefaultPlayerService(db, clock, cfg);
        chars = new DefaultCharacterService(db, bus, clock, cfg);
        bus.register(CharacterCreatedEvent.class, "t", events::add);
        bus.register(CharacterSelectedEvent.class, "t", events::add);
        bus.register(CharacterArchivedEvent.class, "t", events::add);
        players.onJoin(pid);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private PlayerCharacter create(String first, String last) {
        CreateCharacterResult r = chars.create(pid, first, last, BIRTH);
        return ((CreateCharacterResult.Created) r).character();
    }

    @Test
    void joinCreatesPlayerWithConfiguredSlots() {
        QrmPlayer p = players.find(pid).orElseThrow();
        assertEquals(2, p.characterSlots());
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), p.firstSeen());
    }

    @Test
    void secondJoinUpdatesLastSeenOnly() {
        clock.advance(Duration.ofHours(1));
        QrmPlayer p = players.onJoin(pid);
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), p.firstSeen());
        assertEquals(Instant.parse("2026-01-01T01:00:00Z"), p.lastSeen());
    }

    @Test
    void createReturnsActiveCharacterAndFiresEvent() {
        PlayerCharacter c = create("Mario", "Rossi");
        assertEquals(CharacterStatus.ACTIVE, c.status());
        assertEquals("Mario", c.firstName());
        assertTrue(events.contains(new CharacterCreatedEvent(c)));
    }

    @Test
    void firstCharacterIsAutoSelectedSecondIsNot() {
        PlayerCharacter first = create("Mario", "Rossi");
        create("Luigi", "Verdi");
        assertEquals(first.id(), chars.activeOf(pid).orElseThrow().id());
    }

    @Test
    void unknownPlayerRejected() {
        var r = chars.create(new PlayerId(UUID.randomUUID()), "Mario", "Rossi", BIRTH);
        assertInstanceOf(CreateCharacterResult.UnknownPlayer.class, r);
    }

    @Test
    void slotsFull() {
        create("Mario", "Rossi");
        create("Luigi", "Verdi");
        assertInstanceOf(CreateCharacterResult.SlotsFull.class, chars.create(pid, "Anna", "Neri", BIRTH));
    }

    @Test
    void invalidNames() {
        assertInstanceOf(CreateCharacterResult.InvalidName.class, chars.create(pid, "M", "Rossi", BIRTH));
        assertInstanceOf(CreateCharacterResult.InvalidName.class, chars.create(pid, "Ma1", "Rossi", BIRTH));
        assertInstanceOf(CreateCharacterResult.InvalidName.class, chars.create(pid, "Mario", "x".repeat(25), BIRTH));
        assertInstanceOf(CreateCharacterResult.InvalidName.class, chars.create(pid, null, "Rossi", BIRTH));
    }

    @Test
    void duplicateNameRejectedCaseInsensitively() {
        create("Mario", "Rossi");
        assertInstanceOf(CreateCharacterResult.NameTaken.class, chars.create(pid, "MARIO", "rossi", BIRTH));
    }

    @Test
    void selectSwitchesActiveCharacter() {
        create("Mario", "Rossi");
        PlayerCharacter second = create("Luigi", "Verdi");
        var r = chars.select(pid, second.id());
        assertInstanceOf(SelectCharacterResult.Selected.class, r);
        assertEquals(second.id(), chars.activeOf(pid).orElseThrow().id());
        assertTrue(events.contains(new CharacterSelectedEvent(second)));
    }

    @Test
    void selectRejectsForeignOrMissingCharacter() {
        PlayerCharacter mine = create("Mario", "Rossi");
        PlayerId other = new PlayerId(UUID.randomUUID());
        players.onJoin(other);
        assertInstanceOf(SelectCharacterResult.NotOwner.class, chars.select(other, mine.id()));
        assertInstanceOf(SelectCharacterResult.NotFound.class, chars.select(pid, CharacterId.random()));
    }

    @Test
    void archiveFreesSlotClearsActiveAndBlocksSelect() {
        PlayerCharacter a = create("Mario", "Rossi");
        create("Luigi", "Verdi");
        assertTrue(chars.archive(pid, a.id()));
        assertTrue(chars.activeOf(pid).isEmpty());
        assertInstanceOf(SelectCharacterResult.NotUsable.class, chars.select(pid, a.id()));
        assertInstanceOf(CreateCharacterResult.Created.class, chars.create(pid, "Anna", "Neri", BIRTH));
        assertEquals(CharacterStatus.ARCHIVED, chars.find(a.id()).orElseThrow().status());
        assertFalse(chars.archive(pid, a.id()));
        assertEquals(3, chars.listOf(pid).size());
    }

    @Test
    void archivedNameCanBeReused() {
        PlayerCharacter a = create("Mario", "Rossi");
        chars.archive(pid, a.id());
        assertInstanceOf(CreateCharacterResult.Created.class, chars.create(pid, "Mario", "Rossi", BIRTH));
    }

    @Test
    void findByNameIsCaseInsensitiveAndTrimmed() {
        PlayerCharacter anna = create("Anna", "Rossi");
        assertEquals(List.of(anna), chars.findByName("  anna ROSSI "));
        assertTrue(chars.findByName("Anna Bianchi").isEmpty());
        assertTrue(chars.findByName("").isEmpty());
        assertTrue(chars.findByName(null).isEmpty());
    }

    @Test
    void findByNameSkipsArchivedCharacters() {
        PlayerCharacter anna = create("Anna", "Rossi");
        assertTrue(chars.archive(pid, anna.id()));
        assertTrue(chars.findByName("Anna Rossi").isEmpty());
    }
}
