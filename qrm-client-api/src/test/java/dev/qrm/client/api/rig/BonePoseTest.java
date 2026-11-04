package dev.qrm.client.api.rig;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class BonePoseTest {

    private static final float EPS = 1e-5f;

    private static Rig rig() {
        return Rig.parse(new StringReader("""
                {"parts_prefix":"qrm_police:item/p/","bones":{
                  "root":{"parent":null,"pivot":[8,6,11],"has_model":false},
                  "slide":{"parent":"root","pivot":[8,10,8],"has_model":true},
                  "sights":{"parent":"slide","pivot":[8,11,8],"has_model":true}
                }}"""));
    }

    private static Vector3f apply(Matrix4f m, float x, float y, float z) {
        return m.transformPosition(new Vector3f(x, y, z));
    }

    @Test
    void aRiposoTutteLeMatriciSonoIdentita() {
        Map<String, Matrix4f> m = BonePose.compose(rig(), Map.of());
        for (Matrix4f mat : m.values()) assertTrue(mat.equals(new Matrix4f(), EPS));
        assertEquals(3, m.size());
    }

    @Test
    void laPosizioneEInUnitaBlockbench() {
        BoneTransform t = new BoneTransform(new float[] {0, 0, 2.5f}, new float[] {0, 0, 0}, new float[] {1, 1, 1});
        Map<String, Matrix4f> m = BonePose.compose(rig(), Map.of("slide", t));
        Vector3f p = apply(m.get("slide"), 0.5f, 0.5f, 0.5f);
        assertEquals(0.5f + 2.5f / 16f, p.z, EPS);
        // Il figlio eredita la traslazione del genitore.
        Vector3f s = apply(m.get("sights"), 0.5f, 0.5f, 0.5f);
        assertEquals(0.5f + 2.5f / 16f, s.z, EPS);
        // Il genitore non si muove.
        assertTrue(m.get("root").equals(new Matrix4f(), EPS));
    }

    @Test
    void laRotazioneLasciaFermoIlPerno() {
        BoneTransform t = new BoneTransform(new float[] {0, 0, 0}, new float[] {30, 20, 10}, new float[] {1, 1, 1});
        Map<String, Matrix4f> m = BonePose.compose(rig(), Map.of("slide", t));
        Vector3f pivot = apply(m.get("slide"), 8 / 16f, 10 / 16f, 8 / 16f);
        assertEquals(8 / 16f, pivot.x, EPS);
        assertEquals(10 / 16f, pivot.y, EPS);
        assertEquals(8 / 16f, pivot.z, EPS);
    }

    @Test
    void rotazioneSuXDestrorsa() {
        // +90 su X attorno al perno, regola della mano destra: un punto davanti al perno (0,0,-1) va in alto (0,1,0).
        BoneTransform t = new BoneTransform(new float[] {0, 0, 0}, new float[] {90, 0, 0}, new float[] {1, 1, 1});
        Map<String, Matrix4f> m = BonePose.compose(rig(), Map.of("root", t));
        Vector3f p = apply(m.get("root"), 8 / 16f, 6 / 16f, 11 / 16f - 1f);
        assertEquals(6 / 16f + 1f, p.y, EPS);
        assertEquals(11 / 16f, p.z, EPS);
    }

    @Test
    void ordineZYX() {
        // Rz·Ry·Rx: prima X, poi Y, poi Z applicati al punto.
        BoneTransform t = new BoneTransform(new float[] {0, 0, 0}, new float[] {90, 90, 0}, new float[] {1, 1, 1});
        Map<String, Matrix4f> m = BonePose.compose(rig(), Map.of("root", t));
        // (0,0,-1) → X+90 → (0,1,0) → Y+90 → (0,1,0)
        Vector3f p = apply(m.get("root"), 8 / 16f, 6 / 16f, 11 / 16f - 1f);
        assertEquals(8 / 16f, p.x, EPS);
        assertEquals(6 / 16f + 1f, p.y, EPS);
        // (1,0,0) → X+90 → (1,0,0) → Y+90 → (0,0,-1)
        Vector3f q = apply(m.get("root"), 8 / 16f + 1f, 6 / 16f, 11 / 16f);
        assertEquals(11 / 16f - 1f, q.z, EPS);
    }

    @Test
    void scalaZeroSchiacciaSulPerno() {
        BoneTransform t = new BoneTransform(new float[] {0, 0, 0}, new float[] {0, 0, 0}, new float[] {0, 0, 0});
        Map<String, Matrix4f> m = BonePose.compose(rig(), Map.of("slide", t));
        Vector3f p = apply(m.get("sights"), 0.9f, 0.1f, 0.3f);
        assertEquals(8 / 16f, p.x, EPS);
        assertEquals(10 / 16f, p.y, EPS);
        assertEquals(8 / 16f, p.z, EPS);
    }

    @Test
    void cornicePerLeBracciaNelPerno() {
        Map<String, Matrix4f> m = BonePose.compose(rig(), Map.of());
        Matrix4f frame = BonePose.pivotFrame(rig().bone("slide").orElseThrow(), m.get("slide"));
        Vector3f o = apply(frame, 0, 0, 0);
        assertEquals(8 / 16f, o.x, EPS);
        assertEquals(10 / 16f, o.y, EPS);
        assertEquals(8 / 16f, o.z, EPS);
    }
}
