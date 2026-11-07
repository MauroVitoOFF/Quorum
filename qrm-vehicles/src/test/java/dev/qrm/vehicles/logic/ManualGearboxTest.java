package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ManualGearboxTest {

    private static final VehicleDef CAR = VehicleDef.CHARGER_BLACK;

    private static DriveState run(DriveState s, DriveInput in, int ticks, int gear) {
        for (int i = 0; i < ticks; i++) s = DrivingModel.step(CAR, s, in, Surface.PAVED, true, 0, gear);
        return s;
    }

    @Test
    void inFolleIlGasNonMuoveLAuto() {
        DriveState s = run(DriveState.rest(0), new DriveInput(1, 0, false), 100, 0);
        assertEquals(0, s.speed(), 1e-9);
        assertTrue(s.rpm() > CAR.powertrain().idleRpm(), "il motore non sale di giri: " + s.rpm());
    }

    @Test
    void inPrimaIlGasPortaAvanti() {
        DriveState s = run(DriveState.rest(0), new DriveInput(1, 0, false), 100, 1);
        assertTrue(s.forwardSpeed() > 0.2, "non parte: " + s.forwardSpeed());
        assertEquals(1, s.gear());
    }

    @Test
    void conLaRetromarciaIlGasVaIndietro() {
        DriveState s = run(DriveState.rest(0), new DriveInput(1, 0, false), 100, -1);
        assertTrue(s.forwardSpeed() < -0.05, "non va indietro: " + s.forwardSpeed());
        assertEquals(-1, s.gear());
    }

    @Test
    void inManualeIlComandoIndietroFrenaENonInnestaLaRetro() {
        DriveState s = run(DriveState.rest(0), new DriveInput(1, 0, false), 100, 1);
        DriveState b = run(s, new DriveInput(-1, 0, false), 200, 1);
        assertTrue(b.forwardSpeed() >= -1e-9, "ha innestato la retro da solo: " + b.forwardSpeed());
        assertTrue(b.speed() < s.speed());
    }

    @Test
    void ilCambioNonCambiaDaSolo() {
        DriveState s = run(DriveState.rest(0), new DriveInput(1, 0, false), 600, 1);
        assertEquals(1, s.gear());
        assertTrue(s.rpm() <= CAR.powertrain().redlineRpm());
    }

    @Test
    void ilCambioAutomaticoRestaComeEra() {
        DriveState s = DriveState.rest(0);
        for (int i = 0; i < 400; i++) {
            s = DrivingModel.step(CAR, s, new DriveInput(1, 0, false), Surface.PAVED, true);
        }
        assertTrue(s.gear() > 1, "l'automatico non sale di marcia: " + s.gear());
    }
}
