package dev.qrm.police.weapon.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.qrm.police.weapon.logic.PistolIntents.Admission;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PistolIntentsTest {
    private static final UUID ID = UUID.randomUUID();

    private static WeaponState state(int rounds, boolean chambered, boolean locked) {
        return new WeaponState(ID, Optional.of(new MagazineState(UUID.randomUUID(), rounds, 17, java.util.Set.of())),
                chambered, locked);
    }

    @Test
    void reloadIsTacticalOnlyWithRoundChambered() {
        assertEquals(PistolAction.RELOAD_TACTICAL, PistolIntents.reloadFor(state(5, true, false)));
        assertEquals(PistolAction.RELOAD_EMPTY, PistolIntents.reloadFor(state(0, false, true)));
        assertEquals(PistolAction.RELOAD_EMPTY, PistolIntents.reloadFor(WeaponState.empty(ID)));
    }

    @Test
    void tapPrefersSlideOverAmmoCheck() {
        assertEquals(PistolAction.SLIDE_RELEASE, PistolIntents.tapFor(state(10, false, true)));
        assertEquals(PistolAction.SLIDE_RACK, PistolIntents.tapFor(state(10, false, false)));
        assertEquals(PistolAction.AMMO_CHECK, PistolIntents.tapFor(state(10, true, false)));
        assertEquals(PistolAction.AMMO_CHECK, PistolIntents.tapFor(WeaponState.empty(ID)));
    }

    @Test
    void blockingSessionsRefuseNonBlockingArePreempted() {
        assertEquals(Admission.START, PistolIntents.admit(null));
        for (PistolAction a : PistolAction.values()) {
            assertEquals(a.blocksFire() ? Admission.REFUSE : Admission.PREEMPT_AND_START, PistolIntents.admit(a), a.name());
        }
    }

    @Test
    void miraRifiutataAterraOSenzaPistola() {
        assertTrue(PistolIntents.allowsAim(true, false));
        assertFalse(PistolIntents.allowsAim(true, true));
        assertFalse(PistolIntents.allowsAim(false, false));
        assertFalse(PistolIntents.allowsAim(false, true));
    }
}
