package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Guida arcade: sterzo diretto, velocità che segue il muso entro il limite di aderenza. */
class ArcadeHandlingTest {

    private static final VehicleDef BASE = VehicleDef.CHARGER_BLACK;
    /** La Charger con la guida arcade (quella di serie ora è la classica). */
    private static final VehicleDef CHARGER = new VehicleDef(BASE.id(), BASE.seats(), BASE.maxSpeed(),
            BASE.maxSteerDeg(), BASE.steerRateDeg(), BASE.wheelBase(), BASE.fuelCapacity(), BASE.fuelPer100Blocks(),
            BASE.powertrain(), HandlingModel.ARCADE);

    private static double slip(DriveState s) {
        double along = s.forwardSpeed();
        double lateral = Math.sqrt(Math.max(0, s.speed() * s.speed() - along * along));
        return Math.toDegrees(Math.atan2(lateral, Math.abs(along)));
    }

    private static DriveState run(DriveState s, DriveInput in, int ticks, Surface surface) {
        for (int i = 0; i < ticks; i++) s = DrivingModel.step(CHARGER, s, in, surface, true);
        return s;
    }

    @Test
    void laChargerEClassicaELaJeepNo() {
        assertEquals(HandlingModel.CLASSIC, VehicleDef.CHARGER_BLACK.handling());
        assertEquals(HandlingModel.DYNAMIC, VehicleDef.JEEP.handling());
    }

    @Test
    void ilModelloSiLeggeEScriveInJson() {
        String json = VehicleParams.toJson(CHARGER);
        assertTrue(json.contains("\"handling\": \"arcade\""), json);
        assertEquals(HandlingModel.DYNAMIC,
                VehicleParams.overlay(CHARGER, "{\"handling\":\"dynamic\"}").handling());
        assertEquals(HandlingModel.ARCADE, VehicleParams.overlay(CHARGER, "{}").handling());
        assertEquals(HandlingModel.CLASSIC, VehicleParams.overlay(CHARGER, "{\"handling\":\"classic\"}").handling());
    }

    @Test
    void unModelloSconosciutoVieneRifiutato() {
        try {
            VehicleParams.overlay(CHARGER, "{\"handling\":\"boh\"}");
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("doveva rifiutare il modello");
    }

    @Test
    void loSterzoRispondeSubito() {
        DriveState s = new DriveState(0, 0, 0, 20.0 / DrivingModel.TPS);
        DriveState early = run(s, new DriveInput(0.4, 1, false), 8, Surface.PAVED);
        DriveState late = run(s, new DriveInput(0.4, 1, false), 40, Surface.PAVED);
        assertTrue(Math.abs(early.yawRate()) > 0.6 * Math.abs(late.yawRate()),
                "dopo 8 tick " + early.yawRate() + " contro regime " + late.yawRate());
    }

    @Test
    void suAsfaltoLaVelocitaSegueIlMusoSenzaScivolare() {
        DriveState s = new DriveState(0, 0, 0, 25.0 / DrivingModel.TPS);
        for (int i = 0; i < 60; i++) {
            s = DrivingModel.step(CHARGER, s, new DriveInput(0.5, 1, false), Surface.PAVED, true);
            assertTrue(slip(s) < 3, "deriva " + slip(s) + " al tick " + i);
        }
    }

    @Test
    void suGhiaccioSiCurvaMoltoMeno() {
        DriveState ice = run(new DriveState(0, 0, 0, 25.0 / DrivingModel.TPS), new DriveInput(0, 1, false), 40,
                Surface.ICE);
        DriveState paved = run(new DriveState(0, 0, 0, 25.0 / DrivingModel.TPS), new DriveInput(0, 1, false), 40,
                Surface.PAVED);
        assertTrue(Math.abs(ice.yawRate()) < 0.5 * Math.abs(paved.yawRate()),
                "ghiaccio " + ice.yawRate() + " contro asfalto " + paved.yawRate());
    }

    @Test
    void ilFrenoAManoMetteLAutoDiTraverso() {
        DriveState s = new DriveState(0, 0, 0, 25.0 / DrivingModel.TPS);
        s = run(s, new DriveInput(0.3, 1, false), 15, Surface.PAVED);
        DriveState slide = run(s, new DriveInput(0, 1, true), 10, Surface.PAVED);
        assertTrue(slip(slide) > 5, "deriva col freno a mano " + slip(slide));
    }

    @Test
    void ilPesoDellaVelocitaNonCresceCurvando() {
        DriveState s = new DriveState(0, 0, 0, 25.0 / DrivingModel.TPS);
        double before = s.speed();
        for (int i = 0; i < 40; i++) {
            s = DrivingModel.step(CHARGER, s, new DriveInput(0, 1, false), Surface.PAVED, true);
            assertTrue(s.speed() <= before + 1e-9, "velocita' " + s.speed() + " al tick " + i);
        }
    }

    @Test
    void allaVelocitaMassimaLaVelocitaNonPulsa() {
        for (VehicleDef def : VehicleDef.ALL) {
            DriveState s = DriveState.rest(0);
            for (int i = 0; i < 600; i++) s = DrivingModel.step(def, s, new DriveInput(1, 0, false), Surface.PAVED, true);
            double min = Double.MAX_VALUE;
            double max = 0;
            for (int i = 0; i < 100; i++) {
                s = DrivingModel.step(def, s, new DriveInput(1, 0, false), Surface.PAVED, true);
                double v = s.speed() * DrivingModel.TPS;
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
            assertTrue(max - min < 0.3, def.id() + ": la velocita' pulsa tra " + min + " e " + max);
        }
    }
}
