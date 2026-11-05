package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.qrm.client.api.rig.BoneTransform;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VehiclePoseTest {

    private static final float EPS = 1e-4f;

    @Test
    void sterzataADestraGiraLeRuoteAnterioriVersoDestra() {
        // Davanti = -Z: per portare il muso della ruota verso +X (destra) serve una rotazione negativa attorno a Y.
        Map<String, BoneTransform> p = VehiclePose.pose(0, 20, 0, 0);
        assertArrayEquals(new float[] {0, -20, 0}, p.get("steer_fl").rotationDeg(), EPS);
        assertArrayEquals(new float[] {0, -20, 0}, p.get("steer_fr").rotationDeg(), EPS);
    }

    @Test
    void lePosterioriNonSterzano() {
        Map<String, BoneTransform> p = VehiclePose.pose(0, 20, 0, 0);
        assertTrue(!p.containsKey("wheel_rl") || p.get("wheel_rl").rotationDeg()[1] == 0);
    }

    @Test
    void ilRotolareDellaRuotaAvantiPortaLaCimaVersoIlDavanti() {
        // La cima della ruota va verso -Z (davanti): rotazione negativa attorno a X.
        Map<String, BoneTransform> p = VehiclePose.pose(90, 0, 0, 0);
        for (String w : new String[] {"wheel_fl", "wheel_fr", "wheel_rl", "wheel_rr"}) {
            assertArrayEquals(new float[] {-90, 0, 0}, p.get(w).rotationDeg(), EPS);
        }
    }

    @Test
    void beccheggioERolioVannoSulCorpo() {
        Map<String, BoneTransform> p = VehiclePose.pose(0, 0, 2, -3);
        assertArrayEquals(new float[] {2, 0, -3}, p.get("body").rotationDeg(), EPS);
    }

    @Test
    void aRiposoLaPosaECompletaMaNeutra() {
        Map<String, BoneTransform> p = VehiclePose.pose(0, 0, 0, 0);
        for (String bone : new String[] {"body", "steer_fl", "steer_fr", "wheel_fl", "wheel_fr", "wheel_rl", "wheel_rr"}) {
            assertArrayEquals(new float[] {0, 0, 0}, p.get(bone).rotationDeg(), EPS);
        }
    }
}
