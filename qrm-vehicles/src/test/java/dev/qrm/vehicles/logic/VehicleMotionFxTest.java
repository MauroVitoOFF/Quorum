package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.qrm.client.api.rig.BoneTransform;
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

    @Test
    void unGradinoScuoteLaCassaCheSiAssesta() {
        VehicleMotionFx fx = new VehicleMotionFx();
        double peak = 0;
        for (int i = 0; i < 80; i++) {
            fx.tick(0.5, 0, 1.8, 28, i == 5 ? 0.3 : 0);
            peak = Math.max(peak, Math.abs(fx.heavePx(1f)));
            assertTrue(Math.abs(fx.heavePx(1f)) <= VehicleMotionFx.MAX_HEAVE_PX + 1e-9);
        }
        assertTrue(peak > 0.3, "la cassa non si muove: " + peak);
        assertTrue(Math.abs(fx.heavePx(1f)) < 0.05, "non si assesta: " + fx.heavePx(1f));
    }

    @Test
    void ilTerrenoPianoNonMuoveLaCassa() {
        VehicleMotionFx fx = new VehicleMotionFx();
        for (int i = 0; i < 50; i++) fx.tick(0.5, 0, 1.8, 28, 0);
        assertEquals(0, fx.heavePx(1f), 1e-9);
    }

    @Test
    void unaRipresaFaRimbalzareIlMusoMaPoiSiAssesta() {
        VehicleMotionFx fx = new VehicleMotionFx();
        double speed = 0.2;
        for (int i = 0; i < 6; i++) fx.tick(speed += 0.05, 0, 1.8, 28);
        for (int i = 0; i < 60; i++) fx.tick(speed, 0, 1.8, 28);
        assertEquals(0, fx.pitchDeg(1f), 0.05);
    }

    @Test
    void ilVolanteGiraAttornoAlSuoMozzoELungoLAsseInclinato() {
        // Scostamento dal perno di steering_wheel al mozzo vero, dopo l'inclinazione di 20 gradi della piantana.
        double[] d = {0, 3.6 * 0.34202, 3.6 * 0.93969 - 3.6};
        for (float steer : new float[] {20, -20, 5}) {
            BoneTransform bt = VehiclePose.pose(0, steer, 0, 0).get("steering_wheel");
            assertTrue(Math.abs(bt.rotationDeg()[2]) > 5, "angolo " + java.util.Arrays.toString(bt.rotationDeg()));
            double x = Math.toRadians(bt.rotationDeg()[0]);
            double y = Math.toRadians(bt.rotationDeg()[1]);
            double z = Math.toRadians(bt.rotationDeg()[2]);
            // Z·Y·X applicata al vettore perno->mozzo, poi la traslazione: il mozzo deve restare fermo.
            double[] v = {d[0], d[1] * Math.cos(x) - d[2] * Math.sin(x), d[1] * Math.sin(x) + d[2] * Math.cos(x)};
            v = new double[] {v[0] * Math.cos(y) + v[2] * Math.sin(y), v[1], -v[0] * Math.sin(y) + v[2] * Math.cos(y)};
            v = new double[] {v[0] * Math.cos(z) - v[1] * Math.sin(z), v[0] * Math.sin(z) + v[1] * Math.cos(z), v[2]};
            for (int i = 0; i < 3; i++) assertEquals(d[i], bt.position()[i] + v[i], 1e-3);
        }
        BoneTransform straight = VehiclePose.pose(0, 0, 0, 0).get("steering_wheel");
        assertEquals(0, straight.rotationDeg()[2], 1e-6);
        assertEquals(3f, VehiclePose.pose(0, 0, 0, 0, 3f).get("body").position()[1], 1e-6);
    }

    @Test
    void loSterzoScorreSenzaScatti() {
        VehicleMotionFx fx = new VehicleMotionFx();
        double last = 0;
        double biggestStep = 0;
        for (int i = 0; i < 40; i++) {
            fx.tick(0.5, 20, 1.8, 28);
            for (float p = 0.25f; p <= 1f; p += 0.25f) {
                double now = fx.steerDeg(p);
                biggestStep = Math.max(biggestStep, Math.abs(now - last));
                last = now;
            }
        }
        assertTrue(biggestStep < 3.0, "salto massimo " + biggestStep);
        assertEquals(20, fx.steerDeg(1f), 0.1);
    }
}
