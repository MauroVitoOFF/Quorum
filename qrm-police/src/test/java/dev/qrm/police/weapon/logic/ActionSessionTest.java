package dev.qrm.police.weapon.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.qrm.police.weapon.logic.SessionOutcome.Status;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActionSessionTest {
    private static final UUID ID = UUID.randomUUID();
    private static final String CHAR = "char-1";

    private static MagazineState mag(int rounds) {
        return new MagazineState(UUID.randomUUID(), rounds, 17, Set.of());
    }

    private static WeaponState w(MagazineState m, boolean chambered, boolean locked) {
        return new WeaponState(ID, Optional.ofNullable(m), chambered, locked);
    }

    private static ActionSession start(PistolAction a, WeaponState s, FakeInventory inv) {
        return ActionSession.start(a, s, inv, CHAR).orElseThrow();
    }

    /** Avanza fino al tick {@code upTo} incluso, verificando che la sessione resti in corso. */
    private static SessionOutcome run(ActionSession s, int fromTick, int upTo) {
        SessionOutcome last = null;
        for (int t = fromTick; t <= upTo; t++) {
            last = s.tick(true, false);
        }
        return last;
    }

    @Test
    void tacticalReloadComplete() {
        MagazineState old = mag(5);
        MagazineState ten = mag(10);
        MagazineState full = mag(17);
        FakeInventory inv = new FakeInventory(ten, full);
        ActionSession s = start(PistolAction.RELOAD_TACTICAL, w(old, true, false), inv);

        assertEquals(Status.RUNNING, run(s, 1, 15).status());
        assertTrue(s.state().magazine().isPresent());
        SessionOutcome o16 = s.tick(true, false);
        assertEquals(Status.RUNNING, o16.status());
        assertTrue(o16.state().magazine().isEmpty());
        assertTrue(inv.find(old.id()).isPresent());

        run(s, 17, 31);
        SessionOutcome o32 = s.tick(true, false);
        assertEquals(full.id(), o32.state().magazine().orElseThrow().id());
        assertTrue(inv.find(full.id()).isEmpty());
        assertTrue(inv.find(ten.id()).isPresent());

        assertEquals(Status.RUNNING, run(s, 33, 49).status());
        SessionOutcome o50 = s.tick(true, false);
        assertEquals(Status.DONE, o50.status());
        assertTrue(o50.state().chambered());
        assertEquals(17, o50.state().magazine().orElseThrow().rounds());
        assertThrows(IllegalStateException.class, () -> s.tick(true, false));
    }

    @Test
    void cancelledAtTick20LeavesPistolWithoutMagazine() {
        MagazineState old = mag(5);
        FakeInventory inv = new FakeInventory(mag(17));
        ActionSession s = start(PistolAction.RELOAD_TACTICAL, w(old, true, false), inv);
        run(s, 1, 19);
        SessionOutcome o = s.tick(false, false);
        assertEquals(Status.CANCELLED, o.status());
        assertTrue(o.state().magazine().isEmpty());
        assertTrue(o.state().chambered());
        assertTrue(inv.find(old.id()).isPresent());
        assertEquals(o.state(), s.state());
    }

    @Test
    void cancelledForOneTickNeverAppliesLaterCommits() {
        // Review Focus 2: cambio slot e ritorno immediato sulla pistola.
        MagazineState old = mag(5);
        FakeInventory inv = new FakeInventory(mag(17));
        ActionSession s = start(PistolAction.RELOAD_TACTICAL, w(old, true, false), inv);
        run(s, 1, 31);
        SessionOutcome o = s.tick(false, false); // tick 32: il commit MAG_IN non viene applicato
        assertEquals(Status.CANCELLED, o.status());
        assertTrue(o.state().magazine().isEmpty());
        assertEquals(1, inv.magazines.stream().filter(m -> m.rounds() == 17).count());
        assertThrows(IllegalStateException.class, () -> s.tick(true, false));
    }

    @Test
    void emptyReloadCancelledAfterMagIn() {
        MagazineState empty = mag(0);
        MagazineState full = mag(17);
        FakeInventory inv = new FakeInventory(full);
        ActionSession s = start(PistolAction.RELOAD_EMPTY, w(empty, false, true), inv);
        run(s, 1, 32);
        SessionOutcome o = s.tick(false, false);
        assertEquals(Status.CANCELLED, o.status());
        assertTrue(o.state().slideLocked());
        assertFalse(o.state().chambered());
        assertEquals(full.id(), o.state().magazine().orElseThrow().id());
        assertEquals(17, o.state().magazine().get().rounds());
    }

    @Test
    void emptyReloadCompleteChambersRound() {
        FakeInventory inv = new FakeInventory(mag(17));
        ActionSession s = start(PistolAction.RELOAD_EMPTY, w(mag(0), false, true), inv);
        SessionOutcome o = run(s, 1, 55);
        assertEquals(Status.DONE, o.status());
        assertTrue(o.state().chambered());
        assertFalse(o.state().slideLocked());
        assertEquals(16, o.state().magazine().orElseThrow().rounds());
    }

    @Test
    void incapacitatedCancelsLikeWeaponChange() {
        MagazineState old = mag(5);
        FakeInventory inv = new FakeInventory(mag(17));
        ActionSession s = start(PistolAction.RELOAD_TACTICAL, w(old, true, false), inv);
        run(s, 1, 19);
        SessionOutcome o = s.tick(true, true);
        assertEquals(Status.CANCELLED, o.status());
        assertTrue(o.state().magazine().isEmpty());
        assertTrue(o.state().chambered());
    }

    @Test
    void removedMagazineIsNotReinsertedEvenIfFullest() {
        // Review Focus 3.
        MagazineState old = mag(17);
        MagazineState three = mag(3);
        FakeInventory inv = new FakeInventory(three);
        ActionSession s = start(PistolAction.RELOAD_TACTICAL, w(old, false, false), inv);
        SessionOutcome o = run(s, 1, 50);
        assertEquals(Status.DONE, o.status());
        assertEquals(three.id(), o.state().magazine().orElseThrow().id());
        assertTrue(inv.find(old.id()).isPresent());
    }

    @Test
    void noLoadedSpareAtMagInCancelsAndLeavesPistolWithoutMagazine() {
        // Review Focus 1 e specifica §7: la scorta viene buttata a terra prima di mag_in, l'azione è annullata.
        MagazineState old = mag(5);
        MagazineState spare = mag(8);
        FakeInventory inv = new FakeInventory(spare);
        ActionSession s = start(PistolAction.RELOAD_TACTICAL, w(old, true, false), inv);
        int before = 5 + 8;
        run(s, 1, 20);
        inv.throwAway(spare);
        assertEquals(Status.RUNNING, run(s, 21, 31).status());
        SessionOutcome o = s.tick(true, false); // tick 32: MAG_IN non trova nulla
        assertEquals(Status.CANCELLED, o.status());
        assertTrue(o.state().magazine().isEmpty());
        assertTrue(o.state().chambered());
        assertEquals(before, inv.totalRounds());
        assertThrows(IllegalStateException.class, () -> s.tick(true, false));
    }

    @Test
    void emptyReloadWithoutSpareAtMagInCancelsBeforeSlideCommit() {
        // Il carrello resta indietro: il commit SLIDE del tick 42 non deve rilasciarlo su un'arma senza caricatore.
        MagazineState old = mag(0);
        MagazineState spare = mag(6);
        FakeInventory inv = new FakeInventory(spare);
        ActionSession s = start(PistolAction.RELOAD_EMPTY, w(old, false, true), inv);
        run(s, 1, 20);
        inv.throwAway(spare);
        run(s, 21, 31);
        SessionOutcome o = s.tick(true, false);
        assertEquals(Status.CANCELLED, o.status());
        assertTrue(o.state().magazine().isEmpty());
        assertTrue(o.state().slideLocked());
        assertFalse(o.state().chambered());
        assertEquals(6, inv.totalRounds());
    }

    @Test
    void fullInventoryDropsRemovedMagazine() {
        // Review Focus 5.
        MagazineState old = mag(5);
        MagazineState spare = mag(17);
        FakeInventory inv = new FakeInventory(1, spare);
        ActionSession s = start(PistolAction.RELOAD_TACTICAL, w(old, true, false), inv);
        run(s, 1, 16);
        assertEquals(1, inv.dropped.size());
        assertEquals(old.id(), inv.dropped.get(0).id());
        assertEquals(5, inv.dropped.get(0).rounds());
        SessionOutcome o = run(s, 17, 50);
        assertEquals(spare.id(), o.state().magazine().orElseThrow().id());
    }

    @Test
    void reloadNeedsLoadedSpareOtherThanInserted() {
        assertTrue(ActionSession.start(PistolAction.RELOAD_TACTICAL, w(mag(5), true, false),
                new FakeInventory(mag(0)), CHAR).isEmpty());
        assertTrue(ActionSession.start(PistolAction.RELOAD_EMPTY, w(mag(0), false, true),
                new FakeInventory(), CHAR).isEmpty());
        assertTrue(ActionSession.start(PistolAction.RELOAD_EMPTY, WeaponState.empty(ID),
                new FakeInventory(mag(3)), CHAR).isPresent());
    }

    @Test
    void slideActionsStartOnlyWhenAvailable() {
        FakeInventory inv = new FakeInventory();
        WeaponState locked = w(mag(5), false, true);
        WeaponState toRack = w(mag(5), false, false);
        assertTrue(ActionSession.start(PistolAction.SLIDE_RELEASE, locked, inv, CHAR).isPresent());
        assertTrue(ActionSession.start(PistolAction.SLIDE_RACK, locked, inv, CHAR).isEmpty());
        assertTrue(ActionSession.start(PistolAction.SLIDE_RACK, toRack, inv, CHAR).isPresent());
        assertTrue(ActionSession.start(PistolAction.SLIDE_RELEASE, toRack, inv, CHAR).isEmpty());

        SessionOutcome o = run(start(PistolAction.SLIDE_RACK, toRack, inv), 1, 10);
        assertEquals(Status.DONE, o.status());
        assertTrue(o.state().chambered());
        assertEquals(4, o.state().magazine().orElseThrow().rounds());
    }

    @Test
    void alwaysStartableActions() {
        FakeInventory inv = new FakeInventory();
        WeaponState empty = WeaponState.empty(ID);
        assertTrue(ActionSession.start(PistolAction.DRAW, empty, inv, CHAR).isPresent());
        assertTrue(ActionSession.start(PistolAction.INSPECT, empty, inv, CHAR).isPresent());
        assertTrue(ActionSession.start(PistolAction.AMMO_CHECK, empty, inv, CHAR).isPresent());
    }

    @Test
    void ammoCheckRevealsBandAndMarksMagazineKnown() {
        MagazineState m = mag(9);
        ActionSession s = start(PistolAction.AMMO_CHECK, w(m, true, false), new FakeInventory());
        SessionOutcome o11 = run(s, 1, 22);
        assertTrue(o11.revealedBand().isEmpty());
        assertFalse(o11.state().magazine().orElseThrow().isKnownBy(CHAR));
        SessionOutcome o12 = s.tick(true, false); // tick 23
        assertEquals(Optional.of(AmmoBand.MORE_THAN_HALF), o12.revealedBand());
        assertTrue(o12.state().magazine().orElseThrow().isKnownBy(CHAR));
        assertEquals(9, o12.state().magazine().get().rounds());
        SessionOutcome o20 = run(s, 24, 62);
        assertEquals(Status.DONE, o20.status());
        assertEquals(Optional.of(AmmoBand.MORE_THAN_HALF), o20.revealedBand());
    }

    @Test
    void ammoCheckWithoutMagazineRevealsNothing() {
        SessionOutcome o = run(start(PistolAction.AMMO_CHECK, WeaponState.empty(ID), new FakeInventory()), 1, 62);
        assertEquals(Status.DONE, o.status());
        assertTrue(o.revealedBand().isEmpty());
    }

@Test
    void drawDoesNotChangeState() {
        WeaponState st = w(mag(7), true, false);
        SessionOutcome o = run(start(PistolAction.DRAW, st, new FakeInventory()), 1, 16);
        assertEquals(Status.DONE, o.status());
        assertSame(st, o.state());
    }
}
