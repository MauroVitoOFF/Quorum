package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Forze, motore e cambio: le proprietà fisiche che il modello di guida deve rispettare per ogni veicolo. */
class PowertrainTest {

    private static DriveState run(VehicleDef def, DriveState s, DriveInput in, int ticks, Surface surface) {
        for (int i = 0; i < ticks; i++) s = DrivingModel.step(def, s, in, surface, true);
        return s;
    }

    private static double stoppingDistance(VehicleDef def, Surface surface, double speedBlocksPerTick, DriveInput in) {
        DriveState s = new DriveState(0, 0, 0, speedBlocksPerTick);
        double z = 0;
        for (int i = 0; i < 2000 && s.forwardSpeed() > 1e-9; i++) {
            s = DrivingModel.step(def, s, in, surface, true);
            z += s.vz();
        }
        return z;
    }

    @Test
    void ogniVeicoloArrivaAllaVelocitaDiProgettoSenzaSuperarla() {
        for (VehicleDef def : VehicleDef.ALL) {
            DriveState s = run(def, DriveState.rest(0), new DriveInput(1, 0, false), 1200, Surface.PAVED);
            assertTrue(s.speed() <= 1.03 * def.maxSpeed(), def.id() + " troppo veloce: " + s.speed());
            assertTrue(s.speed() >= 0.93 * def.maxSpeed(), def.id() + " troppo lento: " + s.speed());
            assertEquals(def.powertrain().gears(), s.gear(), def.id() + " non arriva all'ultima marcia");
        }
    }

    @Test
    void ilLimitatoreFermaLaSpintaAlRegimeMassimo() {
        for (VehicleDef def : VehicleDef.ALL) {
            Powertrain pt = def.powertrain();
            DriveState s = run(def, DriveState.rest(0), new DriveInput(1, 0, false), 1200, Surface.PAVED);
            double limitMs = pt.redlineRpm() * 2 * Math.PI / 60 * pt.wheelRadius() / pt.ratio(pt.gears());
            assertTrue(s.speed() * DrivingModel.TPS <= limitMs * 1.01, def.id());
            assertTrue(s.rpm() <= pt.redlineRpm() + 1e-6 && s.rpm() >= pt.idleRpm(), def.id() + " rpm " + s.rpm());
        }
    }

    @Test
    void ilCambioSaleEScalaSenzaOscillare() {
        for (VehicleDef def : VehicleDef.ALL) {
            Powertrain pt = def.powertrain();
            for (int g = 1; g < pt.gears(); g++) {
                double step = pt.ratio(g) / pt.ratio(g + 1);
                assertTrue(step > 1, def.id() + ": i rapporti devono diminuire");
                // Dopo aver salito a shiftUp si resta sopra shiftDown, e dopo aver scalato sotto shiftUp.
                assertTrue(pt.shiftUpRpm() / step > pt.shiftDownRpm(), def.id() + " sale e subito riscala");
                assertTrue(pt.shiftDownRpm() * step < pt.shiftUpRpm(), def.id() + " scala e subito risale");
            }
        }
    }

    @Test
    void daFermoSiPartePrimaMarciaEIlRegimeStaAlMinimo() {
        for (VehicleDef def : VehicleDef.ALL) {
            DriveState idle = run(def, DriveState.rest(0), DriveInput.NONE, 5, Surface.PAVED);
            assertEquals(1, idle.gear());
            assertEquals(def.powertrain().idleRpm(), idle.rpm(), 1e-9);
            assertEquals(0, idle.speed(), 1e-12);
            DriveState go = run(def, DriveState.rest(0), new DriveInput(1, 0, false), 10, Surface.PAVED);
            assertEquals(1, go.gear());
            assertTrue(go.speed() > 0, def.id() + " non parte");
        }
    }

    @Test
    void laRetromarciaSiInnestaEIlFrenoNonLaInverte() {
        VehicleDef def = VehicleDef.CHARGER_BLACK;
        DriveState back = run(def, DriveState.rest(0), new DriveInput(-1, 0, false), 40, Surface.PAVED);
        assertEquals(-1, back.gear());
        assertTrue(back.forwardSpeed() < 0);
        // In marcia avanti S frena e non innesta la retromarcia.
        DriveState s = run(def, new DriveState(0, 0, 0, 0.5), new DriveInput(-1, 0, false), 1, Surface.PAVED);
        assertTrue(s.gear() > 0, "andando avanti S frena e non innesta la retro");
        DriveState halted = new DriveState(0, 0, 0, 0.5);
        for (int i = 0; i < 100; i++) {
            halted = DrivingModel.step(def, halted, new DriveInput(0, 0, true), Surface.PAVED, true);
            assertTrue(halted.forwardSpeed() >= 0, "il freno a mano fa retrocedere al tick " + i);
        }
        assertEquals(0, halted.speed(), 1e-9);
    }

    @Test
    void suTerraEFangoSiFrenaPiuLontano() {
        VehicleDef def = VehicleDef.CHARGER_BLACK;
        DriveInput brake = new DriveInput(-1, 0, false);
        double paved = stoppingDistance(def, Surface.PAVED, 0.7, brake);
        double dirt = stoppingDistance(def, Surface.DIRT, 0.7, brake);
        double rough = stoppingDistance(def, Surface.ROUGH, 0.7, brake);
        assertTrue(paved < dirt && dirt < rough, paved + " " + dirt + " " + rough);
    }

    @Test
    void ilFrenoABloccaggioFrenaMenoDelPedale() {
        // Il freno a mano supera l'attrito disponibile: le ruote si bloccano e lo spazio è più lungo del pedale,
        // che sull'asfalto resta sotto il limite.
        VehicleDef def = VehicleDef.CHARGER_BLACK;
        double pedal = stoppingDistance(def, Surface.PAVED, 0.7, new DriveInput(-1, 0, false));
        double locked = stoppingDistance(def, Surface.PAVED, 0.7, new DriveInput(0, 0, true));
        assertTrue(locked > pedal, "bloccate " + locked + " contro pedale " + pedal);
    }

    @Test
    void ilFondoDifficileRiduceLaTrazione() {
        VehicleDef def = VehicleDef.CHARGER_BLACK;
        DriveInput full = new DriveInput(1, 0, false);
        double paved = run(def, DriveState.rest(0), full, 20, Surface.PAVED).speed();
        double rough = run(def, DriveState.rest(0), full, 20, Surface.ROUGH).speed();
        assertTrue(rough < 0.8 * paved, "asfalto " + paved + ", fondo difficile " + rough);
    }

    @Test
    void aGasChiusoIlVeicoloRallentaMaNonInstantaneamente() {
        VehicleDef def = VehicleDef.JEEP;
        DriveState s = new DriveState(0, 0, 0, def.maxSpeed());
        s = run(def, s, DriveInput.NONE, 20, Surface.PAVED);
        assertTrue(s.speed() < def.maxSpeed() && s.speed() > 0.5 * def.maxSpeed(), "dopo 1 s: " + s.speed());
    }
}
