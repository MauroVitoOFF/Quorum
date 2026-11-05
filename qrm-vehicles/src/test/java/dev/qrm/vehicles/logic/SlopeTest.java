package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** La pendenza del terreno: gravità lungo il muso. */
class SlopeTest {

    private static final VehicleDef DEF = VehicleDef.CHARGER_BLACK;

    private static DriveState run(DriveState s, DriveInput in, int ticks, double slope) {
        for (int i = 0; i < ticks; i++) s = DrivingModel.step(DEF, s, in, Surface.PAVED, true, slope);
        return s;
    }

    @Test
    void pendenzaZeroEquivaleAlTerrenoPiano() {
        DriveState a = run(DriveState.rest(0), new DriveInput(1, 0, false), 40, 0);
        DriveState b = DriveState.rest(0);
        for (int i = 0; i < 40; i++) b = DrivingModel.step(DEF, b, new DriveInput(1, 0, false), Surface.PAVED, true);
        assertEquals(a.speed(), b.speed(), 1e-12);
    }

    @Test
    void inSalitaSiVaPiuPianoEInDiscesaPiuForte() {
        DriveInput gas = new DriveInput(1, 0, false);
        double flat = run(DriveState.rest(0), gas, 40, 0).speed();
        double up = run(DriveState.rest(0), gas, 40, 0.2).speed();
        double down = run(DriveState.rest(0), gas, 40, -0.2).speed();
        assertTrue(up < flat && flat < down, up + " " + flat + " " + down);
    }

    @Test
    void aGasChiusoInDiscesaIlVeicoloPartePerConto() {
        DriveState s = run(DriveState.rest(0), DriveInput.NONE, 80, -0.2);
        assertTrue(s.forwardSpeed() > 0.1, "velocita' " + s.forwardSpeed());
    }

    @Test
    void ilFrenoATenutaFermoIlVeicoloInSalita() {
        DriveState s = run(DriveState.rest(0), new DriveInput(0, 0, true), 100, 0.2);
        assertEquals(0, s.speed(), 1e-9);
    }

    @Test
    void unaPendenzaFolleOUnValoreNonFinitoNonRompeIlModello() {
        DriveState s = run(DriveState.rest(0), new DriveInput(1, 0, false), 20, Double.NaN);
        assertTrue(Double.isFinite(s.speed()));
        s = run(s, new DriveInput(1, 0, false), 20, 50);
        assertTrue(Double.isFinite(s.speed()));
    }
}
