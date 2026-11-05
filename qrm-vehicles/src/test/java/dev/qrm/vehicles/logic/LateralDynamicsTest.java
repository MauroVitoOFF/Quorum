package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Dinamica laterale: aderenza limitata, sotto- e sovrasterzo, derapata col freno a mano. */
class LateralDynamicsTest {

    private static final VehicleDef CHARGER = VehicleDef.CHARGER_BLACK;

    private static DriveState run(VehicleDef def, DriveState s, DriveInput in, int ticks, Surface surface) {
        for (int i = 0; i < ticks; i++) s = DrivingModel.step(def, s, in, surface, true);
        return s;
    }

    /** Raggio di curva a regime (m) con lo sterzo a fondo, partendo dalla velocità data (m/s) mantenuta col gas. */
    private static double turnRadius(VehicleDef def, double speedMs, Surface surface) {
        DriveState s = new DriveState(0, 0, 0, speedMs / DrivingModel.TPS);
        s = run(def, s, new DriveInput(0.4, 1, false), 60, surface);
        double yawRate = Math.abs(s.yawRate());
        return s.speed() * DrivingModel.TPS / Math.max(yawRate, 1e-9);
    }

    @Test
    void aVelocitaBassaVale5LaGeometriaDiAckermann() {
        DriveState s = run(CHARGER, new DriveState(0, 0, 0, 1.0 / DrivingModel.TPS), new DriveInput(0, 1, false), 60,
                Surface.PAVED);
        // R = L / tan(θ) a regime con lo sterzo a fondo (28° ridotti di poco a 1 m/s).
        double expected = CHARGER.wheelBase() / Math.tan(Math.toRadians(s.steerAngle()));
        double actual = Math.abs(s.speed() * DrivingModel.TPS / s.yawRate());
        assertEquals(expected, actual, expected * 0.1, "raggio " + actual + " contro Ackermann " + expected);
    }

    @Test
    void conIlLimiteDiAderenzaLaCurvaSiAllarga() {
        // Con lo sterzo a fondo ad alta velocità il raggio reale supera quello geometrico: l'auto sottosterza.
        DriveState s = new DriveState(0, 0, 0, 14.0 / DrivingModel.TPS);
        double worst = 0;
        for (int i = 0; i < 60; i++) {
            s = DrivingModel.step(CHARGER, s, new DriveInput(1, 1, false), Surface.PAVED, true);
            if (i < 10 || s.speed() * DrivingModel.TPS < 6) continue;
            double radius = s.speed() * DrivingModel.TPS / Math.abs(s.yawRate());
            double geometric = CHARGER.wheelBase() / Math.tan(Math.toRadians(s.steerAngle()));
            worst = Math.max(worst, radius / geometric);
        }
        assertTrue(worst > 1.15, "il raggio reale supera quello geometrico al massimo di " + worst);
    }

    @Test
    void laForzaLateraleNonSuperaLAttrito() {
        for (VehicleDef def : VehicleDef.ALL) {
            DriveState s = new DriveState(0, 0, 0, 0.6);
            for (int i = 0; i < 80; i++) {
                s = DrivingModel.step(def, s, new DriveInput(0, 1, false), Surface.PAVED, true);
                double lateralAccel = Math.abs(s.yawRate()) * s.speed() * DrivingModel.TPS;
                assertTrue(lateralAccel <= 1.25 * def.powertrain().tyreMu() * Powertrain.GRAVITY,
                        def.id() + " accelerazione laterale " + lateralAccel + " al tick " + i);
            }
        }
    }

    @Test
    void suFondoScivolosoSiSottosterzaDiPiu() {
        double paved = turnRadius(CHARGER, 14, Surface.PAVED);
        double dirt = turnRadius(CHARGER, 14, Surface.DIRT);
        assertTrue(dirt > paved, "asfalto " + paved + ", terra " + dirt);
    }

    @Test
    void ilFrenoAManoFaDerapareIlPosteriore() {
        // Sterzata a velocità sostenuta, poi freno a mano: il posteriore perde presa e l'angolo di deriva cresce.
        DriveState s = new DriveState(0, 0, 0, 0.6);
        s = run(CHARGER, s, new DriveInput(0.3, 1, false), 25, Surface.PAVED);
        DriveState grip = run(CHARGER, s, new DriveInput(0.3, 1, false), 8, Surface.PAVED);
        DriveState slide = run(CHARGER, s, new DriveInput(0.3, 1, true), 8, Surface.PAVED);
        assertTrue(slip(slide) > slip(grip) + 2, "deriva con freno a mano " + slip(slide) + " contro " + slip(grip));
    }

    @Test
    void rilasciandoLoSterzoLAutoSiRaddrizza() {
        DriveState s = new DriveState(0, 0, 0, 0.6);
        s = run(CHARGER, s, new DriveInput(0.3, 1, false), 30, Surface.PAVED);
        s = run(CHARGER, s, new DriveInput(0.3, 0, false), 80, Surface.PAVED);
        assertTrue(Math.abs(s.yawRate()) < 0.05, "imbardata residua " + s.yawRate());
        assertTrue(slip(s) < 2, "deriva residua " + slip(s));
    }

    @Test
    void fermoOQuasiFermoNonGira() {
        DriveState s = run(CHARGER, DriveState.rest(25), new DriveInput(0, 1, false), 40, Surface.PAVED);
        assertEquals(25, s.heading(), 1e-9);
        assertEquals(0, s.yawRate(), 1e-9);
    }

    @Test
    void tuttiIValoriRestanoFinitiInOgniCondizione() {
        for (VehicleDef def : VehicleDef.ALL) {
            for (Surface surface : Surface.values()) {
                DriveState s = new DriveState(0, 0, 0, def.maxSpeed());
                for (int i = 0; i < 300; i++) {
                    double steer = Math.sin(i / 7.0);
                    s = DrivingModel.step(def, s, new DriveInput(i % 90 < 60 ? 1 : -1, steer, i % 50 > 40), surface,
                            true);
                    assertTrue(Double.isFinite(s.heading()) && Double.isFinite(s.vx()) && Double.isFinite(s.vz())
                            && Double.isFinite(s.yawRate()), def.id() + " non finito al tick " + i);
                    assertTrue(s.speed() <= 1.2 * def.maxSpeed(), def.id() + " velocita' " + s.speed());
                }
            }
        }
    }

    private static double slip(DriveState s) {
        double along = s.forwardSpeed();
        double lateral = Math.sqrt(Math.max(0, s.speed() * s.speed() - along * along));
        return Math.toDegrees(Math.atan2(lateral, Math.abs(along)));
    }
}
