package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EngineStatusTest {

    private static EngineStatus run(EngineStatus s, int ticks, boolean fuelEmpty, boolean blocked, double throttle,
            double speedMs) {
        for (int i = 0; i < ticks; i++) s = s.tick(fuelEmpty, blocked, throttle, speedMs);
        return s;
    }

    private static EngineStatus idle() {
        EngineStatus s = EngineStatus.OFF.toggle(true, false);
        return run(s, EngineStatus.STARTING_TICKS, false, false, 0, 0);
    }

    @Test
    void ilVeicoloNasceSpentoENonCambiaDaSolo() {
        assertEquals(EngineState.OFF, EngineStatus.OFF.state());
        assertEquals(EngineState.OFF, run(EngineStatus.OFF, 100, false, false, 1, 10).state());
    }

    @Test
    void laMessaInMotoDuraEPoiSiVaAlMinimo() {
        EngineStatus s = EngineStatus.OFF.toggle(true, false);
        assertEquals(EngineState.STARTING, s.state());
        s = run(s, EngineStatus.STARTING_TICKS - 1, false, false, 0, 0);
        assertEquals(EngineState.STARTING, s.state(), "ancora in avviamento un tick prima della fine");
        assertFalse(s.state().isRunning(), "durante l'avviamento il motore non spinge");
        s = s.tick(false, false, 0, 0);
        assertEquals(EngineState.IDLE, s.state());
        assertTrue(s.state().isRunning());
    }

    @Test
    void ilGasEIlMovimentoPortanoInRunningEFermiSiTornaAlMinimo() {
        EngineStatus s = idle();
        s = s.tick(false, false, 1, 0);
        assertEquals(EngineState.RUNNING, s.state());
        s = run(s, 5, false, false, 0, 10);
        assertEquals(EngineState.RUNNING, s.state(), "in movimento senza gas e' ancora in lavoro");
        s = run(s, 1, false, false, 0, 0);
        assertEquals(EngineState.IDLE, s.state());
    }

    @Test
    void ilTempoNelloStatoSiAzzeraAlCambio() {
        EngineStatus s = run(idle(), 7, false, false, 0, 0);
        assertEquals(7, s.ticks());
        s = s.tick(false, false, 1, 0);
        assertEquals(0, s.ticks());
    }

    @Test
    void conIlSerbatoioVuotoOInAcquaIlMotoreVaInStallo() {
        assertEquals(EngineState.STALLED, run(idle(), 1, true, false, 0, 0).state());
        assertEquals(EngineState.STALLED, run(idle(), 1, false, true, 0, 0).state());
        EngineStatus starting = EngineStatus.OFF.toggle(true, false);
        assertEquals(EngineState.STALLED, starting.tick(true, false, 0, 0).state());
    }

    @Test
    void daStalloSiRipartePassandoPerLAccensioneSoloSeSiPuo() {
        EngineStatus stalled = run(idle(), 1, true, false, 0, 0);
        assertEquals(EngineState.STALLED, stalled.toggle(false, false).state(), "senza carburante resta in stallo");
        assertEquals(EngineState.STALLED, stalled.toggle(true, true).state(), "in acqua resta in stallo");
        assertEquals(EngineState.STARTING, stalled.toggle(true, false).state());
    }

    @Test
    void lAccensioneSpegneDaQualsiasiStatoAcceso() {
        assertEquals(EngineState.OFF, EngineStatus.OFF.toggle(true, false).toggle(true, false).state());
        assertEquals(EngineState.OFF, idle().toggle(true, false).state());
        assertEquals(EngineState.OFF, idle().tick(false, false, 1, 0).toggle(true, false).state());
    }

    @Test
    void senzaCarburanteDaSpentoNonSiAccende() {
        assertEquals(EngineState.OFF, EngineStatus.OFF.toggle(false, false).state());
    }

    @Test
    void gliIdSiRicostruiscono() {
        for (EngineState s : EngineState.values()) assertEquals(s, EngineState.byId(s.id()));
        assertEquals(EngineState.OFF, EngineState.byId(99));
        assertEquals(EngineState.OFF, EngineState.byId(-1));
    }
}
