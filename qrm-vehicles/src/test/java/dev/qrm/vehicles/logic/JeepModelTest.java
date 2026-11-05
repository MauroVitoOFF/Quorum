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
        // 114 ossa negli asset + 3 ossa vetro separate (windshield_glass, door_l_glass, door_r_glass).
        assertEquals(117, m.rig().bones().size());
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
        assertArrayEquals(new float[] {-8f, 24.7f, 3.75f}, m.locators().get("driver_eye"), 1e-4f);
        assertArrayEquals(new float[] {-8f, 8.4f, 3.75f}, m.locators().get("driver_hip"), 1e-4f);
        assertArrayEquals(new float[] {8f, 8.4f, 3.75f}, m.locators().get("passenger_hip"), 1e-4f);
        assertArrayEquals(new float[] {0f, 12f, -22.5f}, m.locators().get("engine_sound"), 1e-4f);
        assertArrayEquals(new float[] {5.5f, 4f, 37.5f}, m.locators().get("exhaust_tip"), 1e-4f);
    }
}
