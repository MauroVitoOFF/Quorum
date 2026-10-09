package dev.qrm.client.api.staff;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaffPlayerActionsTest {
    @BeforeEach
    void clear() {
        StaffPlayerActions.clearForTests();
    }

    @Test
    void registrationKeepsOrder() {
        StaffPlayerActions.register(StaffPlayerActions.Action.planned("kick", "k.kick", "staff.player.kick", true));
        StaffPlayerActions.register(StaffPlayerActions.Action.of("view", "k.view", "staff.player.view", false, ctx -> {}));
        assertEquals(List.of("kick", "view"), StaffPlayerActions.all().stream().map(StaffPlayerActions.Action::id).toList());
    }

    @Test
    void plannedHasNoRunAndOfHasOne() {
        assertTrue(StaffPlayerActions.Action.planned("a", "k", "staff.player.a", false).run().isEmpty());
        assertTrue(StaffPlayerActions.Action.of("a", "k", "staff.player.a", false, ctx -> {}).run().isPresent());
    }

    @Test
    void duplicateIdIsRejected() {
        StaffPlayerActions.register(StaffPlayerActions.Action.planned("kick", "k", "staff.player.kick", true));
        assertThrows(IllegalStateException.class, () -> StaffPlayerActions.register(
                StaffPlayerActions.Action.planned("kick", "k", "staff.player.kick", true)));
    }

    @Test
    void invalidIdOrNodeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> StaffPlayerActions.Action.planned("Kick", "k", "staff.player.x", false));
        assertThrows(IllegalArgumentException.class,
                () -> StaffPlayerActions.Action.planned("kick", "k", "staff.*", false));
    }

    @Test
    void formsOfActionAreExclusive() {
        StaffPlayerActions.Action choices = StaffPlayerActions.Action.withChoices("gm", "k", "staff.player.gm", false,
                List.of(new StaffPlayerActions.Choice("a", "k.a")), c -> {});
        assertEquals(1, choices.choices().size());
        assertTrue(choices.fields().isEmpty());
        StaffPlayerActions.Action input = StaffPlayerActions.Action.withInput("ban", "k", "staff.player.ban", true,
                List.of(new StaffPlayerActions.Field("k.f", "k.h", 32, true)), c -> {});
        assertEquals(1, input.fields().size());
        assertThrows(IllegalArgumentException.class, () -> new StaffPlayerActions.Action("x", "k", "staff.player.x", false,
                java.util.Optional.of(c -> {}), List.of(new StaffPlayerActions.Choice("a", "k")),
                List.of(new StaffPlayerActions.Field("k", "h", 8, false))));
        assertThrows(IllegalArgumentException.class, () -> StaffPlayerActions.Action.withChoices("x", "k", "staff.player.x",
                false, List.of(), c -> {}));
        assertThrows(IllegalArgumentException.class, () -> new StaffPlayerActions.Field("k", "h", 0, false));
    }
}
