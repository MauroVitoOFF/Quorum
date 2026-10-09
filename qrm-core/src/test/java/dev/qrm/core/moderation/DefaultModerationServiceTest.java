package dev.qrm.core.moderation;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.moderation.Mute;
import dev.qrm.api.moderation.StaffNote;
import dev.qrm.api.moderation.Warning;
import dev.qrm.core.db.Database;
import dev.qrm.core.db.Migrator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultModerationServiceTest {
    private static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-10-09T12:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private Database db;
    private TestClock clock;
    private DefaultModerationService svc;
    private dev.qrm.core.event.SimpleEventBus bus;
    private final UUID luca = UUID.randomUUID();
    private final UUID anna = UUID.randomUUID();
    private final UUID marco = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        db = Database.open("jdbc:sqlite::memory:");
        Migrator.migrate(db);
        clock = new TestClock();
        bus = new dev.qrm.core.event.SimpleEventBus();
        svc = new DefaultModerationService(db, bus, clock);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void timedMuteIsActiveThenExpires() {
        svc.mute(luca, "Luca", Optional.of(marco), "Marco", "spam", Optional.of(Duration.ofHours(2)));
        Mute m = svc.activeMute(luca).orElseThrow();
        assertEquals("spam", m.reason());
        assertEquals("Marco", m.staffName());
        assertEquals(Optional.of(Instant.parse("2026-10-09T14:00:00Z")), m.expiresAt());
        assertFalse(m.permanent());
        clock.now = Instant.parse("2026-10-09T13:59:00Z");
        assertTrue(svc.activeMute(luca).isPresent());
        clock.now = Instant.parse("2026-10-09T14:00:01Z");
        assertTrue(svc.activeMute(luca).isEmpty());
        assertTrue(svc.activeMutes(10).isEmpty());
    }

    @Test
    void permanentMuteNeverExpires() {
        svc.mute(luca, "Luca", Optional.empty(), "Server", "insulti", Optional.empty());
        clock.now = Instant.parse("2030-01-01T00:00:00Z");
        Mute m = svc.activeMute(luca).orElseThrow();
        assertTrue(m.permanent());
        assertTrue(m.staffId().isEmpty());
    }

    @Test
    void newMuteReplacesThePreviousOne() {
        svc.mute(luca, "Luca", Optional.of(marco), "Marco", "uno", Optional.of(Duration.ofDays(1)));
        svc.mute(luca, "Luca", Optional.of(anna), "Anna", "due", Optional.empty());
        assertEquals(1, svc.activeMutes(10).size());
        assertEquals("due", svc.activeMute(luca).orElseThrow().reason());
    }

    @Test
    void unmuteReportsWhetherThereWasAMute() {
        assertFalse(svc.unmute(luca));
        svc.mute(luca, "Luca", Optional.of(marco), "Marco", "x", Optional.empty());
        assertTrue(svc.unmute(luca));
        assertTrue(svc.activeMute(luca).isEmpty());
        assertFalse(svc.unmute(luca));
    }

    @Test
    void unmuteOfAnExpiredMuteReportsFalse() {
        svc.mute(luca, "Luca", Optional.of(marco), "Marco", "x", Optional.of(Duration.ofMinutes(5)));
        clock.now = Instant.parse("2026-10-09T13:00:00Z");
        assertFalse(svc.unmute(luca));
    }

    @Test
    void activeMutesListsNewestFirstAndOnlyActiveOnes() {
        svc.mute(luca, "Luca", Optional.of(marco), "Marco", "a", Optional.of(Duration.ofMinutes(1)));
        clock.now = Instant.parse("2026-10-09T12:30:00Z");
        svc.mute(anna, "Anna", Optional.of(marco), "Marco", "b", Optional.empty());
        List<Mute> active = svc.activeMutes(10);
        assertEquals(List.of("Anna"), active.stream().map(Mute::playerName).toList());   // Luca è scaduto
    }

    @Test
    void warnReturnsTheRunningCountAndKeepsHistoryNewestFirst() {
        assertEquals(1, svc.warn(luca, "Luca", Optional.of(marco), "Marco", "primo"));
        assertEquals(2, svc.warn(luca, "Luca", Optional.of(anna), "Anna", "secondo"));
        assertEquals(1, svc.warn(anna, "Anna", Optional.of(marco), "Marco", "altro"));
        assertEquals(2, svc.warningCount(luca));
        List<Warning> w = svc.warningsOf(luca, 10);
        assertEquals(List.of("secondo", "primo"), w.stream().map(Warning::reason).toList());
        assertEquals(List.of("altro", "secondo", "primo"), svc.recentWarnings(10).stream().map(Warning::reason).toList());
        assertEquals(2, svc.recentWarnings(2).size());
    }

    @Test
    void notesAreStoredPerPlayerNewestFirst() {
        svc.addNote(luca, "Luca", Optional.of(marco), "Marco", "sospetto xray");
        svc.addNote(luca, "Luca", Optional.of(anna), "Anna", "ha chiesto scusa");
        svc.addNote(anna, "Anna", Optional.of(marco), "Marco", "staff nuova");
        assertEquals(2, svc.noteCount(luca));
        List<StaffNote> n = svc.notesOf(luca, 10);
        assertEquals(List.of("ha chiesto scusa", "sospetto xray"), n.stream().map(StaffNote::text).toList());
        assertEquals(1, svc.noteCount(anna));
        assertEquals(0, svc.noteCount(UUID.randomUUID()));
    }

    @Test
    void longReasonsAndNotesAreTruncated() {
        svc.warn(luca, "Luca", Optional.of(marco), "Marco", "x".repeat(500));
        assertEquals(128, svc.warningsOf(luca, 1).get(0).reason().length());
        svc.addNote(luca, "Luca", Optional.of(marco), "Marco", "y".repeat(500));
        assertEquals(200, svc.notesOf(luca, 1).get(0).text().length());
    }

    @Test
    void limitsAreClampedAndDataSurvivesAnotherInstance() {
        svc.warn(luca, "Luca", Optional.of(marco), "Marco", "a");
        svc.warn(luca, "Luca", Optional.of(marco), "Marco", "b");
        assertEquals(1, svc.warningsOf(luca, 0).size());                 // minimo 1
        DefaultModerationService again = new DefaultModerationService(db, bus, clock);
        assertEquals(3, again.warn(luca, "Luca", Optional.of(marco), "Marco", "c"));
        assertEquals("c", again.warningsOf(luca, 1).get(0).reason());
        assertTrue(again.warningsOf(luca, 1).get(0).id() > svc.warningsOf(luca, 3).get(2).id());
    }

    @Test
    void muteAndUnmutePublishAChangeEventButExpiryDoesNot() {
        List<UUID> seen = new java.util.ArrayList<>();
        bus.register(dev.qrm.api.moderation.MuteChangedEvent.class, "test", e -> seen.add(e.player()));
        svc.mute(luca, "Luca", Optional.of(marco), "Marco", "x", Optional.of(Duration.ofMinutes(5)));
        assertEquals(List.of(luca), seen);
        assertTrue(svc.unmute(luca));
        assertEquals(List.of(luca, luca), seen);
        assertFalse(svc.unmute(luca));                                   // niente da togliere: nessun evento
        assertEquals(2, seen.size());
        svc.mute(anna, "Anna", Optional.of(marco), "Marco", "y", Optional.of(Duration.ofMinutes(5)));
        clock.now = Instant.parse("2026-10-09T13:00:00Z");
        assertTrue(svc.activeMute(anna).isEmpty());                      // scaduto da solo
        assertEquals(List.of(luca, luca, anna), seen);
    }
}
