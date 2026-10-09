package dev.qrm.core.staff;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.staff.StaffLogEntry;
import dev.qrm.api.staff.StaffLogEntry.Outcome;
import dev.qrm.core.db.Database;
import dev.qrm.core.db.Migrator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultStaffLogServiceTest {
    /** Orologio manovrabile per fissare l'istante di ogni voce. */
    private static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-10-09T12:00:00Z");

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private Database db;
    private TestClock clock;
    private DefaultStaffLogService log;
    private final UUID marco = UUID.randomUUID();
    private final UUID luca = UUID.randomUUID();
    private final UUID anna = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        db = Database.open("jdbc:sqlite::memory:");
        Migrator.migrate(db);
        clock = new TestClock();
        log = new DefaultStaffLogService(db, clock);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private void rec(UUID staff, String name, String action, UUID target, String detail, Outcome outcome) {
        log.record(Optional.ofNullable(staff), name, action, Optional.ofNullable(target),
                Optional.ofNullable(target).map(t -> "T"), detail, outcome);
    }

    @Test
    void recentReturnsNewestFirstAndHonoursTheLimit() {
        rec(marco, "Marco", "panel.heal", luca, "", Outcome.OK);
        rec(marco, "Marco", "panel.ban", luca, "7d | spam", Outcome.OK);
        rec(marco, "Marco", "panel.kick", anna, "", Outcome.OK);
        List<StaffLogEntry> all = log.recent(10);
        assertEquals(List.of("panel.kick", "panel.ban", "panel.heal"), all.stream().map(StaffLogEntry::action).toList());
        assertEquals(2, log.recent(2).size());
        assertEquals(1, log.recent(0).size());                       // limite minimo 1
        assertEquals(3, log.recent(100000).size());                  // limite massimo 200: qui ne esistono 3
    }

    @Test
    void entriesKeepAllTheirFields() {
        clock.now = Instant.parse("2026-10-09T08:30:00Z");
        rec(marco, "Marco", "panel.give_item", luca, "diamond 16", Outcome.DENIED);
        StaffLogEntry e = log.recent(1).get(0);
        assertEquals(Optional.of(marco), e.staffId());
        assertEquals("Marco", e.staffName());
        assertEquals("panel.give_item", e.action());
        assertEquals(Optional.of(luca), e.targetId());
        assertEquals(Optional.of("T"), e.targetName());
        assertEquals("diamond 16", e.detail());
        assertEquals(Outcome.DENIED, e.outcome());
        assertEquals(Instant.parse("2026-10-09T08:30:00Z"), e.at());
    }

    @Test
    void consoleHasNoStaffId() {
        log.record(Optional.empty(), "Console", "cmd.money.grant", Optional.empty(), Optional.empty(), "money give x", Outcome.OK);
        StaffLogEntry e = log.recent(1).get(0);
        assertTrue(e.staffId().isEmpty());
        assertEquals("Console", e.staffName());
        assertTrue(e.targetId().isEmpty());
        assertTrue(e.targetName().isEmpty());
    }

    @Test
    void aboutMatchesTheStaffAndTheTargetButNotOthers() {
        rec(marco, "Marco", "panel.ban", luca, "", Outcome.OK);     // luca è il bersaglio
        rec(luca, "Luca", "panel.heal", anna, "", Outcome.OK);      // luca è lo staff
        rec(anna, "Anna", "panel.kick", marco, "", Outcome.OK);     // luca non c'entra
        List<StaffLogEntry> forLuca = log.about(luca, 10);
        assertEquals(List.of("panel.heal", "panel.ban"), forLuca.stream().map(StaffLogEntry::action).toList());
        assertEquals(2, log.about(marco, 10).size());
        assertTrue(log.about(UUID.randomUUID(), 10).isEmpty());
    }

    @Test
    void longDetailIsTruncated() {
        rec(marco, "Marco", "panel.kick", luca, "x".repeat(1000), Outcome.OK);
        assertEquals(256, log.recent(1).get(0).detail().length());
    }

    @Test
    void pruneRemovesOnlyOlderEntries() {
        clock.now = Instant.parse("2026-01-01T00:00:00Z");
        rec(marco, "Marco", "panel.heal", luca, "", Outcome.OK);
        clock.now = Instant.parse("2026-10-01T00:00:00Z");
        rec(marco, "Marco", "panel.kick", luca, "", Outcome.OK);
        int removed = log.pruneOlderThan(Instant.parse("2026-10-01T00:00:00Z").minus(Duration.ofDays(30)));
        assertEquals(1, removed);
        assertEquals(List.of("panel.kick"), log.recent(10).stream().map(StaffLogEntry::action).toList());
        assertEquals(0, log.pruneOlderThan(Instant.parse("2000-01-01T00:00:00Z")));
    }

    @Test
    void sequenceContinuesAfterRestart() {
        rec(marco, "Marco", "panel.heal", luca, "", Outcome.OK);
        long first = log.recent(1).get(0).id();
        DefaultStaffLogService again = new DefaultStaffLogService(db, clock);
        again.record(Optional.of(marco), "Marco", "panel.kick", Optional.empty(), Optional.empty(), "", Outcome.OK);
        assertTrue(again.recent(1).get(0).id() > first);
        assertEquals(2, again.recent(10).size());
    }
}
