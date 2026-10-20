package dev.qrm.police.weapon.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PistolRulesTest {
    private static final UUID ID = UUID.randomUUID();

    private static MagazineState mag(int rounds) {
        return new MagazineState(UUID.randomUUID(), rounds, 17, Set.of());
    }

    private static WeaponState w(MagazineState m, boolean chambered, boolean locked) {
        return new WeaponState(ID, Optional.ofNullable(m), chambered, locked);
    }

    @Test
    void fireWithRoundsInMagazine() {
        FireResult r = PistolRules.fire(w(mag(5), true, false));
        assertTrue(r.fired());
        assertEquals(4, r.state().magazine().get().rounds());
        assertTrue(r.state().chambered());
        assertFalse(r.state().slideLocked());
    }

    @Test
    void fireLastRoundLocksSlide() {
        FireResult r = PistolRules.fire(w(mag(0), true, false));
        assertTrue(r.fired());
        assertFalse(r.state().chambered());
        assertTrue(r.state().slideLocked());
    }

    @Test
    void fireWithoutMagazineKeepsSlideClosed() {
        FireResult r = PistolRules.fire(w(null, true, false));
        assertTrue(r.fired());
        assertFalse(r.state().chambered());
        assertFalse(r.state().slideLocked());
    }

    @Test
    void fireWithEmptyChamberDoesNothing() {
        WeaponState s = w(mag(5), false, false);
        FireResult r = PistolRules.fire(s);
        assertFalse(r.fired());
        assertEquals(s, r.state());
    }

    @Test
    void releaseSlideLoadsRound() {
        WeaponState s = w(mag(17), false, true);
        assertEquals(SlideAction.RELEASE, PistolRules.slideActionFor(s));
        WeaponState n = PistolRules.applySlide(s);
        assertEquals(16, n.magazine().get().rounds());
        assertTrue(n.chambered());
        assertFalse(n.slideLocked());
    }

    @Test
    void rackSlideLoadsRound() {
        WeaponState s = w(mag(3), false, false);
        assertEquals(SlideAction.RACK, PistolRules.slideActionFor(s));
        WeaponState n = PistolRules.applySlide(s);
        assertEquals(2, n.magazine().get().rounds());
        assertTrue(n.chambered());
    }

    @Test
    void chamberedMeansNoSlideAction() {
        WeaponState s = w(mag(3), true, false);
        assertEquals(SlideAction.NONE, PistolRules.slideActionFor(s));
        assertSame(s, PistolRules.applySlide(s));
    }

    @Test
    void releaseWithoutMagazineClosesSlide() {
        WeaponState s = w(null, false, true);
        assertEquals(SlideAction.RELEASE, PistolRules.slideActionFor(s));
        WeaponState n = PistolRules.applySlide(s);
        assertFalse(n.slideLocked());
        assertFalse(n.chambered());
    }

    @Test
    void hints() {
        assertEquals(Hint.RELEASE_SLIDE, PistolRules.hint(w(mag(2), false, true), false));
        assertEquals(Hint.RACK_SLIDE, PistolRules.hint(w(mag(2), false, false), false));
        assertEquals(Hint.RELOAD, PistolRules.hint(w(null, false, false), true));
        assertEquals(Hint.RELOAD, PistolRules.hint(w(mag(0), false, true), true));
        assertEquals(Hint.NO_SPARE, PistolRules.hint(w(null, false, false), false));
        assertEquals(Hint.NO_SPARE, PistolRules.hint(w(mag(0), false, false), false));
        assertEquals(Hint.NONE, PistolRules.hint(w(mag(2), true, false), true));
    }

    @Test
    void magOutAndIn() {
        MagazineState m = mag(4);
        WeaponState s = w(m, true, false);
        WeaponState out = PistolRules.magOut(s);
        assertTrue(out.magazine().isEmpty());
        assertTrue(out.chambered());
        assertSame(out, PistolRules.magOut(out));
        MagazineState other = mag(9);
        assertEquals(Optional.of(other), PistolRules.magIn(out, other).magazine());
        assertThrows(IllegalStateException.class, () -> PistolRules.magIn(s, other));
    }
}
