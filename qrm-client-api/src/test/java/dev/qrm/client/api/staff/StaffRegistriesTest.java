package dev.qrm.client.api.staff;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaffRegistriesTest {
    @BeforeEach
    @AfterEach
    void clear() {
        StaffEntries.clearForTests();
        StaffPlayerActions.clearForTests();
    }

    private static StaffEntries.Entry entry(String id, String node) {
        return new StaffEntries.Entry(id, StaffEntries.Section.ECONOMY, "k." + id, node, false, () -> true, c -> {});
    }

    @Test
    void entriesKeepRegistrationOrderAndRejectDuplicates() {
        StaffEntries.register(entry("bank", "staff.bank"));
        StaffEntries.register(entry("audit", "staff.audit"));
        assertEquals(List.of("bank", "audit"), StaffEntries.all().stream().map(StaffEntries.Entry::id).toList());
        assertThrows(IllegalStateException.class, () -> StaffEntries.register(entry("bank", "staff.other")));
    }

    @Test
    void entriesValidateIdNodeAndSection() {
        assertThrows(IllegalArgumentException.class, () -> entry("Bank", "staff.bank"));
        assertThrows(IllegalArgumentException.class, () -> entry("1bank", "staff.bank"));
        assertThrows(IllegalArgumentException.class, () -> entry("a".repeat(33), "staff.bank"));
        assertThrows(IllegalArgumentException.class, () -> entry("bank", "staff.*"));
        assertThrows(IllegalArgumentException.class, () -> entry("bank", "staff..bank"));
        assertThrows(IllegalArgumentException.class, () -> new StaffEntries.Entry("p", StaffEntries.Section.PLAYERS, "k",
                "staff.p", false, () -> true, c -> {}));
    }

    @Test
    void sectionLabelKeysFollowTheirIds() {
        for (StaffEntries.Section s : StaffEntries.Section.values()) {
            assertEquals("qrm.staff.section." + s.id(), s.labelKey());
        }
    }

    @Test
    void plannedActionHasNoExecution() {
        StaffPlayerActions.Action a = StaffPlayerActions.Action.planned("kick", "k.kick", "staff.player.kick", true);
        assertTrue(a.run().isEmpty());
        assertTrue(a.danger());
        assertTrue(a.choices().isEmpty());
        assertTrue(a.fields().isEmpty());
    }

    @Test
    void actionShapesAreMutuallyExclusive() {
        var choice = new StaffPlayerActions.Choice("1h", "k.1h");
        var field = new StaffPlayerActions.Field("k.reason", "k.hint", 64, true);
        assertThrows(IllegalArgumentException.class,
                () -> StaffPlayerActions.Action.withChoices("a", "k", "staff.x", false, List.of(), c -> {}));
        assertThrows(IllegalArgumentException.class,
                () -> StaffPlayerActions.Action.withInput("a", "k", "staff.x", false, List.of(), c -> {}));
        assertThrows(IllegalArgumentException.class, () -> new StaffPlayerActions.Action("a", "k", "staff.x", false,
                Optional.empty(), List.of(choice), List.of(field)));
        assertThrows(IllegalArgumentException.class, () -> new StaffPlayerActions.Field("l", "h", 0, true));
        assertThrows(IllegalArgumentException.class, () -> new StaffPlayerActions.Field("l", "h", 257, true));
        assertEquals(List.of(choice),
                StaffPlayerActions.Action.withChoices("a", "k", "staff.x", false, List.of(choice), c -> {}).choices());
    }

    @Test
    void actionsRejectDuplicatesAndBadIds() {
        StaffPlayerActions.register(StaffPlayerActions.Action.planned("kick", "k", "staff.player.kick", true));
        assertThrows(IllegalStateException.class,
                () -> StaffPlayerActions.register(StaffPlayerActions.Action.planned("kick", "k", "staff.player.kick", true)));
        assertThrows(IllegalArgumentException.class,
                () -> StaffPlayerActions.Action.planned("Kick", "k", "staff.player.kick", true));
        assertEquals(1, StaffPlayerActions.all().size());
    }
}
