package dev.qrm.police.weapon.logic.anim;

import dev.qrm.client.api.rig.BoneTransform;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnimEvaluatorTest {

    private static AnimClip clip(String interp, double[][] keys) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"t\":").append(keys[i][0]).append(",\"v\":[").append(keys[i][1]).append(",0,0],\"interp\":\"")
                    .append(interp).append("\"}");
        }
        String json = "{\"format\":\"qrm.anim/2\",\"animations\":{\"c\":{\"length\":4,\"loop\":false,\"hold_last\":true,"
                + "\"bones\":{\"b\":{\"position\":[" + sb + "]}},\"events\":[]}}}";
        return AnimFormat.parse(new StringReader(json)).get("c");
    }

    private static float x(AnimClip c, double t) {
        return AnimEvaluator.sample(c, t).get("b").position()[0];
    }

    @Test
    void linearAMeta() {
        AnimClip c = clip("linear", new double[][] {{0, 0}, {1, 10}});
        assertEquals(5f, x(c, 0.5), 1e-5);
    }

    @Test
    void stepRestaAlValorePrecedente() {
        AnimClip c = clip("step", new double[][] {{0, 0}, {1, 10}});
        assertEquals(0f, x(c, 0.99), 0f);
        assertEquals(10f, x(c, 1.0), 0f);
    }

    @Test
    void catmullRomPassaPerIKeyframeEVale11_25() {
        AnimClip c = clip("catmullrom", new double[][] {{0, 0}, {1, 10}, {2, 10}, {3, 0}});
        assertEquals(0f, x(c, 0), 1e-5);
        assertEquals(10f, x(c, 1), 1e-5);
        assertEquals(10f, x(c, 2), 1e-5);
        assertEquals(0f, x(c, 3), 1e-5);
        assertEquals(11.25f, x(c, 1.5), 1e-4);
    }

    @Test
    void primaDelPrimoKeyframeRiposoDopoUltimoValore() {
        AnimClip c = clip("linear", new double[][] {{1, 4}, {2, 8}});
        assertEquals(0f, x(c, 0.5), 0f);
        assertEquals(8f, x(c, 5), 0f);
    }

    @Test
    void keyframeSingolo() {
        AnimClip c = clip("linear", new double[][] {{1, 4}});
        assertEquals(0f, x(c, 0.5), 0f);
        assertEquals(4f, x(c, 9), 0f);
    }

    @Test
    void canaleAssenteERiposoEOssoSenzaCanaliAssente() {
        AnimClip c = clip("linear", new double[][] {{0, 0}, {1, 10}});
        BoneTransform b = AnimEvaluator.sample(c, 0.5).get("b");
        assertArrayEquals(new float[] {0, 0, 0}, b.rotationDeg());
        assertArrayEquals(new float[] {1, 1, 1}, b.scale());
        assertNull(AnimEvaluator.sample(c, 0.5).get("altro"));
    }

    @Test
    void blendAMeta() {
        Map<String, BoneTransform> a = Map.of("b",
                new BoneTransform(new float[] {0, 0, 0}, new float[] {0, 0, 0}, new float[] {1, 1, 1}));
        Map<String, BoneTransform> b = Map.of("b",
                new BoneTransform(new float[] {10, 0, 0}, new float[] {0, 90, 0}, new float[] {3, 3, 3}));
        BoneTransform m = AnimEvaluator.blend(a, b, 0.5f).get("b");
        assertEquals(5f, m.position()[0], 1e-5);
        assertEquals(45f, m.rotationDeg()[1], 1e-5);
        assertEquals(2f, m.scale()[0], 1e-5);
    }

    @Test
    void blendOssoAssenteDaUnLatoVaVersoRiposo() {
        Map<String, BoneTransform> b = Map.of("b",
                new BoneTransform(new float[] {10, 0, 0}, new float[] {0, 0, 0}, new float[] {1, 1, 1}));
        assertEquals(5f, AnimEvaluator.blend(Map.of(), b, 0.5f).get("b").position()[0], 1e-5);
    }

    @Test
    void restNonSiAlteraDallEsterno() {
        BoneTransform.REST.scale()[0] = 9f;
        BoneTransform.REST.position()[1] = 9f;
        assertEquals(1f, BoneTransform.REST.scale()[0]);
        assertEquals(0f, BoneTransform.REST.position()[1]);
    }

    @Test
    void formatoSconosciutoNominaIlFormato() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AnimFormat.parse(new StringReader("{\"format\":\"qrm.anim/1\",\"animations\":{}}")));
        assertTrue(e.getMessage().contains("qrm.anim/1"));
    }

    @Test
    void leggeIlFileEsportatoDellaPistola() throws IOException {
        try (Reader r = new InputStreamReader(
                getClass().getResourceAsStream("/assets/qrm_police/animations/police_pistol.json"),
                StandardCharsets.UTF_8)) {
            Map<String, AnimClip> clips = AnimFormat.parse(r);
            assertTrue(clips.containsKey("fire"));
            AnimClip reload = clips.get("reload_tactical");
            assertNotNull(reload);
            List<AnimEvent> commits = reload.events().stream().filter(e -> e.type().equals("commit")).toList();
            assertEquals(2, commits.size());
            assertEquals(0.78, commits.get(0).t(), 1e-9);
            assertEquals(List.of("mag_out"), commits.get(0).args());
            assertEquals(1.58, commits.get(1).t(), 1e-9);
            assertEquals(List.of("mag_in"), commits.get(1).args());
            assertFalse(AnimEvaluator.sample(reload, 1.0).isEmpty());
        }
    }
}
