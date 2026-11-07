package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.qrm.client.api.rig.BoneTransform;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VehiclePartsTest {

    private static float[] t(int flags, boolean lights, int engine, int gear) {
        return VehicleParts.targets(flags, lights, engine, gear);
    }

    @Test
    void leZoneAzionanoIlBitGiusto() {
        assertEquals(VehicleParts.DOOR_L, VehicleParts.bitOfZone("hit_door_handle_l"));
        assertEquals(VehicleParts.DOOR_R, VehicleParts.bitOfZone("hit_door_handle_r"));
        assertEquals(VehicleParts.HOOD, VehicleParts.bitOfZone("hit_hood"));
        assertEquals(VehicleParts.PARKING_BRAKE, VehicleParts.bitOfZone("hit_handbrake"));
        assertEquals(0, VehicleParts.bitOfZone("hit_light_switch"));
        assertEquals(0, VehicleParts.bitOfZone("hit_ignition_key"));
        assertEquals(0, VehicleParts.bitOfZone("hit_seat_0"));
    }

    @Test
    void ilCofanoSiApreInVentiquattroTick() {
        VehicleParts.Animator a = new VehicleParts.Animator();
        float[] open = t(VehicleParts.HOOD, false, 0, 0);
        for (int i = 0; i < 23; i++) a.tick(open);
        assertTrue(a.value(VehicleParts.CH_HOOD, 1f) < 1f);
        a.tick(open);
        assertEquals(1f, a.value(VehicleParts.CH_HOOD, 1f), 1e-4f);
        assertEquals(0f, a.value(VehicleParts.CH_DOOR_L, 1f), 0f);
    }

    @Test
    void sichiudeComeSiApreEPartialTickInterpola() {
        VehicleParts.Animator a = new VehicleParts.Animator();
        a.snap(t(VehicleParts.DOOR_L, false, 0, 0));
        assertEquals(1f, a.value(VehicleParts.CH_DOOR_L, 1f), 0f);
        a.tick(t(0, false, 0, 0));
        float half = a.value(VehicleParts.CH_DOOR_L, 0.5f);
        assertTrue(half < 1f && half > a.value(VehicleParts.CH_DOOR_L, 1f));
        for (int i = 0; i < 16; i++) a.tick(t(0, false, 0, 0));
        assertEquals(0f, a.value(VehicleParts.CH_DOOR_L, 1f), 1e-4f);
    }

    @Test
    void ilCanaleDeiFariSegueLInterruttore() {
        VehicleParts.Animator a = new VehicleParts.Animator();
        for (int i = 0; i < 3; i++) a.tick(t(0, true, 0, 0));
        assertEquals(1f, a.value(VehicleParts.CH_LIGHT_SWITCH, 1f), 1e-4f);
    }

    @Test
    void laPosaHaGliAngoliDelleAnimazioniDisegnate() {
        Map<String, BoneTransform> p = VehicleParts.pose(
                t(VehicleParts.DOOR_L | VehicleParts.DOOR_R | VehicleParts.HOOD | VehicleParts.PARKING_BRAKE
                        | VehicleParts.KEY_INSERTED, true, 1, 0));
        assertArrayEquals(new float[] {0, -62, 0}, p.get("door_l").rotationDeg(), 1e-3f);
        assertArrayEquals(new float[] {0, 62, 0}, p.get("door_r").rotationDeg(), 1e-3f);
        assertArrayEquals(new float[] {55, 0, 0}, p.get("hood").rotationDeg(), 1e-3f);
        assertArrayEquals(new float[] {25, 0, 0}, p.get("handbrake").rotationDeg(), 1e-3f);
        assertEquals(0.42f, p.get("light_switch").position()[2], 1e-4f);
        // Blocchetto su "acceso": -40 gradi come ignition_on.
        assertArrayEquals(new float[] {0, 0, -40}, p.get("ignition_key").rotationDeg(), 1e-3f);
    }

    @Test
    void laChiaveSparisceSeTolta() {
        Map<String, BoneTransform> p = VehicleParts.pose(t(0, false, 0, 0));
        assertArrayEquals(new float[] {0, 0, 0}, p.get("ignition_key").scale(), 0f);
        Map<String, BoneTransform> q = VehicleParts.pose(t(VehicleParts.KEY_INSERTED, false, 0, 0));
        assertFalse(q.containsKey("ignition_key"));
    }

    @Test
    void ilBlocchettoGiraDiPiuInAvviamento() {
        Map<String, BoneTransform> p = VehicleParts.pose(t(VehicleParts.KEY_INSERTED, false, 2, 0));
        assertArrayEquals(new float[] {0, 0, -70}, p.get("ignition_key").rotationDeg(), 1e-3f);
    }

    @Test
    void laLevaDelCambioHaGliAngoliDelleAnimazioni() {
        // Retromarcia 25, folle 15, prima 5, seconda -5.
        assertEquals(25f, VehicleParts.pose(t(0, false, 0, -1)).get("gear_shifter").rotationDeg()[0], 1e-3f);
        assertEquals(15f, VehicleParts.pose(t(0, false, 0, 0)).get("gear_shifter").rotationDeg()[0], 1e-3f);
        assertEquals(5f, VehicleParts.pose(t(0, false, 0, 1)).get("gear_shifter").rotationDeg()[0], 1e-3f);
        assertEquals(-5f, VehicleParts.pose(t(0, false, 0, 2)).get("gear_shifter").rotationDeg()[0], 1e-3f);
    }

    @Test
    void laMaschera() {
        assertFalse(VehicleParts.has(0, VehicleParts.HOOD));
        assertTrue(VehicleParts.has(VehicleParts.KEY_INSERTED, VehicleParts.KEY_INSERTED));
    }
}
