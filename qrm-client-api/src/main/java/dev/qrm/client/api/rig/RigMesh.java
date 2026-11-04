package dev.qrm.client.api.rig;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.qrm.client.api.rig.PartMesh;

/**
 * Mesh di una parte della pistola pronta da disegnare: i quad calcolati da {@link PartMesh} sono copiati in array
 * piatti alla ricarica delle risorse, così ogni fotogramma si limita a emettere vertici, senza leggere JSON.
 *
 * <p>Le posizioni sono emesse in blocchi (unità del modello / 16): chi disegna applica le trasformazioni dei bone
 * sul {@link PoseStack} usando anche i pivot del rig divisi per 16. La texture è quella del {@code RenderType}
 * scelto da chi invia la geometria (del {@code RenderType} scelto da chi disegna).
 */
public final class RigMesh {

    public static final RigMesh EMPTY = new RigMesh(PartMesh.EMPTY);

    private static final int WHITE = 0xFFFFFFFF;

    private final int quadCount;
    /** Per vertice: x, y, z (blocchi), u, v. */
    private final float[] vertices;
    /** Per quad: nx, ny, nz. */
    private final float[] normals;

    public RigMesh(PartMesh mesh) {
        this.quadCount = mesh.quads().size();
        this.vertices = new float[quadCount * 4 * 5];
        this.normals = new float[quadCount * 3];
        int v = 0;
        int n = 0;
        for (PartMesh.Quad q : mesh.quads()) {
            for (PartMesh.Vertex vx : q.vertices()) {
                vertices[v++] = vx.x() / 16f;
                vertices[v++] = vx.y() / 16f;
                vertices[v++] = vx.z() / 16f;
                vertices[v++] = vx.u();
                vertices[v++] = vx.v();
            }
            normals[n++] = q.nx();
            normals[n++] = q.ny();
            normals[n++] = q.nz();
        }
    }

    public int quadCount() {
        return quadCount;
    }

    /** Emette i quad della parte con la trasformazione corrente della pila. */
    public void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        render(poseStack.last(), consumer, light, overlay);
    }

    /**
     * Emette i quad con la posa indicata: è la forma che serve dentro
     * {@code SubmitNodeCollector.CustomGeometryRenderer#render(PoseStack.Pose, VertexConsumer)}.
     */
    public void render(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay) {
        int v = 0;
        for (int q = 0; q < quadCount; q++) {
            float nx = normals[q * 3];
            float ny = normals[q * 3 + 1];
            float nz = normals[q * 3 + 2];
            for (int i = 0; i < 4; i++) {
                consumer.addVertex(pose, vertices[v], vertices[v + 1], vertices[v + 2])
                        .setColor(WHITE)
                        .setUv(vertices[v + 3], vertices[v + 4])
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(pose, nx, ny, nz);
                v += 5;
            }
        }
    }
}
