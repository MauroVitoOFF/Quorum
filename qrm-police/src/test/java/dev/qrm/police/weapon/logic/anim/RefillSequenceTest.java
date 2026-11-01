package dev.qrm.police.weapon.logic.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.qrm.police.weapon.logic.anim.RefillSequence.Step;
import org.junit.jupiter.api.Test;

class RefillSequenceTest {
    private static final double LEN = 1.0;

    @Test
    void laPrimaRichiestaAvviaLaPreparazione() {
        RefillSequence s = new RefillSequence();
        assertFalse(s.active());
        assertEquals(RefillSequence.IN, s.request(3, false));
        assertTrue(s.active());
        // un'altra richiesta durante la preparazione si accoda, senza riavviare
        assertNull(s.request(2, false));
        assertEquals(5, s.pending());
    }

    @Test
    void unGiroPerColpoPoiLaChiusuraPoiFine() {
        RefillSequence s = new RefillSequence();
        s.request(2, false);
        assertEquals(Step.NONE, s.tick(RefillSequence.IN, 0.5, LEN));
        assertEquals(Step.PLAY_LOOP, s.tick(RefillSequence.IN, LEN, LEN)); // primo colpo
        assertEquals(1, s.pending());
        assertEquals(Step.NONE, s.tick(RefillSequence.LOOP, 0.3, LEN));
        assertEquals(Step.PLAY_LOOP, s.tick(RefillSequence.LOOP, LEN, LEN)); // secondo colpo
        assertEquals(0, s.pending());
        assertEquals(Step.PLAY_OUT, s.tick(RefillSequence.LOOP, LEN, LEN));
        assertEquals(Step.STOP, s.tick(RefillSequence.OUT, LEN, LEN));
        assertFalse(s.active());
    }

    @Test
    void ilPacchettoDelServerNonContaDueVolteLaPrevisione() {
        RefillSequence s = new RefillSequence();
        assertEquals(RefillSequence.IN, s.request(5, true)); // previsione del client
        assertNull(s.request(5, false)); // conferma del server: gia' contata
        assertEquals(5, s.pending());
        // il server ne sposta di piu' di quanto previsto: l'eccedenza si aggiunge
        s.request(3, true);
        s.request(4, false);
        assertEquals(9, s.pending());
        // il server ne sposta meno: la previsione in eccesso resta (non puo' toglierla), ma non si somma altro
        RefillSequence t = new RefillSequence();
        t.request(5, true);
        t.request(3, false);
        assertEquals(5, t.pending());
    }

    @Test
    void nuoviColpiDuranteLaChiusuraRiprendonoIlCiclo() {
        RefillSequence s = new RefillSequence();
        s.request(1, false);
        s.tick(RefillSequence.IN, LEN, LEN); // giro 1
        s.tick(RefillSequence.LOOP, LEN, LEN); // chiusura
        assertEquals(RefillSequence.LOOP, s.request(2, false));
        assertEquals(1, s.pending());
        assertTrue(s.active());
    }

    @Test
    void ilServerChiudeFinitoIlGiroInCorso() {
        RefillSequence s = new RefillSequence();
        s.request(10, false);
        s.tick(RefillSequence.IN, LEN, LEN); // giro 1, 9 in coda
        s.finish();
        assertEquals(0, s.pending());
        assertEquals(Step.NONE, s.tick(RefillSequence.LOOP, 0.4, LEN));
        assertEquals(Step.PLAY_OUT, s.tick(RefillSequence.LOOP, LEN, LEN));
        assertEquals(Step.STOP, s.tick(RefillSequence.OUT, LEN, LEN));
    }

    @Test
    void laGuiNonInterrompeNulla() {
        // la sequenza non conosce la GUI: con colpi in coda continua fino a esaurirli, aperta o chiusa che sia
        RefillSequence s = new RefillSequence();
        s.request(2, false);
        assertEquals(Step.PLAY_LOOP, s.tick(RefillSequence.IN, LEN, LEN));
        assertEquals(1, s.pending());
        assertEquals(Step.PLAY_LOOP, s.tick(RefillSequence.LOOP, LEN, LEN));
        assertEquals(Step.PLAY_OUT, s.tick(RefillSequence.LOOP, LEN, LEN));
    }

    @Test
    void unAltraAzionePrendeIlPostoESiAzzera() {
        RefillSequence s = new RefillSequence();
        s.request(4, true);
        assertEquals(Step.NONE, s.tick("fire", 0.1, LEN));
        assertFalse(s.active());
        assertEquals(0, s.pending());
        // senza clip in corso (l'azione è finita altrove) lo stesso
        s.request(4, true);
        assertEquals(Step.NONE, s.tick(null, 0, 0));
        assertFalse(s.active());
    }

    @Test
    void inCodaNonSiSuperaIlMassimo() {
        RefillSequence s = new RefillSequence();
        s.request(1000, false);
        assertEquals(RefillSequence.MAX_PENDING, s.pending());
    }

    @Test
    void richiesteVuoteNonFannoPartireNulla() {
        RefillSequence s = new RefillSequence();
        assertNull(s.request(0, false));
        assertNull(s.request(0, true));
        assertFalse(s.active());
    }
}
