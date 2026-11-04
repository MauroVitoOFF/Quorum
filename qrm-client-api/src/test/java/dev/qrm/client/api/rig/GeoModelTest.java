package dev.qrm.client.api.rig;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeoModelTest {

    private static final float EPS = 1e-4f;

    /** Un solo cubo 2x2x2 con origine (-1, 0, -1) e UV (0, 0), texture 64x32. */
    private static String geo(String bones) {
        return "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"geometry.t\","
                + "\"texture_width\":64,\"texture_height\":32},\"bones\":[" + bones + "]}]}";
    }

    private static final String CUBE = "{\"name\":\"a\",\"pivot\":[0,0,0],\"cubes\":[{\"origin\":[-1,0,-1],"
            + "\"size\":[2,2,2],\"uv\":[0,0]}]}";

    private static GeoModel parse(String bones) {
        return GeoModel.parse(new StringReader(geo(bones)));
    }

    @Test
    void cuboHaSeiFacceConNormaliVersoLEsterno() {
        PartMesh mesh = parse(CUBE).meshes().get("a");
        assertEquals(6, mesh.quads().size());
        float[] c = {0, 1, 0}; // centro del cubo (simmetrico in X, quindi lo specchio non lo sposta)
        for (PartMesh.Quad q : mesh.quads()) {
            float cx = 0, cy = 0, cz = 0;
            for (PartMesh.Vertex v : q.vertices()) {
                cx += v.x() / 4;
                cy += v.y() / 4;
                cz += v.z() / 4;
            }
            float dot = q.nx() * (cx - c[0]) + q.ny() * (cy - c[1]) + q.nz() * (cz - c[2]);
            assertTrue(dot > 0, "normale verso l'interno su " + q);
        }
    }

    @Test
    void inflateAllargaIlCubo() {
        String inflated = CUBE.replace("\"uv\":[0,0]", "\"uv\":[0,0],\"inflate\":0.5");
        PartMesh mesh = parse(inflated).meshes().get("a");
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE;
        for (PartMesh.Quad q : mesh.quads()) {
            for (PartMesh.Vertex v : q.vertices()) {
                minX = Math.min(minX, v.x());
                maxX = Math.max(maxX, v.x());
                minY = Math.min(minY, v.y());
            }
        }
        assertEquals(-1.5f, minX, EPS);
        assertEquals(1.5f, maxX, EPS);
        assertEquals(-0.5f, minY, EPS);
    }

    @Test
    void assiXSpecchiato() {
        String body = "{\"name\":\"w\",\"pivot\":[11,6.5,-18],\"locators\":{\"eye\":[-6.5,24.5,-1.5]}}";
        GeoModel m = parse(body);
        assertArrayEquals(new float[] {-11, 6.5f, -18}, m.rig().bone("w").orElseThrow().pivot(), EPS);
        assertArrayEquals(new float[] {6.5f, 24.5f, -1.5f}, m.locators().get("eye"), EPS);
    }

    @Test
    void cuboNonSimmetricoSiSpecchia() {
        // origine X 3, larghezza 2: nel file occupa [3,5], nel gioco [-5,-3].
        String body = "{\"name\":\"a\",\"pivot\":[0,0,0],\"cubes\":[{\"origin\":[3,0,0],\"size\":[2,2,2],\"uv\":[0,0]}]}";
        PartMesh mesh = parse(body).meshes().get("a");
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        for (PartMesh.Quad q : mesh.quads()) {
            for (PartMesh.Vertex v : q.vertices()) {
                minX = Math.min(minX, v.x());
                maxX = Math.max(maxX, v.x());
            }
        }
        assertEquals(-5f, minX, EPS);
        assertEquals(-3f, maxX, EPS);
    }

    @Test
    void uvAScatola() {
        // Cubo 2x2x2 con uv (0,0), texture 64x32: la faccia nord sta a u in [2,4], v in [2,4] pixel.
        PartMesh mesh = parse(CUBE).meshes().get("a");
        PartMesh.Quad north = null;
        for (PartMesh.Quad q : mesh.quads()) if (q.nz() < -0.5f) north = q;
        float minU = Float.MAX_VALUE, maxU = -Float.MAX_VALUE, minV = Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
        for (PartMesh.Vertex v : north.vertices()) {
            minU = Math.min(minU, v.u());
            maxU = Math.max(maxU, v.u());
            minV = Math.min(minV, v.v());
            maxV = Math.max(maxV, v.v());
        }
        assertEquals(2f / 64f, minU, EPS);
        assertEquals(4f / 64f, maxU, EPS);
        assertEquals(2f / 32f, minV, EPS);
        assertEquals(4f / 32f, maxV, EPS);
    }

    @Test
    void ossaOrdinateGenitorePrimaESenzaCubiSenzaModello() {
        String bones = "{\"name\":\"child\",\"parent\":\"root\",\"pivot\":[0,0,0]},"
                + "{\"name\":\"root\",\"pivot\":[0,0,0]}";
        GeoModel m = parse(bones);
        List<Rig.Bone> list = m.rig().bones();
        assertEquals("root", list.get(0).name());
        assertEquals("child", list.get(1).name());
        assertFalse(list.get(0).hasModel());
        assertFalse(m.meshes().containsKey("root"));
        assertEquals(64, m.textureWidth());
        assertEquals(32, m.textureHeight());
    }

    @Test
    void ossaConCubiHannoModello() {
        assertTrue(parse(CUBE).rig().bone("a").orElseThrow().hasModel());
    }

    @Test
    void rifiutaFeatureNonSupportate() {
        String rotated = CUBE.replace("\"uv\":[0,0]", "\"uv\":[0,0],\"rotation\":[0,45,0]");
        String perFace = CUBE.replace("\"uv\":[0,0]", "\"uv\":{\"north\":{\"uv\":[0,0],\"uv_size\":[1,1]}}");
        String mirrored = CUBE.replace("\"uv\":[0,0]", "\"uv\":[0,0],\"mirror\":true");
        for (String bad : new String[] {rotated, perFace, mirrored}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> parse(bad));
            assertTrue(e.getMessage().contains("'a'"), e.getMessage());
        }
    }
}
