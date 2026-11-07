package dev.qrm.vehicles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.qrm.client.api.rig.BonePose;
import dev.qrm.client.api.rig.GeoModel;
import dev.qrm.client.api.rig.Rig;
import dev.qrm.client.api.rig.RigMesh;
import dev.qrm.vehicles.logic.VehicleMotionFx;
import dev.qrm.vehicles.logic.VehiclePose;
import dev.qrm.vehicles.mc.QrmVehicleEntity;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/**
 * Disegna il veicolo osso per osso: le matrici vengono da {@link BonePose} (ruote, sterzo e assetto dalla posa di
 * {@link VehiclePose}), la geometria da {@link VehicleAssets}. Vetri translucidi in un passaggio a parte.
 */
public final class VehicleRenderer extends EntityRenderer<QrmVehicleEntity, VehicleRenderState> {

    /** Luce piena: il pezzo puntato risplende anche all'ombra o di notte. */
    private static final int FULL_BRIGHT = 0xF000F0;

    public VehicleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public VehicleRenderState createRenderState() {
        return new VehicleRenderState();
    }

    @Override
    public void extractRenderState(QrmVehicleEntity entity, VehicleRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        VehicleMotionFx fx = entity.motionFx();
        // rotLerp e non getYRot(partialTick): l'angolo è in 0..360 e l'interpolazione lineare farebbe un giro intero a ogni passaggio per lo zero.
        state.yaw = net.minecraft.util.Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        state.wheelDeg = (float) fx.wheelDeg(partialTick);
        state.steerDeg = visibleSteer((float) fx.steerDeg(partialTick), entity.def().maxSteerDeg());
        state.heavePx = (float) fx.heavePx(partialTick);
        state.pitchDeg = (float) fx.pitchDeg(partialTick);
        state.rollDeg = (float) fx.rollDeg(partialTick);
        state.lightsOn = entity.lightsOn();
        state.parts = entity.partsAnimator().values(partialTick);
        state.hideGlass = !VehicleClientConfig.SHOW_GLASS_INSIDE.get() && VehicleView.isInside(Minecraft.getInstance().player)
                && Minecraft.getInstance().player.getVehicle() == entity;
        state.vehicleId = entity.def().id();
    }

    /** Il veicolo è lungo quasi quattro blocchi: la scatola dell'entità da sola lo taglierebbe dalla vista. */
    @Override
    public boolean shouldRender(QrmVehicleEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return frustum.isVisible(entity.getBoundingBox().inflate(2.5));
    }

    @Override
    public void submit(VehicleRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        GeoModel model = VehicleAssets.model(state.vehicleId);
        Rig rig = model.rig();
        if (!rig.bones().isEmpty()) {
            Map<String, Matrix4f> matrices = BonePose.compose(rig,
                    VehiclePose.pose(state.wheelDeg, state.steerDeg, state.pitchDeg, state.rollDeg, state.heavePx,
                            dev.qrm.vehicles.logic.VehicleParts.pose(state.parts)));
            RenderType opaque = RenderTypes.entityCutout(VehicleAssets.texture(state.vehicleId));
            RenderType glass = RenderTypes.entityTranslucent(VehicleAssets.GLASS_TEXTURE);
            RenderType pane = RenderTypes.entityTranslucent(VehicleAssets.texture(state.vehicleId));
            poseStack.pushPose();
            // Il davanti del modello è -Z; con imbardata 0 l'entità guarda verso +Z.
            poseStack.mulPose(Axis.YP.rotationDegrees(180f - state.yaw));
            for (Rig.Bone bone : rig.bones()) {
                RigMesh mesh = VehicleAssets.mesh(state.vehicleId, bone.name());
                if (mesh.quadCount() == 0) continue;
                boolean glassBone = VehicleAssets.isTransparent(bone.name());
                if (glassBone && state.hideGlass) continue;
                RenderType type = VehicleAssets.isPane(bone.name()) ? pane : glassBone ? glass : opaque;
                poseStack.pushPose();
                poseStack.mulPose(matrices.get(bone.name()));
                collector.submitCustomGeometry(poseStack, type,
                        (pose, buffer) -> mesh.render(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
                poseStack.popPose();
            }
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }

    /**
     * Angolo con cui si disegnano le ruote anteriori. Quello reale è limitato dalla velocità (pochi gradi a 100 km/h) e
     * le ruote sembrerebbero ferme mentre l'auto gira: con la radice l'angolo si vede sempre, senza arrivare oltre il massimo.
     */

    static float visibleSteer(float steerDeg, double maxSteerDeg) {
        double ratio = Math.min(1, Math.abs(steerDeg) / maxSteerDeg);
        return (float) (Math.signum(steerDeg) * maxSteerDeg * Math.sqrt(ratio));
    }
}
