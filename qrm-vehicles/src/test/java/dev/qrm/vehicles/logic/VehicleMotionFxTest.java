package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VehicleMotionFxTest {

    private static final VehicleDef JEEP = VehicleDef.JEEP;

    private static void run(VehicleMotionFx fx, double speed, double steer, int ticks) {
        for (int i = 0; i < ticks; i++) fx.tick(speed, steer, JEEP.maxSpeed(), JEEP.maxSteerDeg());
    }

    @Test
    void leRuoteGiranoInProporzioneAllaVelocita() {
        VehicleMotionFx fx = new VehicleMotionFx();
        // A velocità pari al raggio della ruota, ogni tick fa un radiante.
        run(fx, VehicleMotionFx.WHEEL_RADIUS, 0, 10);
        assertEquals(10 * Math.toDegrees(1), fx.wheelDeg(1f), 1e-6);
    }

    @Test
    void inRetromarciaGiranoAlContrario() {
        VehicleMotionFx fx = new VehicleMotionFx();
        run(fx, -VehicleMotionFx.WHEEL_RADIUS, 0, 5);
        assertTrue(fx.wheelDeg(1f) < 0);
    }

    @Test
    void frenandoIlMusoSiAbbassa() {
        VehicleMotionFx fx = new VehicleMotionFx();
        run(fx, 0.5, 0, 100);
        run(fx, 0.0, 0, 1); // frenata secca
        assertTrue(fx.pitchDeg(1f) < 0, "il muso non si abbassa: " + fx.pitchDeg(1f));
        assertTrue(fx.pitchDeg(1f) >= -VehicleMotionFx.MAX_PITCH_DEG);
    }

    @Test
    void accelerandoIlMusoSiAlza() {
        VehicleMotionFx fx = new VehicleMotionFx();
        run(fx, 0.0, 0, 5);
        run(fx, 0.02, 0, 1);
        assertTrue(fx.pitchDeg(1f) > 0, "il muso non si alza: " + fx.pitchDeg(1f));
    }

    @Test
    void inCurvaLaCasseSiInclinaVersoLEsterno() {
        VehicleMotionFx right = new VehicleMotionFx();
        run(right, 0.5, JEEP.maxSteerDeg(), 30);
        assertTrue(right.rollDeg(1f) > 0, "curva a destra: " + right.rollDeg(1f));
        assertTrue(right.rollDeg(1f) <= VehicleMotionFx.MAX_ROLL_DEG + 1e-9);

        VehicleMotionFx left = new VehicleMotionFx();
        run(left, 0.5, -JEEP.maxSteerDeg(), 30);
        assertTrue(left.rollDeg(1f) < 0, "curva a sinistra: " + left.rollDeg(1f));
    }

    @Test
    void fermoNonSiInclina() {
        VehicleMotionFx fx = new VehicleMotionFx();
        run(fx, 0.0, JEEP.maxSteerDeg(), 50);
        assertEquals(0, fx.rollDeg(1f), 1e-9);
    }

    @Test
    void aRegimeTornaInAssetto() {
        VehicleMotionFx fx = new VehicleMotionFx();
        run(fx, 0.5, JEEP.maxSteerDeg(), 30);
        run(fx, 0.5, 0, 100);
        assertEquals(0, fx.pitchDeg(1f), 0.01);
        assertEquals(0, fx.rollDeg(1f), 0.01);
    }

    @Test
    void l_interpolazioneVaDalPrecedenteAlCorrente() {
        VehicleMotionFx fx = new VehicleMotionFx();
        run(fx, VehicleMotionFx.WHEEL_RADIUS, 0, 3);
        double prev = fx.wheelDeg(0f);
        double cur = fx.wheelDeg(1f);
        assertEquals(Math.toDegrees(2), prev, 1e-6);
        assertEquals(Math.toDegrees(3), cur, 1e-6);
        assertEquals((prev + cur) / 2, fx.wheelDeg(0.5f), 1e-6);
    }
}
