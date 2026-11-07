package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Urto tra veicoli, scocca contro scocca, limite di velocità del fondo e controlli dei parametri. */
class VehicleCollisionTest {

    /** Una scocca di 2 x 5 blocchi, alta 1,5, centrata sui piedi. */
    private static final VehicleHitboxes CAR =
            new VehicleHitboxes(List.of(new VehicleHitboxes.Box(0, 0.75, 0, 1, 0.75, 2.5)), Map.of());

    // ---- impulso ----

    @Test
    void tamponamentoConservaLaQuantitaDiMoto() {
        // Veicolo 1 (2000 kg, 1 blocco/tick verso +Z) tampona il 2 (1000 kg, fermo) davanti a lui: normale dal 2 all'1 = -Z.
        double[] dv = VehicleCollision.impulse(2000, 0, 1, 1000, 0, 0, 0, -1, 0.2);
        assertNotNull(dv);
        double before = 2000 * 1;
        double after = 2000 * (1 + dv[1]) + 1000 * dv[3];
        assertEquals(before, after, 1e-9);
        assertTrue(dv[1] < 0, "chi tampona rallenta");
        assertTrue(dv[3] > 0, "chi è tamponato parte in avanti");
        // Il più pesante cambia velocità meno del più leggero.
        assertTrue(Math.abs(dv[1]) < Math.abs(dv[3]));
    }

    @Test
    void urtoAnelasticoLasciaUnaVelocitaDiAllontanamentoPiccola() {
        double e = 0.2;
        double[] dv = VehicleCollision.impulse(1500, 0, 1, 1500, 0, 0, 0, -1, e);
        double v1 = 1 + dv[1];
        double v2 = dv[3];
        // Dopo l'urto si allontanano a e volte la velocità con cui si avvicinavano.
        assertEquals(e * 1, v2 - v1, 1e-9);
    }

    @Test
    void chiSiAllontanaNonRiceveImpulso() {
        assertNull(VehicleCollision.impulse(1500, 0, -1, 1500, 0, 0, 0, -1, 0.2));
        assertNull(VehicleCollision.impulse(1500, 1, 0, 1500, 1, 0, 0, -1, 0.2));
    }

    @Test
    void ilPiuLeggeroSiSpostaDiPiu() {
        assertEquals(0.5, VehicleCollision.separationShare(1000, 1000), 1e-9);
        assertTrue(VehicleCollision.separationShare(1000, 3000) > 0.5);
    }

    // ---- scocca contro scocca ----

    @Test
    void scoccheLontaneNonSiToccano() {
        assertNull(CAR.bodyPush(0, 0, 0, 0, CAR, 10, 0, 0, 0));
        // Una sopra l'altra senza sovrapporsi in altezza.
        assertNull(CAR.bodyPush(0, 0, 0, 0, CAR, 0, 2, 0, 0));
    }

    @Test
    void scoccheAffiancateSiSpingonoDiLato() {
        // Larghe 2: a 1,5 di distanza si sovrappongono di 0,5 lungo X.
        double[] p = CAR.bodyPush(0, 0, 0, 0, CAR, 1.5, 0, 0, 0);
        assertNotNull(p);
        assertEquals(-0.5, p[0], 1e-6);
        assertEquals(0, p[1], 1e-6);
        assertEquals(-1, p[2], 1e-6);
    }

    @Test
    void scoccaRuotataUsaGliAssiDiEntrambe() {
        // L'altra è girata di 90 gradi: lunga 5 lungo X. Il suo fianco arriva a 2,5 dal centro.
        assertNull(CAR.bodyPush(0, 0, 0, 0, CAR, 3.6, 0, 0, 90));
        double[] p = CAR.bodyPush(0, 0, 0, 0, CAR, 3.0, 0, 0, 90);
        assertNotNull(p);
        assertEquals(-0.5, p[0], 1e-6);
        // La spinta è simmetrica: l'altra viene spinta dal verso opposto.
        double[] q = CAR.bodyPush(3.0, 0, 0, 90, CAR, 0, 0, 0, 0);
        assertNotNull(q);
        assertEquals(0.5, q[0], 1e-6);
    }

    // ---- fondo lento ----

    private static double topSpeed(VehicleDef def, Surface surface) {
        DriveState s = DriveState.rest(0);
        for (int i = 0; i < 1500; i++) {
            s = DrivingModel.step(def, s, new DriveInput(1, 0, false), surface, true);
        }
        return s.forwardSpeed();
    }

    @Test
    void unFondoLentoLimitaLaVelocitaDiPunta() {
        VehicleDef def = VehicleDef.CHARGER_BLACK;
        Surface slow = new Surface("test_slow", 1.0, 0.5, Surface.PAVED.rollFactor());
        double paved = topSpeed(def, Surface.PAVED);
        double limited = topSpeed(def, slow);
        assertTrue(paved > def.maxSpeed() * 0.6, "su asfalto si va oltre metà della punta: " + paved);
        assertTrue(limited <= def.maxSpeed() * 0.5 * 1.1, "velocità sul fondo lento: " + limited);
        assertTrue(limited > def.maxSpeed() * 0.5 * 0.8, "il fondo lento non deve fermare l'auto: " + limited);
    }

    @Test
    void sullaSabbiaSiSenteLaDifferenza() {
        for (VehicleDef def : VehicleDef.ALL) {
            double paved = topSpeed(def, Surface.PAVED);
            double sand = topSpeed(def, Surface.SAND);
            assertTrue(sand < paved * 0.6, def.id() + ": sabbia " + sand + " contro asfalto " + paved);
            // E si accelera più piano: dopo 3 secondi a gas pieno si è più lenti che su asfalto.
            DriveState a = DriveState.rest(0);
            DriveState s = DriveState.rest(0);
            for (int i = 0; i < 60; i++) {
                a = DrivingModel.step(def, a, new DriveInput(1, 0, false), Surface.PAVED, true);
                s = DrivingModel.step(def, s, new DriveInput(1, 0, false), Surface.SAND, true);
            }
            assertTrue(s.forwardSpeed() < a.forwardSpeed() * 0.85,
                    def.id() + ": dopo 3 s sabbia " + s.forwardSpeed() + " contro " + a.forwardSpeed());
        }
    }

    // ---- scala di lastre ----

    /** Il suolo, una lastra davanti al muso e subito dopo un blocco pieno (la scocca va da z -2,5 a +2,5). */
    private static final double[] GROUND = {-5, -1, -5, 5, 0, 5};
    private static final double[] SLAB = {-3, 0, 1, 3, 0.5, 2};
    private static final double[] BLOCK = {-3, 0, 2, 3, 1, 3};

    @Test
    void sullaScalaLastraBloccoIlMusoNonSiFerma() {
        assertNull(CAR.bodyPush(0, 0, 0, 0, List.of(GROUND, SLAB, BLOCK), 0.6));
    }

    @Test
    void unMuroSenzaGradinoFermaAncoraLaScocca() {
        assertNotNull(CAR.bodyPush(0, 0, 0, 0, List.of(GROUND, BLOCK), 0.6));
        // Un gradino troppo basso per arrivare in cima al muro non lo rende salibile.
        double[] tall = {-3, 0, 2, 3, 1.5, 3};
        assertNotNull(CAR.bodyPush(0, 0, 0, 0, List.of(GROUND, SLAB, tall), 0.6));
        // Né un gradino lontano dal muro.
        double[] farSlab = {-3, 0, -2, 3, 0.5, -1};
        assertNotNull(CAR.bodyPush(0, 0, 0, 0, List.of(GROUND, farSlab, BLOCK), 0.6));
    }

    @Test
    void entrareLanciatiSulFondoLentoNonEUnMuro() {
        VehicleDef def = VehicleDef.CHARGER_BLACK;
        Surface slow = new Surface("test_slow", 1.0, 0.5, Surface.PAVED.rollFactor());
        DriveState s = DriveState.rest(0);
        for (int i = 0; i < 1500; i++) s = DrivingModel.step(def, s, new DriveInput(0.5, 0, false), Surface.PAVED, true);
        DriveState next = DrivingModel.step(def, s, new DriveInput(0, 0, false), slow, true);
        double decelG = (s.forwardSpeed() - next.forwardSpeed()) * DrivingModel.TPS * DrivingModel.TPS / 9.81;
        assertTrue(decelG > 0.5, "rallenta davvero: " + decelG);
        assertTrue(decelG < 1.2, "ma non come contro un muro: " + decelG);
    }

    @Test
    void sottoLaPuntaDelFondoNonCambiaNulla() {
        VehicleDef def = VehicleDef.CHARGER_BLACK;
        Surface slow = new Surface("test_slow", 1.0, 0.5, Surface.PAVED.rollFactor());
        DriveState a = DriveState.rest(0);
        DriveState b = DriveState.rest(0);
        for (int i = 0; i < 20; i++) {
            a = DrivingModel.step(def, a, new DriveInput(1, 0, false), Surface.PAVED, true);
            b = DrivingModel.step(def, b, new DriveInput(1, 0, false), slow, true);
        }
        assertEquals(a.forwardSpeed(), b.forwardSpeed(), 1e-12);
    }

    // ---- parametri ----

    @Test
    void parametriCheRomponoLaFisicaSonoRifiutati() {
        VehicleDef base = VehicleDef.CHARGER_BLACK;
        assertThrows(IllegalArgumentException.class, () -> VehicleParams.overlay(base, "{\"maxSteerDeg\":90}"));
        assertThrows(IllegalArgumentException.class, () -> VehicleParams.overlay(base, "{\"seats\":0.5}"));
        assertThrows(IllegalArgumentException.class,
                () -> VehicleParams.overlay(base, "{\"powertrain\":{\"launchRpm\":7000}}"));
        // I valori predefiniti restano validi.
        VehicleParams.overlay(VehicleDef.JEEP, VehicleParams.toJson(VehicleDef.JEEP));
        VehicleParams.overlay(base, VehicleParams.toJson(base));
    }
}
