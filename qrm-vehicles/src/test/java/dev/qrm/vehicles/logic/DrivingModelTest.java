package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DrivingModelTest {

    private static final VehicleDef JEEP = VehicleDef.JEEP;
    private static final double EPS = 1e-9;

    private static DriveState run(DriveState s, DriveInput in, int ticks, Surface surface, boolean engineOn) {
        for (int i = 0; i < ticks; i++) s = DrivingModel.step(JEEP, s, in, surface, engineOn);
        return s;
    }

    private static DriveState cruising(double speed) {
        return new DriveState(0, 0, 0, speed); // heading 0 = verso +Z
    }

    private static double slipAngleDeg(DriveState s) {
        double along = s.forwardSpeed();
        double lateral = Math.sqrt(Math.max(0, s.speed() * s.speed() - along * along));
        return Math.toDegrees(Math.atan2(lateral, along));
    }

    @Test
    void accelerazionePienaSiFermaAllaVelocitaMassima() {
        DriveState s = run(DriveState.rest(0), new DriveInput(1, 0, false), 600, Surface.PAVED, true);
        assertTrue(s.speed() <= 1.02 * JEEP.maxSpeed(), "supera la velocita' massima: " + s.speed());
        assertTrue(s.speed() > 0.95 * JEEP.maxSpeed(), "non arriva a regime: " + s.speed());
    }

    @Test
    void lAccelerazioneEMonotona() {
        DriveState s = DriveState.rest(0);
        double last = 0;
        for (int i = 0; i < 50; i++) {
            s = DrivingModel.step(JEEP, s, new DriveInput(1, 0, false), Surface.PAVED, true);
            assertTrue(s.speed() >= last - EPS, "la velocita' cala al tick " + i);
            last = s.speed();
        }
    }

    @Test
    void frenataFermaIlVeicolo() {
        DriveState s = run(cruising(JEEP.maxSpeed()), new DriveInput(0, 0, true), 60, Surface.PAVED, true);
        assertEquals(0, s.speed(), 1e-6);
    }

    @Test
    void retromarciaLimitata() {
        DriveState s = run(DriveState.rest(0), new DriveInput(-1, 0, false), 200, Surface.PAVED, true);
        assertTrue(s.forwardSpeed() >= -0.3, "retromarcia troppo veloce: " + s.forwardSpeed());
        assertTrue(s.forwardSpeed() < 0, "non va indietro");
        assertEquals(-1, s.gear());
    }

    @Test
    void terrenoDifficileRallenta() {
        DriveState s = run(DriveState.rest(0), new DriveInput(1, 0, false), 600, Surface.ROUGH, true);
        assertTrue(s.speed() < 0.7 * JEEP.maxSpeed(), "su terreno difficile va a " + s.speed());
    }

    @Test
    void fermoNonSterza() {
        DriveState s = run(DriveState.rest(10), new DriveInput(0, 1, false), 20, Surface.PAVED, true);
        assertEquals(10, s.heading(), EPS);
    }

    @Test
    void sterzataACorsaCambiaDirezione() {
        DriveState s = run(cruising(0.5 * JEEP.maxSpeed()), new DriveInput(0, 1, false), 20, Surface.PAVED, true);
        assertTrue(Math.abs(s.heading()) >= 10, "heading " + s.heading());
    }

    @Test
    void ancheAPienaVelocitaLaCurvaNonEBrusca() {
        DriveState s = cruising(JEEP.maxSpeed());
        double maxTurn = 0;
        for (int i = 0; i < 40; i++) {
            double before = s.heading();
            s = DrivingModel.step(JEEP, s, new DriveInput(0, 1, false), Surface.PAVED, true);
            maxTurn = Math.max(maxTurn, Math.abs(s.heading() - before));
        }
        assertTrue(maxTurn <= 4.0, "gira di " + maxTurn + " gradi in un tick");
    }

    @Test
    void sbandataRientra() {
        DriveState s = run(cruising(JEEP.maxSpeed()), new DriveInput(0, 1, false), 10, Surface.PAVED, true);
        double during = slipAngleDeg(s);
        assertTrue(during > 0 && during < 45, "durante la sterzata lo scarto e' " + during);
        s = run(s, new DriveInput(0, 0, false), 40, Surface.PAVED, true);
        assertTrue(slipAngleDeg(s) < 2, "dopo 40 tick lo scarto e' " + slipAngleDeg(s));
    }

    @Test
    void motoreSpentoNonAccelera() {
        DriveState s = run(DriveState.rest(0), new DriveInput(1, 0, false), 20, Surface.PAVED, false);
        assertEquals(0, s.speed(), EPS);
        s = cruising(JEEP.maxSpeed());
        double start = s.speed();
        for (int i = 0; i < 200; i++) {
            double before = s.speed();
            s = DrivingModel.step(JEEP, s, new DriveInput(1, 0, false), Surface.PAVED, false);
            assertTrue(s.speed() < before, "non rallenta al tick " + i);
            assertEquals(0, s.rpm(), EPS);
        }
        assertTrue(s.speed() < 0.9 * start, "a motore spento la resistenza non frena: " + s.speed());
    }

    @Test
    void inputCorrottoNonRompeIlModello() {
        DriveInput in = new DriveInput(Double.NaN, 5.0, false).clamped();
        assertEquals(0, in.throttle(), EPS);
        assertEquals(1, in.steer(), EPS);
        DriveState s = run(DriveState.rest(0), in, 10, Surface.PAVED, true);
        assertTrue(Double.isFinite(s.heading()) && Double.isFinite(s.vx()) && Double.isFinite(s.vz())
                && Double.isFinite(s.steerAngle()) && Double.isFinite(s.rpm()));
    }
}
