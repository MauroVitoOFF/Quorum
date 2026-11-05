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

/** Il contratto tra il modello della Charger nera e il codice: ossa, locator e sedili su cui contano guida e rendering. */
class ChargerModelTest {

    private static GeoModel load() throws Exception {
        try (InputStream in = ChargerModelTest.class
                        .getResourceAsStream("/assets/qrm_vehicles/geo/dodge_charger_69_black.geo.json");
                Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return GeoModel.parse(r);
        }
    }

    @Test
    void ilModelloHaLeOssaCheLaPoseUsa() throws Exception {
        GeoModel m = load();
        for (String name : new String[] {"root", "body", "steer_fl", "steer_fr", "wheel_fl", "wheel_fr", "wheel_rl",
                "wheel_rr", "hood", "pane_side_glass", "pane_windshield", "pane_rear_window"}) {
            assertTrue(m.rig().bone(name).isPresent(), "manca l'osso " + name);
        }
        assertEquals(512, m.textureWidth());
        assertEquals(256, m.textureHeight());
    }

    @Test
    void leRuoteHannoIlPernoNelCentro() throws Exception {
        GeoModel m = load();
        for (String wheel : new String[] {"wheel_fl", "wheel_fr", "wheel_rl", "wheel_rr"}) {
            float[] pivot = m.rig().bone(wheel).orElseThrow().pivot();
            assertEquals(7.8f, pivot[1], 1e-4f, wheel);
            assertEquals(16.2f, Math.abs(pivot[0]), 1e-4f, wheel);
        }
        // lo sterzo ruota attorno allo stesso punto della ruota
        assertArrayEquals(m.rig().bone("wheel_fl").orElseThrow().pivot(),
                m.rig().bone("steer_fl").orElseThrow().pivot(), 1e-4f);
        assertArrayEquals(m.rig().bone("wheel_fr").orElseThrow().pivot(),
                m.rig().bone("steer_fr").orElseThrow().pivot(), 1e-4f);
    }

    @Test
    void iSediliDelCodiceCoincidonoConIlModello() throws Exception {
        GeoModel m = load();
        assertArrayEquals(m.locators().get("driver_hip"), VehicleSeats.CHARGER_BLACK.get(0), 1e-4f);
        assertArrayEquals(m.locators().get("passenger_hip"), VehicleSeats.CHARGER_BLACK.get(1), 1e-4f);
        assertEquals(VehicleDef.CHARGER_BLACK.seats(), VehicleSeats.CHARGER_BLACK.size());
        assertEquals(VehicleSeats.CHARGER_BLACK, VehicleSeats.of(VehicleDef.CHARGER_BLACK));
        assertEquals(VehicleSeats.JEEP, VehicleSeats.of(VehicleDef.JEEP));
        assertTrue(m.locators().containsKey("engine_sound"));
        assertTrue(m.locators().containsKey("exhaust_tip"));
    }

    @Test
    void ilVeicoloSiTrovaPerIdELInterasseTorna() {
        assertEquals(VehicleDef.CHARGER_BLACK, VehicleDef.byId("dodge_charger_69_black").orElseThrow());
        // assi a z -28,8 e +27,6: 56,4 unità
        assertEquals(56.4 / 16.0, VehicleDef.CHARGER_BLACK.wheelBase(), 1e-9);
    }
}
