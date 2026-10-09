package dev.qrm.neoforge.server;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.staff.StaffLogEntry;
import dev.qrm.api.staff.StaffLogEntry.Outcome;
import dev.qrm.neoforge.net.PlayerInfoLine;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LogLinesTest {
    private static StaffLogEntry entry(Optional<UUID> staff, String staffName, String action, Optional<String> target,
            String detail, Outcome outcome) {
        return new StaffLogEntry(1, Instant.parse("2026-10-09T14:32:00Z"), staff, staffName, action,
                target.map(t -> UUID.randomUUID()), target, detail, outcome);
    }

    @Test
    void lineHasLiteralLabelWithDateAndStaffAndValueWithActionTargetDetail() {
        List<PlayerInfoLine> lines = LogLines.build(List.of(entry(Optional.of(UUID.randomUUID()), "Marco", "panel.ban",
                Optional.of("Luca"), "7d | spam", Outcome.OK)), ZoneOffset.UTC);
        assertEquals(1, lines.size());
        assertEquals("=09/10 14:32 Marco", lines.get(0).labelKey());
        assertEquals("panel.ban → Luca · 7d | spam", lines.get(0).value());
    }

    @Test
    void deniedAttemptsAreMarked() {
        List<PlayerInfoLine> lines = LogLines.build(List.of(entry(Optional.of(UUID.randomUUID()), "Anna", "panel.kick",
                Optional.of("Luca"), "", Outcome.DENIED)), ZoneOffset.UTC);
        assertEquals("✗ panel.kick → Luca", lines.get(0).value());
    }

    @Test
    void consoleAndMissingTargetAndDetail() {
        List<PlayerInfoLine> lines = LogLines.build(List.of(entry(Optional.empty(), "Console", "cmd.log.prune",
                Optional.empty(), "", Outcome.OK)), ZoneOffset.UTC);
        assertEquals("=09/10 14:32 Console", lines.get(0).labelKey());
        assertEquals("cmd.log.prune", lines.get(0).value());
    }

    @Test
    void timeFollowsTheGivenZone() {
        List<PlayerInfoLine> lines = LogLines.build(List.of(entry(Optional.empty(), "Console", "cmd.x", Optional.empty(), "",
                Outcome.OK)), ZoneOffset.ofHours(2));
        assertTrue(lines.get(0).labelKey().startsWith("=09/10 16:32"));
    }

    @Test
    void emptyLogShowsAPlaceholderLine() {
        List<PlayerInfoLine> lines = LogLines.build(List.of(), ZoneOffset.UTC);
        assertEquals(List.of(new PlayerInfoLine("qrm.staff.log.empty", "")), lines);
    }

    @Test
    void textHelpers() {
        assertEquals("qrm admin grant Marco 5", StaffAuditText.detailOf("/qrm admin grant Marco 5"));
        assertEquals("job set a b c", StaffAuditText.detailOf("  job set a b c "));
        assertEquals("", StaffAuditText.detailOf(null));
        assertEquals(StaffAuditText.MAX_DETAIL, StaffAuditText.detailOf("x".repeat(1000)).length());
        assertEquals("7d | spam", StaffAuditText.panelDetail("7d\nspam"));
        assertEquals("| spam", StaffAuditText.panelDetail("\nspam"));
        assertEquals("", StaffAuditText.panelDetail(""));
        assertEquals("diamond 16", StaffAuditText.panelDetail("diamond 16"));
    }
}
