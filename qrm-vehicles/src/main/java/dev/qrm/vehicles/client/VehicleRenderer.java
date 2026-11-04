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
        state.yaw = entity.getYRot(partialTick);
        state.wheelDeg = (float) fx.wheelDeg(partialTick);
        state.steerDeg = entity.steerAngle();
        state.pitchDeg = (float) fx.pitchDeg(partialTick);
        state.rollDeg = (float) fx.rollDeg(partialTick);
        state.lightsOn = entity.lightsOn();
    }

    /** Il veicolo è lungo quasi quattro blocchi: la scatola dell'entità da sola lo taglierebbe dalla vista. */
    @Override
    public boolean shouldRender(QrmVehicleEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return frustum.isVisible(entity.getBoundingBox().inflate(2.5));
    }

    @Override
    public void submit(VehicleRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        GeoModel model = VehicleAssets.model();
        Rig rig = model.rig();
        if (!rig.bones().isEmpty()) {
            Map<String, Matrix4f> matrices = BonePose.compose(rig,
                    VehiclePose.pose(state.wheelDeg, state.steerDeg, state.pitchDeg, state.rollDeg));
            RenderType opaque = RenderTypes.entityCutout(VehicleAssets.TEXTURE);
            RenderType glass = RenderTypes.entityTranslucent(VehicleAssets.GLASS_TEXTURE);
            poseStack.pushPose();
            // Il davanti del modello è -Z; con imbardata 0 l'entità guarda verso +Z.
            poseStack.mulPose(Axis.YP.rotationDegrees(180f - state.yaw));
            for (Rig.Bone bone : rig.bones()) {
                RigMesh mesh = VehicleAssets.mesh(bone.name());
                if (mesh.quadCount() == 0) continue;
                RenderType type = VehicleAssets.isGlass(bone.name()) ? glass : opaque;
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
}
