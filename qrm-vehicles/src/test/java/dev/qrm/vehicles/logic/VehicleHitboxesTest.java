package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import org.junit.jupiter.api.Test;

class VehicleHitboxesTest {

    /** Auto di 2 x 5 blocchi (X da -16 a 16 px, Z da -40 a 40 px), alta 1,5; una zona sul cofano davanti. */
    private static final String GEO = """
            {"format_version":"1.12.0","minecraft:geometry":[{"description":{"identifier":"geometry.t",
            "texture_width":16,"texture_height":16},"bones":[
            {"name":"root","pivot":[0,0,0]},
            {"name":"hitbox","parent":"root","pivot":[0,0,0],"cubes":[{"origin":[-16,0,-40],"size":[32,24,80],"uv":[0,0]}]},
            {"name":"hit_hood","parent":"root","pivot":[0,0,0],"cubes":[{"origin":[-10,16,-40],"size":[20,8,20],"uv":[0,0]}]}
            ]}]}
            """;

    private static VehicleHitboxes load() {
        return VehicleHitboxes.parse(new StringReader(GEO));
    }

    @Test
    void leggeIngombroEZone() {
        VehicleHitboxes h = load();
        assertTrue(h.hasSolid());
        assertEquals(1, h.solid().size());
        assertEquals(java.util.List.of("hit_hood"), java.util.List.copyOf(h.zones().keySet()));
        assertEquals("hood", VehicleHitboxes.boneOfZone("hit_hood"));
        var b = h.solid().get(0);
        assertEquals(1.0, b.hx(), 1e-9);
        assertEquals(2.5, b.hz(), 1e-9);
        assertEquals(0.75, b.cy(), 1e-9);
    }

    @Test
    void senzaOsseDiHitboxNonCambiaNulla() {
        String plain = "{\"minecraft:geometry\":[{\"description\":{},\"bones\":[{\"name\":\"body\"}]}]}";
        VehicleHitboxes h = VehicleHitboxes.parse(new StringReader(plain));
        assertFalse(h.hasSolid());
        assertTrue(h.zones().isEmpty());
    }

    @Test
    void ilGiocatoreNonEntraNellIngombro() {
        VehicleHitboxes h = load();
        // Auto in (0,0,0) con muso a 0 (verso +Z): il lato destro/sinistro sta a x=±1. Giocatore a x=1.1 entra di 0.2.
        double[] push = h.pushOut(1.1, 0, 0, 0, 0, 0, 0);
        assertNotNull(push);
        assertEquals(0.2, Math.abs(push[0]), 1e-9);
        assertEquals(0, push[2], 1e-9);
        assertNull(h.pushOut(1.4, 0, 0, 0, 0, 0, 0));
    }

    @Test
    void l_ingombroRuotaColMuso() {
        VehicleHitboxes h = load();
        // Con il muso a 90 gradi l'auto è lunga lungo X: a x=2.4 si è sul muso (mezza lunghezza 2.5), a z=1.1 di fianco.
        assertNotNull(h.pushOut(2.4, 0, 0, 0, 0, 0, 90));
        assertNull(h.pushOut(0, 0, 2.0, 0, 0, 0, 90));
        assertNotNull(h.pushOut(0, 0, 1.1, 0, 0, 0, 90));
    }

    @Test
    void sopraUnGradinoBassoSiSaleMaUnMuroSiSpinge() {
        VehicleHitboxes h = load();
        double[] up = h.pushOut(0, 1.3, 0, 0, 0, 0, 0); // 0.2 sotto la cima (1.5): si sale
        assertNotNull(up);
        assertEquals(0.2, up[1], 1e-9);
        double[] wall = h.pushOut(0.9, 0.2, 0, 0, 0, 0, 0);
        assertNotNull(wall);
        assertEquals(0, wall[1], 1e-9);
        assertTrue(Math.abs(wall[0]) > 0);
        assertNull(h.pushOut(0, 1.5, 0, 0, 0, 0, 0)); // in piedi sul tetto
    }

    @Test
    void ilRaggioColpisceLaZonaSulCofano() {
        VehicleHitboxes h = load();
        // Cofano: X -0.625..0.625, Y 1..1.5, Z -2.5..-1.25 nel modello (davanti = -Z); con muso a 0 il davanti è +Z mondo.
        var hit = h.raycast(0, 1.75, 4, 0, -0.2, -1, 6, 0, 0, 0, 0);
        assertNotNull(hit);
        assertEquals("hit_hood", hit.zone());
        // Dal lato opposto (dietro l'auto, a z=-4) il raggio non lo prende.
        assertNull(h.raycast(0, 1.75, -4, 0, -0.2, 1, 6, 0, 0, 0, 0));
        // Fuori portata.
        assertNull(h.raycast(0, 1.75, 4, 0, -0.2, -1, 1, 0, 0, 0, 0));
    }

    @Test
    void ilRaggioSeguiIlMuso() {
        VehicleHitboxes h = load();
        // Muso a 90 gradi: il davanti punta a -X nel mondo (forward = (-sin h, cos h) = (-1, 0)).
        assertNotNull(h.raycast(-4, 1.75, 0, 1, -0.2, 0, 6, 0, 0, 0, 90));
        assertNull(h.raycast(4, 1.75, 0, -1, -0.2, 0, 6, 0, 0, 0, 90));
    }
    /** Un osso che ruota di 90 gradi attorno a Z col perno a 1 blocco d'altezza, e una zona appesa. */
    private static final String FLAP = """
            {"minecraft:geometry":[{"description":{},"bones":[
            {"name":"root","pivot":[0,0,0]},
            {"name":"flap","parent":"root","pivot":[0,16,0]},
            {"name":"hit_flap","parent":"flap","pivot":[0,16,0],"cubes":[{"origin":[-8,16,-8],"size":[16,16,16],"uv":[0,0]}]}
            ]}]}
            """;

    private static final java.util.Map<String, dev.qrm.client.api.rig.BoneTransform> OPEN = java.util.Map.of("flap",
            new dev.qrm.client.api.rig.BoneTransform(new float[] {0, 0, 0}, new float[] {0, 0, 90}, new float[] {1, 1, 1}));

    @Test
    void laZonaAppesaAUnOssoNeSegueLaRotazione() {
        VehicleHitboxes h = VehicleHitboxes.parse(new StringReader(FLAP));
        // Muso a 180: il modello coincide col mondo. Dall'alto, a x = 0,3 la zona a riposo c'è; aperta non c'è più.
        assertNotNull(h.raycast(0.3, 4, 0, 0, -1, 0, 8, 0, 0, 0, 180));
        assertNull(h.raycast(0.3, 4, 0, 0, -1, 0, 8, 0, 0, 0, 180, OPEN));
        // A x = -0,8 è il contrario: a riposo manca, aperta la zona ci arriva (x da -1 a 0, y da 0,5 a 1,5).
        assertNull(h.raycast(-0.8, 4, 0, 0, -1, 0, 8, 0, 0, 0, 180));
        assertNotNull(h.raycast(-0.8, 4, 0, 0, -1, 0, 8, 0, 0, 0, 180, OPEN));
    }

    @Test
    void conPosaVuotaLeZoneRestanoAdAvvenire() {
        VehicleHitboxes h = VehicleHitboxes.parse(new StringReader(FLAP));
        assertNotNull(h.raycast(0.3, 4, 0, 0, -1, 0, 8, 0, 0, 0, 180, java.util.Map.of()));
    }
}
