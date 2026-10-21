package dev.qrm.police.weapon.logic.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.qrm.police.weapon.logic.PistolTimings;
import org.junit.jupiter.api.Test;

class PressClassifierTest {
    private static final int T = PistolTimings.HOLD_THRESHOLD_TICKS;

    @Test
    void rilascioPrimaDellaSogliaEUnaPressioneBreve() {
        PressClassifier c = new PressClassifier(T);
        for (int i = 0; i < T - 1; i++) assertEquals(PressClassifier.Press.NONE, c.tick(true, i == 0));
        assertEquals(PressClassifier.Press.TAP, c.tick(false, false));
        assertEquals(PressClassifier.Press.NONE, c.tick(false, false));
    }

    @Test
    void tenutoFinoAllaSogliaMandaUnaSolaTenuta() {
        PressClassifier c = new PressClassifier(T);
        for (int i = 0; i < T - 1; i++) assertEquals(PressClassifier.Press.NONE, c.tick(true, false));
        assertEquals(PressClassifier.Press.HOLD, c.tick(true, false));
        for (int i = 0; i < 10; i++) assertEquals(PressClassifier.Press.NONE, c.tick(true, false));
        // Il rilascio dopo la tenuta non manda una pressione breve.
        assertEquals(PressClassifier.Press.NONE, c.tick(false, false));
    }

    @Test
    void clicPiuBreveDiUnTickContaComePressioneBreve() {
        PressClassifier c = new PressClassifier(T);
        assertEquals(PressClassifier.Press.TAP, c.tick(false, true));
    }

    @Test
    void nienteSenzaPressione() {
        PressClassifier c = new PressClassifier(T);
        for (int i = 0; i < 20; i++) assertEquals(PressClassifier.Press.NONE, c.tick(false, false));
    }

    @Test
    void resetInterrompeLaPressioneInCorso() {
        PressClassifier c = new PressClassifier(T);
        c.tick(true, true);
        c.tick(true, false);
        c.reset();
        // Il tasto è ancora giù: riparte da zero, quindi la tenuta arriva dopo altri T tick.
        for (int i = 0; i < T - 1; i++) assertEquals(PressClassifier.Press.NONE, c.tick(true, false));
        assertEquals(PressClassifier.Press.HOLD, c.tick(true, false));
    }
}
