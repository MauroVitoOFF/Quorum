package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Guida classica: volante lento, sterzo che perde efficacia con la velocità, moto che segue il muso entro la presa. */
class ClassicHandlingTest {

    private static final VehicleDef CHARGER = VehicleDef.CHARGER_BLACK;

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
    void ilVolanteImpiegaUnSecondoACompletareLaCorsa() {
        DriveState s = new DriveState(0, 0, 0, 10.0 / DrivingModel.TPS);
        int ticks = 0;
        while (Math.abs(s.steerAngle()) < ClassicHandling.LOCK_DEG - 1e-9 && ticks < 100) {
            s = DrivingModel.step(CHARGER, s, new DriveInput(0, 1, false), Surface.PAVED, true);
            ticks++;
        }
        assertEquals(Math.round(ClassicHandling.LOCK_DEG / ClassicHandling.TURN_RATE), ticks, 1);
    }

    @Test
    void ilRitornoAlCentroEIlDoppioPiuVeloce() {
        assertEquals(8, ClassicHandling.steerAngle(12, 0), 1e-9);
        assertEquals(-2, ClassicHandling.steerAngle(2, -1), 1e-9); // dal lato opposto: in fretta fino al centro e oltre
        assertEquals(2, ClassicHandling.steerAngle(0, 1), 1e-9);
    }

    @Test
    void loSterzoPerdeEfficaciaAdAltaVelocita() {
        double slow = ClassicHandling.yawDegPerTick(30, 8, 3.525, false);
        double fast = ClassicHandling.yawDegPerTick(30, 28, 3.525, false);
        assertTrue(slow > fast, "a bassa velocità " + slow + " contro alta " + fast);
        assertEquals(0, ClassicHandling.yawDegPerTick(30, 0, 3.525, false), 1e-12);
        // In retromarcia il muso gira al contrario, come in una macchina vera.
        assertEquals(-ClassicHandling.yawDegPerTick(30, 5, 3.525, false),
                ClassicHandling.yawDegPerTick(30, -5, 3.525, false), 1e-12);
    }

    @Test
    void entroLaPresaLaVelocitaSegueIlMuso() {
        DriveState s = new DriveState(0, 0, 0, 14.0 / DrivingModel.TPS);
        for (int i = 0; i < 80; i++) {
            s = DrivingModel.step(CHARGER, s, new DriveInput(0.3, 0.4, false), Surface.PAVED, true);
            assertTrue(slip(s) < 8, "deriva " + slip(s) + " al tick " + i);
        }
    }

    @Test
    void ilFrenoAManoMetteLAutoDiTraverso() {
        DriveState s = new DriveState(0, 0, 0, 22.0 / DrivingModel.TPS);
        s = run(s, new DriveInput(0.3, 1, false), 25, Surface.PAVED);
        DriveState grip = run(s, new DriveInput(0.3, 1, false), 8, Surface.PAVED);
        DriveState slide = run(s, new DriveInput(0.3, 1, true), 8, Surface.PAVED);
        assertTrue(slip(slide) > slip(grip) + 3, "col freno a mano " + slip(slide) + " contro " + slip(grip));
    }

    @Test
    void suGhiaccioLaPresaEPiuBassa() {
        DriveState start = new DriveState(0, 0, 0, 22.0 / DrivingModel.TPS);
        double ice = slip(run(start, new DriveInput(0.3, 1, false), 40, Surface.ICE));
        double paved = slip(run(start, new DriveInput(0.3, 1, false), 40, Surface.PAVED));
        assertTrue(ice > paved, "ghiaccio " + ice + " contro asfalto " + paved);
    }

    @Test
    void rilasciandoLoSterzoLAutoSiRaddrizza() {
        DriveState s = new DriveState(0, 0, 0, 14.0 / DrivingModel.TPS);
        s = run(s, new DriveInput(0.3, 1, false), 30, Surface.PAVED);
        s = run(s, new DriveInput(0.3, 0, false), 80, Surface.PAVED);
        assertTrue(Math.abs(s.yawRate()) < 0.05, "imbardata residua " + s.yawRate());
        assertTrue(slip(s) < 2, "deriva residua " + slip(s));
    }

    @Test
    void ilModelloNonSiAvvitaConGasESterzoAFondo() {
        DriveState s = DriveState.rest(0);
        for (int i = 0; i < 400; i++) {
            s = DrivingModel.step(CHARGER, s, new DriveInput(1, i > 15 ? 1 : 0, false), Surface.PAVED, true);
            assertTrue(slip(s) < 30, "deriva " + slip(s) + " al tick " + i);
            assertTrue(Double.isFinite(s.heading()) && Double.isFinite(s.yawRate()));
        }
    }
}
