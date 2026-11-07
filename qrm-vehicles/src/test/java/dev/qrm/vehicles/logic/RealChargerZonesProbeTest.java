package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.qrm.client.api.rig.BoneTransform;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Le zone del modello vero: sono la geometria dei bone, e restano prendibili con cofano e portiera aperti. */
class RealChargerZonesProbeTest {

    private static VehicleHitboxes h;

    @BeforeAll
    static void load() throws Exception {
        try (Reader r = new InputStreamReader(RealChargerZonesProbeTest.class.getClassLoader()
                .getResourceAsStream("assets/qrm_vehicles/geo/dodge_charger_69_black.geo.json"), StandardCharsets.UTF_8)) {
            h = VehicleHitboxes.parse(r);
        }
    }

    private static Map<String, BoneTransform> opened(int flags) {
        return VehicleParts.pose(VehicleParts.targets(flags, false, 0, 0));
    }

    /** Raggio da (ox,oy,oz) verso (tx,ty,tz); veicolo all'origine, muso a 180 (modello = mondo). */
    private static VehicleHitboxes.Hit aim(double ox, double oy, double oz, double tx, double ty, double tz,
            Map<String, BoneTransform> pose, double margin) {
        return h.raycast(ox, oy, oz, tx - ox, ty - oy, tz - oz, 8, 0, 0, 0, 180, pose, margin);
    }

    @Test
    void laPortieraSiPrendePuntandolaMaManiglia() {
        // Maniglia (parte della portiera): x -1,20, y 0,94..0,97, z -0,08..0,07.
        var handle = aim(-4, 0.955, 0, -1.195, 0.955, 0, Map.of(), 0);
        assertNotNull(handle);
        assertEquals("hit_door_l", handle.zone());
    }

    @Test
    void ilFasciaDelMirinoPrendeLaChiaveDiStriscio() {
        // 4 cm di lato dalla chiave (che è larga 1-4 cm): il raggio sottile la manca, il fascio da 6 cm no.
        assertNull(aim(-0.215, 2.5, -0.605, -0.215, 0.82, -0.605, Map.of(), 0));
        var hit = aim(-0.215, 2.5, -0.605, -0.215, 0.82, -0.605, Map.of(), VehicleHitboxes.AIM_MARGIN);
        assertNotNull(hit);
        assertEquals("hit_ignition_key", hit.zone());
    }

    @Test
    void laManigliaAperteResteRichiudibile() {
        // Aperta, la portiera ha portato via la maniglia: dove stava non è più la portiera.
        double rearZ = h.outlines(Map.of()).stream().filter(o -> o.name().equals("hit_door_l"))
                .flatMap(o -> o.segments().stream()).mapToDouble(g -> Math.max(g[2], g[5])).max().orElseThrow();
        var old = aim(-4, 0.955, rearZ - 0.05, -1.195, 0.955, rearZ - 0.05, opened(VehicleParts.DOOR_L), 0);
        assertTrue(old == null || !old.zone().equals("hit_door_l"), String.valueOf(old));
    }

    @Test
    void ilCofanoSiPrendeInQualunquePuntoDellaLamiera() {
        for (double[] p : new double[][] {{0, 1.2, -1.2}, {0.7, 1.2, -2.4}, {-0.7, 1.2, -1.8}}) {
            var hit = aim(p[0], 2.8, p[2], p[0], p[1], p[2], Map.of(), 0);
            assertNotNull(hit, "mancato a " + java.util.Arrays.toString(p));
            assertEquals("hit_hood", hit.zone());
        }
        // Aperto, dove stava prima non è più il cofano.
        var old = aim(0, 2.8, -2.4, 0, 1.2, -2.4, opened(VehicleParts.HOOD), 0);
        assertTrue(old == null || !old.zone().equals("hit_hood"));
    }

    @Test
    void lePiccoleLevePiccoleSiPrendonoDallAlto() {
        var key = aim(-0.275, 2.5, -0.605, -0.275, 0.82, -0.605, Map.of(), VehicleHitboxes.AIM_MARGIN);
        assertNotNull(key);
        assertEquals("hit_ignition_key", key.zone());
        var sw = aim(-0.635, 2.5, -0.61, -0.635, 0.75, -0.61, Map.of(), VehicleHitboxes.AIM_MARGIN);
        assertNotNull(sw);
        assertEquals("hit_light_switch", sw.zone());
    }

    @Test
    void ilContornoDiDebugSegueLaPosa() {
        var rest = h.outlines(Map.of());
        assertTrue(rest.stream().anyMatch(o -> o.solid()));
        var door = rest.stream().filter(o -> o.name().equals("hit_door_l")).findFirst().orElseThrow();
        var open = h.outlines(opened(VehicleParts.DOOR_L)).stream().filter(o -> o.name().equals("hit_door_l"))
                .findFirst().orElseThrow();
        assertEquals(door.segments().size(), open.segments().size());
        assertTrue(!java.util.Arrays.equals(door.segments().get(0), open.segments().get(0)));
    }

    @Test
    void ilServerPrendeLePartiPiccoleAncheMirandoVicino() {
        // Chiave, cambio e freno a mano: il centro del raggio a 10 cm di distanza, entro la tolleranza del server.
        for (String zone : new String[] {"hit_ignition_key", "hit_gear_shifter", "hit_handbrake"}) {
            double[] c = h.center(zone);
            assertNotNull(c, zone);
            assertTrue(h.hitsZone(zone, c[0] + 0.1, c[1] + 1.5, c[2], 0, -1, 0, 8, 0, 0, 0, 180, Map.of(),
                    VehicleHitboxes.SERVER_MARGIN), zone);
            assertTrue(!h.hitsZone(zone, c[0] + 0.1, c[1] + 1.5, c[2], 0, -1, 0, 8, 0, 0, 0, 180, Map.of(), 0), zone);
        }
    }

    @Test
    void laChiaveToltaSiPrendeNellaSuaSede() {
        // Senza chiave (scala zero) si deve poter rimetterla.
        var hit = aim(-0.215, 2.5, -0.605, -0.215, 0.82, -0.605, VehicleParts.pose(VehicleParts.targets(0, false, 0, 0)),
                VehicleHitboxes.AIM_MARGIN);
        assertNotNull(hit);
        assertEquals("hit_ignition_key", hit.zone());
    }

    @Test
    void ISedileDiUnaZonaSonoQuelliVicini() {
        var seats = VehicleSeats.of(VehicleDef.CHARGER_BLACK);
        assertTrue(h.center("hit_seat_fl")[0] < 0 && seats.get(0)[0] < 0, "il sedile sinistro è il guidatore");
        assertTrue(h.center("hit_seat_fr")[0] > 0 && seats.get(1)[0] > 0);
    }

    @Test
    void laScoccaSiFermaControUnMuroAnchePerLaLunghezza() {
        // Muro davanti al muso (z da -4 a -2,9): la scocca arriva a z = -2,94 e ne entra di 0,04.
        var wall = java.util.List.of(new double[] {-5, 0, -4, 5, 3, -2.9});
        var push = h.bodyPush(0, 0, 0, 180, wall, 0.6);
        assertNotNull(push);
        assertEquals(0.04, push[1], 0.005);
        assertEquals(1.0, push[3], 1e-9);
        // Un gradino basso lo sale la ruota.
        assertNull(h.bodyPush(0, 0, 0, 180, java.util.List.of(new double[] {-5, 0, -4, 5, 0.4, -2.9}), 0.6));
        // Il muro di lato, a muso girato di 90°: la lunghezza ora punta di lato.
        var side = java.util.List.of(new double[] {-4, 0, -5, -2.9, 3, 5});
        assertNotNull(h.bodyPush(0, 0, 0, 90, side, 0.6));
        assertNull(h.bodyPush(0, 0, 0, 180, side, 0.6));
    }

    @Test
    void ilServerTollerailPuntamento() {
        // 15 cm sopra la maniglia: il fascio del client non ci arriva, quello del server sì.
        assertTrue(h.hitsZone("hit_door_l", -4, 1.15, 0, 2.8, -0.2, 0, 8, 0, 0, 0, 180, Map.of(),
                VehicleHitboxes.SERVER_MARGIN));
    }
}
