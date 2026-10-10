package dev.qrm.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.qrm.neoforge.net.PointEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Disegna sulle facce laterali di ogni blocco-punto vicino (tutte e quattro, o solo quella scelta dallo staff) una
 * targhetta 32x32, in alto a sinistra della faccia vista da fuori, come se fosse parte della texture del blocco. Non segue la camera: è fissa sul blocco, illuminata dalla
 * luce del mondo e nascosta da ciò che la copre.
 *
 * <p>La texture del tipo è {@code qrm:textures/point/<idTipo>.png}; se manca si usa {@code default.png}. Un resource
 * pack può sostituirle.
 */
public final class PointWorldRenderer {
    private static final double RANGE = 32.0;
    /** Lato della targhetta in blocchi (7/16) e distanza dai bordi alto e sinistro (1/16). */
    private static final float SIZE = 7F / 16F;
    private static final float MARGIN = 1F / 16F;
    /** Quanto la targhetta sporge dalla faccia, per non sovrapporsi alla texture del blocco (z-fighting). */
    private static final float LIFT = 0.004F;
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private static final Identifier DEFAULT_TEXTURE = Identifier.fromNamespaceAndPath("qrm", "textures/point/default.png");
    /** Esito della ricerca della texture per tipo; svuotato quando si lascia il server. */
    private static final Map<String, Identifier> TEXTURES = new HashMap<>();

    private PointWorldRenderer() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PointWorldRenderer::onSubmit);
    }

    public static void clearCache() {
        TEXTURES.clear();
    }

    private static void onSubmit(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        List<PointEntry> points = ClientState.points();
        if (mc.level == null || points.isEmpty()) return;
        String dimension = mc.level.dimension().identifier().toString();
        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        Vec3 eye = camera.pos;
        PoseStack pose = event.getPoseStack();
        for (PointEntry p : points) {
            if (!p.dimension().equals(dimension)) continue;
            double dx = p.x() + 0.5 - eye.x;
            double dy = p.y() + 0.5 - eye.y;
            double dz = p.z() + 0.5 - eye.z;
            if (dx * dx + dy * dy + dz * dz > RANGE * RANGE) continue;
            BlockPos block = new BlockPos(p.x(), p.y(), p.z());
            pose.pushPose();
            pose.translate(p.x() - eye.x, p.y() - eye.y, p.z() - eye.z);
            for (Direction side : sidesOf(p)) {
                // la faccia si vede solo dal lato in cui si trova la camera
                double toCamera = (eye.x - (p.x() + 0.5)) * side.getStepX() + (eye.z - (p.z() + 0.5)) * side.getStepZ();
                if (toCamera <= 0.5) continue;
                int light = LightCoordsUtil.getLightCoords(mc.level, block.relative(side));
                event.getSubmitNodeCollector().submitCustomGeometry(pose,
                        RenderTypes.entityTranslucent(texture(mc, p.typeId())),
                        (last, vc) -> face(last, vc, side, light));
            }
            pose.popPose();
        }
    }

    /** La faccia scelta dallo staff, oppure tutte e quattro le laterali. */
    private static Direction[] sidesOf(PointEntry p) {
        return p.face().map(f -> Direction.byName(f)).filter(d -> d != null && d.getAxis().isHorizontal())
                .map(d -> new Direction[] {d}).orElse(SIDES);
    }

    private static Identifier texture(Minecraft mc, String typeId) {
        return TEXTURES.computeIfAbsent(typeId, id -> {
            Identifier candidate = Identifier.fromNamespaceAndPath("qrm", "textures/point/" + id + ".png");
            return mc.getResourceManager().getResource(candidate).isPresent() ? candidate : DEFAULT_TEXTURE;
        });
    }

    /**
     * La targhetta su una faccia, in coordinate del blocco (0..1). Visto da fuori, "destra" è {@code avanti x su}, dove
     * avanti è l'opposto della normale della faccia.
     */
    private static void face(PoseStack.Pose pose, VertexConsumer vc, Direction side, int light) {
        float nx = side.getStepX();
        float nz = side.getStepZ();
        // destra = (-n) x su = (nz, 0, -nx)... con su = (0,1,0): (-n) x up = (nz, 0, -nx) per n orizzontale
        float rx = nz;
        float rz = -nx;
        // angolo in alto a sinistra della faccia, poi spostato dentro di MARGIN e sollevato di LIFT
        float cx = 0.5F + nx * 0.5F;
        float cz = 0.5F + nz * 0.5F;
        float tlx = cx - rx * 0.5F + rx * MARGIN + nx * LIFT;
        float tly = 1F - MARGIN;
        float tlz = cz - rz * 0.5F + rz * MARGIN + nz * LIFT;
        float blx = tlx;
        float bly = tly - SIZE;
        float blz = tlz;
        vertex(pose, vc, blx, bly, blz, 0F, 1F, nx, nz, light);
        vertex(pose, vc, blx + rx * SIZE, bly, blz + rz * SIZE, 1F, 1F, nx, nz, light);
        vertex(pose, vc, tlx + rx * SIZE, tly, tlz + rz * SIZE, 1F, 0F, nx, nz, light);
        vertex(pose, vc, tlx, tly, tlz, 0F, 0F, nx, nz, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z, float u, float v,
            float nx, float nz, int light) {
        vc.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, nx, 0F, nz);
    }
}
