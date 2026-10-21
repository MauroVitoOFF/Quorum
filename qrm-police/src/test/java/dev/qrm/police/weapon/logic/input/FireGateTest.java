package dev.qrm.police.weapon.logic.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.qrm.police.weapon.logic.MagazineState;
import dev.qrm.police.weapon.logic.PistolAction;
import dev.qrm.police.weapon.logic.WeaponState;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FireGateTest {
    private static final UUID GUN = UUID.randomUUID();
    private static final UUID MAG = UUID.randomUUID();

    private static WeaponState loaded(int rounds) {
        return new WeaponState(GUN, Optional.of(new MagazineState(MAG, rounds, 17, Set.of())), true, false);
    }

    @Test
    void primoColpoParteEPoiRispettaICinqueTick() {
        FireGate gate = new FireGate();
        assertEquals(FireGate.Outcome.FIRE, gate.pull(loaded(5), 100).outcome());
        assertEquals(FireGate.Outcome.BLOCKED, gate.pull(loaded(4), 104).outcome());
        assertEquals(FireGate.Outcome.FIRE, gate.pull(loaded(4), 105).outcome());
    }

    @Test
    void ultimoColpoBloccaIlCarrello() {
        FireGate gate = new FireGate();
        FireGate.Decision d = gate.pull(loaded(0), 10);
        assertEquals(FireGate.Outcome.FIRE_LAST, d.outcome());
        assertEquals("fire_last", d.outcome().clip());
    }

    @Test
    void senzaColpoInCannaEUnClicAVuoto() {
        FireGate gate = new FireGate();
        FireGate.Decision d = gate.pull(WeaponState.empty(GUN), 10);
        assertEquals(FireGate.Outcome.DRY, d.outcome());
        assertEquals("dry_fire", d.outcome().clip());
        // Anche il clic a vuoto rispetta l'intervallo: niente raffica di clip.
        assertEquals(FireGate.Outcome.BLOCKED, gate.pull(WeaponState.empty(GUN), 12).outcome());
    }

    @Test
    void azioneCheBloccaLoSparoRifiutaInLocale() {
        FireGate gate = new FireGate();
        gate.actionStarted(PistolAction.RELOAD_TACTICAL, 0);
        assertEquals(FireGate.Outcome.BLOCKED, gate.pull(loaded(5), 10).outcome());
        gate.actionEnded();
        assertEquals(FireGate.Outcome.FIRE, gate.pull(loaded(5), 11).outcome());
    }

    @Test
    void azioneCheNonBloccaVieneInterrottaDalloSparo() {
        FireGate gate = new FireGate();
        gate.actionStarted(PistolAction.INSPECT, 0);
        assertEquals(FireGate.Outcome.FIRE, gate.pull(loaded(5), 10).outcome());
        assertNull(gate.running(10));
    }

    @Test
    void azioneSenzaFineScadeDopoLaDurataPiuMargine() {
        FireGate gate = new FireGate();
        gate.actionStarted(PistolAction.RELOAD_EMPTY, 0);
        long expiry = PistolAction.RELOAD_EMPTY.durationTicks() + FireGate.ACTION_GRACE_TICKS;
        assertEquals(PistolAction.RELOAD_EMPTY, gate.running(expiry - 1));
        assertNull(gate.running(expiry));
    }

    @Test
    void laPrevisioneSiAppoggiaAlColpoPrecedenteFinoAlloStatoDelServer() {
        FireGate gate = new FireGate();
        // Un colpo nel caricatore: il primo sparo lo camera, il secondo blocca il carrello.
        WeaponState server = loaded(1);
        assertEquals(FireGate.Outcome.FIRE, gate.pull(server, 0).outcome());
        // Lo stato del server non è ancora arrivato: lo stack è lo stesso.
        assertEquals(FireGate.Outcome.FIRE_LAST, gate.pull(server, 5).outcome());
        // Terzo colpo, sempre senza aggiornamento: camera vuota, clic a vuoto.
        assertEquals(FireGate.Outcome.DRY, gate.pull(server, 10).outcome());
    }

    @Test
    void unoStatoNuovoDelServerVinceSullaPrevisione() {
        FireGate gate = new FireGate();
        gate.pull(loaded(1), 0);
        // Il server ha ricaricato nel frattempo: si parte dal suo stato.
        assertEquals(FireGate.Outcome.FIRE, gate.pull(loaded(10), 5).outcome());
    }

    @Test
    void ilRifiutoCancellaLaPrevisione() {
        FireGate gate = new FireGate();
        WeaponState server = loaded(1);
        gate.pull(server, 0);
        gate.rejected();
        // Il colpo non è partito sul server: lo stack resta la verità.
        assertEquals(FireGate.Outcome.FIRE, gate.pull(server, 5).outcome());
    }

    @Test
    void laPrevisioneScade() {
        FireGate gate = new FireGate();
        WeaponState server = loaded(1);
        gate.pull(server, 0);
        assertEquals(FireGate.Outcome.FIRE, gate.pull(server, FireGate.PREDICTION_TTL_TICKS + 1).outcome());
    }

    @Test
    void resetDimenticaTutto() {
        FireGate gate = new FireGate();
        gate.actionStarted(PistolAction.DRAW, 0);
        gate.pull(loaded(3), 0);
        gate.reset();
        assertNull(gate.running(1));
        assertEquals(FireGate.Outcome.FIRE, gate.pull(loaded(3), 1).outcome());
    }
}
