package dev.qrm.client.api.staff;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaffEntriesTest {
    @BeforeEach
    void clear() {
        StaffEntries.clearForTests();
    }

    private static StaffEntries.Entry entry(String id, StaffEntries.Section section, String node) {
        return new StaffEntries.Entry(id, section, "k." + id, node, false, () -> true, ctx -> {});
    }

    @Test
    void entriesKeepRegistrationOrder() {
        StaffEntries.register(entry("jobs", StaffEntries.Section.PLAYER_MANAGEMENT, "staff.jobs"));
        StaffEntries.register(entry("orgs", StaffEntries.Section.ORGANIZATIONS, "staff.orgs"));
        assertEquals(List.of("jobs", "orgs"), StaffEntries.all().stream().map(StaffEntries.Entry::id).toList());
    }

    @Test
    void duplicateIdIsRejected() {
        StaffEntries.register(entry("jobs", StaffEntries.Section.PLAYER_MANAGEMENT, "staff.jobs"));
        assertThrows(IllegalStateException.class,
                () -> StaffEntries.register(entry("jobs", StaffEntries.Section.MODERATION, "staff.jobs")));
    }

    @Test
    void invalidIdOrNodeIsRejected() {
        for (String id : new String[] {"", "Bank", "1bank", "a b", "a".repeat(33)}) {
            assertThrows(IllegalArgumentException.class,
                    () -> entry(id, StaffEntries.Section.SERVER_MANAGEMENT, "staff.x"), id);
        }
        for (String node : new String[] {"", "Staff.x", "staff.*", "a b"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> entry("ok", StaffEntries.Section.SERVER_MANAGEMENT, node), node);
        }
    }

    @Test
    void playersSectionHasNoEntries() {
        assertThrows(IllegalArgumentException.class, () -> entry("x", StaffEntries.Section.PLAYERS, "staff.x"));
    }

    @Test
    void sectionOrderAndLabelKeys() {
        assertEquals(List.of("players", "player_management", "server_management", "moderation", "economy",
                "organizations", "logs", "quick_actions"),
                java.util.Arrays.stream(StaffEntries.Section.values()).map(StaffEntries.Section::id).toList());
        assertEquals("qrm.staff.section.economy", StaffEntries.Section.ECONOMY.labelKey());
    }
}
