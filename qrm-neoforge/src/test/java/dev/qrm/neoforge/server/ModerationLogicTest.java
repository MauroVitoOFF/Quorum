package dev.qrm.neoforge.server;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.moderation.Mute;
import dev.qrm.api.moderation.StaffNote;
import dev.qrm.api.moderation.Warning;
import dev.qrm.neoforge.net.PlayerInfoLine;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModerationLogicTest {
    private static final Instant AT = Instant.parse("2026-10-09T14:32:00Z");
    private final UUID luca = UUID.randomUUID();

    private Mute mute(Optional<Instant> expires) {
        return new Mute(luca, "Luca", Optional.of(UUID.randomUUID()), "Marco", "spam", AT, expires);
    }

    // ---- MuteCache ----

    @Test
    void cacheReturnsActiveMuteAndDropsExpiredOnes() {
        MuteCache cache = new MuteCache();
        Mute timed = mute(Optional.of(AT.plusSeconds(60)));
        cache.put(timed);
        assertEquals(Optional.of(timed), cache.active(luca, AT));
        assertTrue(cache.active(luca, AT.plusSeconds(61)).isEmpty());
        assertTrue(cache.isEmpty());                                      // il mute scaduto è stato tolto
    }

    @Test
    void permanentMuteNeverExpiresInTheCache() {
        MuteCache cache = new MuteCache();
        cache.put(mute(Optional.empty()));
        assertTrue(cache.active(luca, AT.plusSeconds(10_000_000)).isPresent());
        cache.remove(luca);
        assertTrue(cache.active(luca, AT).isEmpty());
    }

    @Test
    void cachePutReplacesThePreviousMute() {
        MuteCache cache = new MuteCache();
        cache.put(mute(Optional.of(AT.plusSeconds(60))));
        Mute permanent = mute(Optional.empty());
        cache.put(permanent);
        assertEquals(Optional.of(permanent), cache.active(luca, AT.plusSeconds(120)));
    }

    @Test
    void refreshKeepsOnlyMutesOfOnlinePlayers() {
        MuteCache cache = new MuteCache();
        cache.refresh(luca, Optional.of(mute(Optional.empty())), false);
        assertTrue(cache.isEmpty());                                      // offline: senza logout non verrebbe mai tolto
        cache.refresh(luca, Optional.of(mute(Optional.empty())), true);
        assertTrue(cache.active(luca, AT).isPresent());
        cache.refresh(luca, Optional.empty(), true);                      // unmute
        assertTrue(cache.isEmpty());
    }

    // ---- MutedCommands ----

    @Test
    void messagingCommandsAreBlockedWithOrWithoutSlashAndNamespace() {
        for (String c : new String[] {"msg Anna ciao", "/tell Anna ciao", "w Anna x", "/me balla", "teammsg hi", "tm hi", "say ciao",
                "/MSG Anna x", "minecraft:msg Anna x", "/minecraft:me balla", "  /say  x"}) {
            assertTrue(MutedCommands.isBlocked(c), c);
        }
    }

    @Test
    void otherCommandsAreNotBlocked() {
        for (String c : new String[] {"gamemode creative", "/tp 0 0 0", "balance", "msgx a", "/team list", "", "   ", "/"}) {
            assertFalse(MutedCommands.isBlocked(c), c);
        }
        assertFalse(MutedCommands.isBlocked(null));
    }

    // ---- MuteNotice ----

    @Test
    void noticesDescribeTheMute() {
        Mute timed = mute(Optional.of(Instant.parse("2026-10-10T08:00:00Z")));
        assertEquals("fino al 10/10 08:00", MuteNotice.until(timed, ZoneOffset.UTC));
        assertEquals("permanente", MuteNotice.until(mute(Optional.empty()), ZoneOffset.UTC));
        assertEquals("Sei stato silenziato (fino al 10/10 08:00). Motivo: spam", MuteNotice.applied(timed, ZoneOffset.UTC));
        assertEquals("Sei silenziato (permanente). Motivo: spam", MuteNotice.blocked(mute(Optional.empty()), ZoneOffset.UTC));
        assertEquals("Avvertimento dallo staff (2): insulti", MuteNotice.warned(2, "insulti"));
    }

    // ---- ModerationLines ----

    @Test
    void warningsOfPlayerShowDateAndStaffThenReason() {
        List<PlayerInfoLine> lines = ModerationLines.warningsOf(
                List.of(new Warning(1, luca, "Luca", Optional.empty(), "Marco", "insulti", AT)), ZoneOffset.UTC);
        assertEquals(new PlayerInfoLine("=09/10 14:32 Marco", "insulti"), lines.get(0));
    }

    @Test
    void recentWarningsShowDateAndPlayerThenStaffAndReason() {
        List<PlayerInfoLine> lines = ModerationLines.recentWarnings(
                List.of(new Warning(1, luca, "Luca", Optional.empty(), "Marco", "insulti", AT)), ZoneOffset.UTC);
        assertEquals(new PlayerInfoLine("=09/10 14:32 Luca", "Marco: insulti"), lines.get(0));
    }

    @Test
    void notesShowDateAndStaffThenText() {
        List<PlayerInfoLine> lines = ModerationLines.notes(
                List.of(new StaffNote(1, luca, "Luca", Optional.empty(), "Anna", "sospetto xray", AT)), ZoneOffset.UTC);
        assertEquals(new PlayerInfoLine("=09/10 14:32 Anna", "sospetto xray"), lines.get(0));
    }

    @Test
    void mutesShowPlayerThenExpiryReasonAndStaff() {
        List<PlayerInfoLine> lines = ModerationLines.mutes(
                List.of(mute(Optional.empty()), mute(Optional.of(Instant.parse("2026-10-10T08:00:00Z")))), ZoneOffset.UTC);
        assertEquals(new PlayerInfoLine("=Luca", "permanente · spam (Marco)"), lines.get(0));
        assertEquals("fino al 10/10 08:00 · spam (Marco)", lines.get(1).value());
    }

    @Test
    void emptyListsGivePlaceholderLines() {
        assertEquals("qrm.staff.moderation.empty_warnings", ModerationLines.warningsOf(List.of(), ZoneOffset.UTC).get(0).labelKey());
        assertEquals("qrm.staff.moderation.empty_warnings", ModerationLines.recentWarnings(List.of(), ZoneOffset.UTC).get(0).labelKey());
        assertEquals("qrm.staff.moderation.empty_notes", ModerationLines.notes(List.of(), ZoneOffset.UTC).get(0).labelKey());
        assertEquals("qrm.staff.moderation.empty_mutes", ModerationLines.mutes(List.of(), ZoneOffset.UTC).get(0).labelKey());
        assertFalse(ModerationLines.notes(List.of(), ZoneOffset.UTC).get(0).literal());
    }
}
