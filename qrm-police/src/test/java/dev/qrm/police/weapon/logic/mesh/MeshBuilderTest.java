package dev.qrm.police.weapon.logic.mesh;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MeshBuilderTest {

    private static final float EPS = 1e-4f;
    private static final Path ASSETS = Path.of("src/main/resources/assets/qrm_police");

    private static String face(String uv, String extra) {
        return "{\"uv\":" + uv + ",\"texture\":\"#0\"" + extra + "}";
    }

    private static String cube(String from, String to, String rotation, String faces) {
        return "{\"texture_size\":[64,64],\"elements\":[{\"from\":" + from + ",\"to\":" + to
                + (rotation == null ? "" : ",\"rotation\":" + rotation) + ",\"faces\":{" + faces + "}}]}";
    }

    private static String allFaces(String uv) {
        StringBuilder sb = new StringBuilder();
        for (String f : List.of("north", "east", "south", "west", "up", "down")) {
            if (sb.length() > 0) sb.append(',');
            sb.append('"').append(f).append("\":").append(face(uv, ""));
        }
        return sb.toString();
    }

    private static PartMesh parse(String json) {
        return MeshBuilder.parse(new StringReader(json));
    }

    private static void assertNormal(PartMesh.Quad q, float x, float y, float z) {
        assertEquals(x, q.nx(), EPS, "nx");
        assertEquals(y, q.ny(), EPS, "ny");
        assertEquals(z, q.nz(), EPS, "nz");
    }

    @Test
    void unitCubeGivesSixQuadsWithOutwardNormals() {
        PartMesh mesh = parse(cube("[0,0,0]", "[16,16,16]", null, allFaces("[0,0,16,16]")));
        assertEquals(6, mesh.quads().size());
        Set<String> normals = new HashSet<>();
        for (PartMesh.Quad q : mesh.quads()) {
            normals.add(Math.round(q.nx()) + "," + Math.round(q.ny()) + "," + Math.round(q.nz()));
            assertEquals(4, q.vertices().size());
            // Ogni vertice sta sulla faccia indicata dalla normale.
            for (PartMesh.Vertex v : q.vertices()) {
                float along = v.x() * q.nx() + v.y() * q.ny() + v.z() * q.nz();
                float expected = (q.nx() + q.ny() + q.nz()) > 0 ? 16f : 0f;
                assertEquals(expected, along, EPS);
            }
        }
        assertEquals(Set.of("0,0,-1", "1,0,0", "0,0,1", "-1,0,0", "0,1,0", "0,-1,0"), normals);
    }

    @Test
    void northFaceCornersFollowVanillaOrder() {
        PartMesh mesh = parse(cube("[0,0,0]", "[16,16,16]", null, "\"north\":" + face("[0,0,16,16]", "")));
        PartMesh.Quad q = mesh.quads().get(0);
        assertNormal(q, 0, 0, -1);
        float[][] expected = {{16, 16, 0}, {16, 0, 0}, {0, 0, 0}, {0, 16, 0}};
        for (int i = 0; i < 4; i++) {
            PartMesh.Vertex v = q.vertices().get(i);
            assertEquals(expected[i][0], v.x(), EPS);
            assertEquals(expected[i][1], v.y(), EPS);
            assertEquals(expected[i][2], v.z(), EPS);
        }
    }

    @Test
    void fullUvMapsToUnitSquareCorners() {
        PartMesh mesh = parse(cube("[0,0,0]", "[16,16,16]", null, "\"north\":" + face("[0,0,16,16]", "")));
        List<PartMesh.Vertex> vs = mesh.quads().get(0).vertices();
        // Ordine di Minecraft: (u1,v1) (u1,v2) (u2,v2) (u2,v1).
        float[][] uv = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        for (int i = 0; i < 4; i++) {
            assertEquals(uv[i][0], vs.get(i).u(), EPS);
            assertEquals(uv[i][1], vs.get(i).v(), EPS);
        }
    }

    @Test
    void partialUvIsScaledToWholeTexture() {
        PartMesh mesh = parse(cube("[0,0,0]", "[1,1,1]", null, "\"up\":" + face("[4,8,6,12]", "")));
        List<PartMesh.Vertex> vs = mesh.quads().get(0).vertices();
        assertEquals(0.25f, vs.get(0).u(), EPS);
        assertEquals(0.5f, vs.get(0).v(), EPS);
        assertEquals(0.375f, vs.get(2).u(), EPS);
        assertEquals(0.75f, vs.get(2).v(), EPS);
    }

    @Test
    void faceUvRotationShiftsCorners() {
        PartMesh mesh = parse(cube("[0,0,0]", "[16,16,16]", null,
                "\"north\":" + face("[0,0,16,16]", ",\"rotation\":90")));
        List<PartMesh.Vertex> vs = mesh.quads().get(0).vertices();
        // Con 90 gradi il vertice i prende l'angolo UV i+1.
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (int i = 0; i < 4; i++) {
            assertEquals(uv[i][0], vs.get(i).u(), EPS, "u" + i);
            assertEquals(uv[i][1], vs.get(i).v(), EPS, "v" + i);
        }
    }

    @Test
    void elementRotationOnYMovesNorthFaceToWestForPositiveAngle() {
        // Come Minecraft e Blockbench: angolo positivo antiorario visto dall'alto (+Y), quindi nord -> ovest.
        PartMesh mesh = parse(cube("[0,0,0]", "[16,16,16]", "{\"origin\":[8,8,8],\"axis\":\"y\",\"angle\":90}",
                "\"north\":" + face("[0,0,16,16]", "")));
        PartMesh.Quad q = mesh.quads().get(0);
        assertNormal(q, -1, 0, 0);
        for (PartMesh.Vertex v : q.vertices()) assertEquals(0f, v.x(), EPS);
    }

    @Test
    void elementRotationOnYMovesNorthFaceToEastForNegativeAngle() {
        PartMesh mesh = parse(cube("[0,0,0]", "[16,16,16]", "{\"origin\":[8,8,8],\"axis\":\"y\",\"angle\":-90}",
                "\"north\":" + face("[0,0,16,16]", "")));
        PartMesh.Quad q = mesh.quads().get(0);
        assertNormal(q, 1, 0, 0);
        for (PartMesh.Vertex v : q.vertices()) assertEquals(16f, v.x(), EPS);
    }

    @Test
    void arbitraryAngleRotatesAroundOrigin() {
        // Grilletto: 15 gradi su X attorno a (8,7,8.75). Il punto sull'origine resta fermo, la normale gira.
        PartMesh mesh = parse(cube("[7,7,8.75]", "[9,9,10]", "{\"origin\":[8,7,8.75],\"axis\":\"x\",\"angle\":15}",
                "\"north\":" + face("[0,0,1,1]", "")));
        PartMesh.Quad q = mesh.quads().get(0);
        double a = Math.toRadians(15);
        // Rotazione destrorsa su X: normale (0,0,-1) -> (0, sin a, -cos a).
        assertNormal(q, 0, (float) Math.sin(a), (float) -Math.cos(a));
        // Vertice 1 della faccia nord: (maxX, minY, minZ) = (9,7,8.75), sull'asse di rotazione.
        PartMesh.Vertex v1 = q.vertices().get(1);
        assertEquals(9f, v1.x(), EPS);
        assertEquals(7f, v1.y(), EPS);
        assertEquals(8.75f, v1.z(), EPS);
        // Vertice 0: (9,9,8.75) -> y = 7 + 2cos a, z = 8.75 + 2sin a... ruotando (0,2,0) su X: (0, 2cos a, 2sin a).
        PartMesh.Vertex v0 = q.vertices().get(0);
        assertEquals(7 + 2 * (float) Math.cos(a), v0.y(), EPS);
        assertEquals(8.75f + 2 * (float) Math.sin(a), v0.z(), EPS);
    }

    @Test
    void missingFacesAreSkipped() {
        PartMesh mesh = parse(cube("[0,0,0]", "[1,1,1]", null, "\"up\":" + face("[0,0,1,1]", "")));
        assertEquals(1, mesh.quads().size());
        assertNormal(mesh.quads().get(0), 0, 1, 0);
    }

    @Test
    void malformedElementIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("{\"elements\":[{\"to\":[1,1,1],\"faces\":{}}]}"));
    }

    @Test
    void rigKeepsParentsBeforeChildren() {
        String json = "{\"parts_prefix\":\"x:item/p/\",\"bones\":{"
                + "\"child\":{\"parent\":\"mid\",\"pivot\":[1,2,3],\"has_model\":true},"
                + "\"mid\":{\"parent\":\"root\",\"pivot\":[0,0,0],\"has_model\":true},"
                + "\"root\":{\"parent\":null,\"pivot\":[8,6,11],\"has_model\":false}}}";
        Rig rig = Rig.parse(new StringReader(json));
        assertEquals(List.of("root", "mid", "child"), rig.bones().stream().map(Rig.Bone::name).toList());
        assertNull(rig.bones().get(0).parent());
        assertFalse(rig.bones().get(0).hasModel());
        assertArrayEquals(new float[] {1, 2, 3}, rig.bone("child").orElseThrow().pivot());
        assertEquals("x:item/p/", rig.partsPrefix());
    }

    @Test
    void rigRejectsUnknownParentAndCycles() {
        assertThrows(IllegalArgumentException.class, () -> Rig.parse(new StringReader(
                "{\"bones\":{\"a\":{\"parent\":\"ghost\",\"pivot\":[0,0,0],\"has_model\":true}}}")));
        assertThrows(IllegalArgumentException.class, () -> Rig.parse(new StringReader(
                "{\"bones\":{\"a\":{\"parent\":\"b\",\"pivot\":[0,0,0]},\"b\":{\"parent\":\"a\",\"pivot\":[0,0,0]}}}")));
    }

    @Test
    void shippedRigAndPartsLoad() throws IOException {
        Rig rig;
        try (Reader r = Files.newBufferedReader(ASSETS.resolve("rigs/police_pistol.json"), StandardCharsets.UTF_8)) {
            rig = Rig.parse(r);
        }
        assertEquals(14, rig.bones().size());
        assertEquals("root", rig.bones().get(0).name());
        int parts = 0;
        for (Rig.Bone bone : rig.bones()) {
            if (!bone.hasModel()) continue;
            Path part = ASSETS.resolve("models/item/police_pistol_parts/" + bone.name() + ".json");
            try (Reader r = Files.newBufferedReader(part, StandardCharsets.UTF_8)) {
                assertFalse(MeshBuilder.parse(r).quads().isEmpty(), bone.name());
            }
            parts++;
        }
        assertEquals(13, parts);
    }
}
