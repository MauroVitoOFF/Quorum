package dev.qrm.vehicles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.qrm.vehicles.logic.VehicleHitboxes;
import dev.qrm.vehicles.mc.QrmVehicleEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * {@code /qrmhitbox} mostra o nasconde il contorno di tutte le hitbox dei veicoli vicini: l'ingombro solido in rosso, le
 * zone di interazione in ciano nella posa di adesso (seguono portiere, cofano, leve) e quella puntata in giallo.
 * Solo client, solo per provare.
 */
public final class HitboxDebug {
    private HitboxDebug() {}

    private static final double RANGE = 48.0;
    private static final float LINE_WIDTH = 2f;
    private static final int SOLID = 0xFFFF3030;
    private static final int ZONE = 0xFF30E0FF;
    private static final int TARGET = 0xFFFFE030;

    private static boolean on;

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("qrmhitbox").executes(c -> {
            on = !on;
            c.getSource().sendSuccess(
                    () -> Component.translatable(on ? "qrm_vehicles.hitbox_debug.on" : "qrm_vehicles.hitbox_debug.off"),
                    false);
            return 1;
        }));
    }

    public static void onSubmit(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!on || mc.level == null) return;
        Vec3 eye = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack pose = event.getPoseStack();
        for (QrmVehicleEntity v : mc.level.getEntitiesOfClass(QrmVehicleEntity.class,
                mc.player.getBoundingBox().inflate(RANGE))) {
            VehicleHitboxes h = v.hitboxes();
            double yaw = Math.toRadians(180 - v.getYRot());
            float c = (float) Math.cos(yaw);
            float s = (float) Math.sin(yaw);
            String target = VehicleTargeting.targetedZone(v);
            List<VehicleHitboxes.Outline> outlines = h.outlines(v.hitPose());
            pose.pushPose();
            pose.translate(v.getX() - eye.x, v.getY() - eye.y, v.getZ() - eye.z);
            for (VehicleHitboxes.Outline o : outlines) {
                int color = o.solid() ? SOLID : o.name().equals(target) ? TARGET : ZONE;
                event.getSubmitNodeCollector().submitCustomGeometry(pose, RenderTypes.lines(), (last, vc) -> {
                    for (double[] g : o.segments()) {
                        line(last, vc, c, s, g, color);
                    }
                });
            }
            pose.popPose();
        }
    }

    /** Un segmento dal modello al mondo: rotazione inversa di quella del raggio (modello -> mondo). */
    static void line(PoseStack.Pose last, com.mojang.blaze3d.vertex.VertexConsumer vc, float c, float s,
            double[] g, int color) {
        line(last, vc, c, s, g, color, LINE_WIDTH);
    }

    static void line(PoseStack.Pose last, com.mojang.blaze3d.vertex.VertexConsumer vc, float c, float s,
            double[] g, int color, float width) {
        float x1 = (float) (g[0] * c + g[2] * s);
        float z1 = (float) (-g[0] * s + g[2] * c);
        float x2 = (float) (g[3] * c + g[5] * s);
        float z2 = (float) (-g[3] * s + g[5] * c);
        float dx = x2 - x1;
        float dy = (float) (g[4] - g[1]);
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) return;
        dx /= len;
        dy /= len;
        dz /= len;
        vc.addVertex(last, x1, (float) g[1], z1).setColor(color).setNormal(last, dx, dy, dz).setLineWidth(width);
        vc.addVertex(last, x2, (float) g[4], z2).setColor(color).setNormal(last, dx, dy, dz).setLineWidth(width);
    }
}
