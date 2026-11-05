package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Parametri di guida in JSON: andata e ritorno, sovrascritture parziali e file sbagliati. */
class VehicleParamsTest {

    @AfterEach
    void reset() {
        VehicleDef.clearOverrides();
        Surface.clearOverrides();
    }

    @Test
    void ilJsonScrittoRiproduceIlVeicolo() {
        for (VehicleDef def : VehicleDef.ALL) {
            VehicleDef back = VehicleParams.overlay(def, VehicleParams.toJson(def));
            assertEquals(def.seats(), back.seats());
            assertEquals(def.maxSpeed(), back.maxSpeed());
            assertEquals(def.wheelBase(), back.wheelBase());
            assertEquals(def.powertrain().massKg(), back.powertrain().massKg());
            assertArrayEquals(def.powertrain().torqueNm(), back.powertrain().torqueNm());
            assertArrayEquals(def.powertrain().gearRatios(), back.powertrain().gearRatios());
            assertEquals(def.powertrain().rearCornering(), back.powertrain().rearCornering());
        }
    }

    @Test
    void unFileParzialeCambiaSoloIlDichiarato() {
        VehicleDef base = VehicleDef.CHARGER_BLACK;
        VehicleDef d = VehicleParams.overlay(base, "{\"powertrain\":{\"massKg\":2000},\"maxSpeed\":0.6}");
        assertEquals(2000, d.powertrain().massKg());
        assertEquals(0.6, d.maxSpeed());
        assertEquals(base.powertrain().tyreMu(), d.powertrain().tyreMu());
        assertEquals(base.id(), d.id());
    }

    @Test
    void unVeicoloPiuPesanteAccelerarPiuLentamente() {
        VehicleDef base = VehicleDef.CHARGER_BLACK;
        VehicleDef heavy = VehicleParams.overlay(base, "{\"powertrain\":{\"massKg\":3400}}");
        DriveState a = DriveState.rest(0);
        DriveState b = DriveState.rest(0);
        for (int i = 0; i < 40; i++) {
            a = DrivingModel.step(base, a, new DriveInput(1, 0, false), Surface.PAVED, true);
            b = DrivingModel.step(heavy, b, new DriveInput(1, 0, false), Surface.PAVED, true);
        }
        assertTrue(b.speed() < a.speed(), "pesante " + b.speed() + " contro " + a.speed());
    }

    @Test
    void valoriNonAmmessiSonoRifiutatiConUnMessaggio() {
        VehicleDef base = VehicleDef.JEEP;
        assertThrows(IllegalArgumentException.class, () -> VehicleParams.overlay(base, "non json"));
        assertThrows(IllegalArgumentException.class, () -> VehicleParams.overlay(base, "[]"));
        assertThrows(IllegalArgumentException.class, () -> VehicleParams.overlay(base, "{\"maxSpeed\":-1}"));
        assertThrows(IllegalArgumentException.class, () -> VehicleParams.overlay(base, "{\"maxSpeed\":\"veloce\"}"));
        assertThrows(IllegalArgumentException.class,
                () -> VehicleParams.overlay(base, "{\"powertrain\":{\"rpm\":[3000,2000],\"torqueNm\":[1,2]}}"));
        assertThrows(IllegalArgumentException.class,
                () -> VehicleParams.overlay(base, "{\"powertrain\":{\"rpm\":[1000,2000],\"torqueNm\":[1]}}"));
        assertThrows(IllegalArgumentException.class,
                () -> VehicleParams.overlay(base, "{\"powertrain\":{\"efficiency\":1.5}}"));
        assertThrows(IllegalArgumentException.class,
                () -> VehicleParams.overlay(base, "{\"powertrain\":{\"shiftUpRpm\":1000,\"shiftDownRpm\":2000}}"));
    }

    @Test
    void sovrascrivereUnVeicoloLoRendeVisibileDaById() {
        VehicleDef tuned = VehicleParams.overlay(VehicleDef.JEEP, "{\"maxSpeed\":0.5}");
        VehicleDef.override(tuned);
        assertEquals(0.5, VehicleDef.byId("jeep_wrangler").orElseThrow().maxSpeed());
        VehicleDef.clearOverrides();
        assertEquals(VehicleDef.JEEP.maxSpeed(), VehicleDef.byId("jeep_wrangler").orElseThrow().maxSpeed());
    }

    @Test
    void iFondiSiLeggonoESiSovrascrivono() {
        String json = VehicleParams.surfacesToJson(List.of(Surface.values()));
        assertEquals(4, VehicleParams.parseSurfaces(json).size());
        List<Surface> parsed = VehicleParams.parseSurfaces("{\"dirt\":{\"grip\":0.5}}");
        assertEquals(1, parsed.size());
        assertEquals(0.5, parsed.get(0).gripFactor());
        assertEquals(Surface.DIRT.rollFactor(), parsed.get(0).rollFactor());
        Surface.override(parsed.get(0));
        assertEquals(0.5, Surface.get("dirt").gripFactor());
        assertEquals(1.0, Surface.get("sconosciuto").gripFactor());
        assertThrows(IllegalArgumentException.class, () -> VehicleParams.parseSurfaces("{\"dirt\":{\"grip\":0}}"));
    }

    @Test
    void ilGhiaccioFaScivolareDiPiuDellAsfalto() {
        VehicleDef def = VehicleDef.CHARGER_BLACK;
        DriveInput in = new DriveInput(0.5, 1, false);
        double paved = turnRate(def, Surface.PAVED, in);
        double ice = turnRate(def, Surface.SLIPPERY, in);
        assertTrue(ice < paved, "ghiaccio " + ice + ", asfalto " + paved);
    }

    private static double turnRate(VehicleDef def, Surface surface, DriveInput in) {
        DriveState s = new DriveState(0, 0, 0, 0.6);
        double peak = 0;
        for (int i = 0; i < 40; i++) {
            s = DrivingModel.step(def, s, in, surface, true);
            peak = Math.max(peak, Math.abs(s.yawRate()));
        }
        return peak;
    }
}
