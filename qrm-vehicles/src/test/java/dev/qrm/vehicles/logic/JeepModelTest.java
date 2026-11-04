package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.qrm.client.api.rig.GeoModel;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Il contratto tra il modello della Jeep e il codice: ossa e locator su cui contano guida e rendering. */
class JeepModelTest {

    private static GeoModel load() throws Exception {
        try (InputStream in = JeepModelTest.class.getResourceAsStream("/assets/qrm_vehicles/geo/jeep_wrangler.geo.json");
                Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return GeoModel.parse(r);
        }
    }

    @Test
    void ilModelloSiLeggeEHaLeOssaAttese() throws Exception {
        GeoModel m = load();
        // 115 ossa nel file di design; la copia negli asset ha una in meno: un gruppo vuoto "shifter" duplicato, tolto
        // perché il parser vuole nomi unici.
        assertEquals(114, m.rig().bones().size());
        for (String name : new String[] {"body", "steer_fl", "steer_fr", "wheel_fl", "wheel_fr", "wheel_rl",
                "wheel_rr", "door_l", "door_r"}) {
            assertTrue(m.rig().bone(name).isPresent(), "manca l'osso " + name);
        }
        assertEquals(512, m.textureWidth());
        assertEquals(256, m.textureHeight());
    }

    @Test
    void iSediliDelCodiceCoincidonoConIlModello() throws Exception {
        GeoModel m = load();
        assertArrayEquals(m.locators().get("driver_hip"), VehicleSeats.JEEP.get(0), 1e-4f);
        assertArrayEquals(m.locators().get("passenger_hip"), VehicleSeats.JEEP.get(1), 1e-4f);
        assertEquals(VehicleDef.JEEP.seats(), VehicleSeats.JEEP.size());
    }

    @Test
    void iLocatorSonoNelloSpazioDiGioco() throws Exception {
        GeoModel m = load();
        // Nel file il guidatore sta a +X (sinistra del veicolo); nel gioco X è specchiata: guida a sinistra = -X.
        assertArrayEquals(new float[] {-6.5f, 27f, 3f}, m.locators().get("driver_eye"), 1e-4f);
        assertArrayEquals(new float[] {-6.5f, 10f, 3f}, m.locators().get("driver_hip"), 1e-4f);
        assertArrayEquals(new float[] {6.5f, 10f, 3f}, m.locators().get("passenger_hip"), 1e-4f);
        assertArrayEquals(new float[] {0f, 12f, -18f}, m.locators().get("engine_sound"), 1e-4f);
        assertArrayEquals(new float[] {5f, 4f, 30f}, m.locators().get("exhaust_tip"), 1e-4f);
    }
}
