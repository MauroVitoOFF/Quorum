package dev.qrm.police.weapon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.qrm.police.mc.PoliceItems;
import dev.qrm.police.weapon.logic.anim.BoneTransform;
import dev.qrm.police.weapon.logic.mesh.BonePose;
import dev.qrm.police.weapon.logic.mesh.Rig;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Matrix4f;

/**
 * Pistola animata in prima persona: sostituisce il disegno vanilla della mano principale quando impugna la pistola e
 * il rig è caricato.
 *
 * <p>Posizionamento: la stessa catena del vanilla per un oggetto in mano ({@code applyItemArmTransform} con
 * l'abbassamento del cambio d'oggetto; ondeggio della vista e del passo sono già nella PoseStack dell'evento), poi
 * l'equivalente costante di {@code firstperson_righthand}/{@code lefthand} di {@code models/item/police_pistol.json}
 * (traslazione ±1,5/5/-3 sedicesimi, nessuna rotazione, scala 0,7) e lo spostamento di -0,5 che il vanilla applica
 * ai modelli a cubi. Il colpo di braccio (swing) non si applica: il tasto "attacca" spara.
 *
 * <p>Braccia: se il rig ha i bone {@code arm_right}/{@code arm_left} (Parte F) il braccio del giocatore si appende
 * al loro perno; altrimenti si disegna solo il braccio della mano principale alla posa vanilla della mano vuota. In
 * entrambi i casi passa da {@code AvatarRenderer.renderRightHand/renderLeftHand}, che lancia {@code RenderArmEvent}:
 * Wardrobe vi applica la skin della divisa.
 *
 * <p>Convenzione dei bone delle braccia (per la Parte F): il perno è la spalla; a riposo il braccio pende lungo -Y
 * dal perno e sporge verso +X (destro) o -X (sinistro), con le proporzioni del modello del giocatore (4×12×4
 * sedicesimi) nello spazio del modello della pistola, quindi anch'esso in scala 0,7 come in anteprima.
 */
public final class PistolFirstPerson {

    /** Equivalente costante del {@code display.firstperson_righthand} del modello (in blocchi). */
    private static final float DISPLAY_X = 1.5f / 16f;
    private static final float DISPLAY_Y = 5f / 16f;
    private static final float DISPLAY_Z = -3f / 16f;
    private static final float DISPLAY_SCALE = 0.7f;

    private static final String ARM_RIGHT = "arm_right";
    private static final String ARM_LEFT = "arm_left";

    private PistolFirstPerson() {}

    /** Da registrare sul bus di NeoForge, lato client. */
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        ItemStack stack = event.getItemStack();
        if (!stack.is(PoliceItems.PISTOL.get())) return;
        Rig rig = PistolAssets.rig();
        if (rig.bones().isEmpty()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        event.setCanceled(true);
        if (player.isScoping()) return; // come il vanilla: col cannocchiale non si disegna la mano

        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        int light = event.getPackedLight();
        float equip = event.getEquipProgress();
        HumanoidArm mainArm = player.getMainArm();
        float invert = mainArm == HumanoidArm.RIGHT ? 1f : -1f;

        Map<String, BoneTransform> pose = PistolAnimator.local().pose(event.getPartialTick());
        Map<String, Matrix4f> matrices = BonePose.compose(rig, pose);
        boolean armBones = rig.bone(ARM_RIGHT).isPresent() || rig.bone(ARM_LEFT).isPresent();

        if (!armBones && !player.isInvisible()) {
            poseStack.pushPose();
            vanillaArmPose(poseStack, invert, equip);
            renderArm(player, mainArm, poseStack, collector, light);
            poseStack.popPose();
        }

        poseStack.pushPose();
        // ItemInHandRenderer.applyItemArmTransform
        poseStack.translate(invert * 0.56f, -0.52f + equip * -0.6f, -0.72f);
        // ItemTransform.apply di firstperson_righthand / lefthand
        poseStack.translate(invert * DISPLAY_X, DISPLAY_Y, DISPLAY_Z);
        poseStack.scale(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE);
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        for (Rig.Bone bone : rig.bones()) {
            Matrix4f m = matrices.get(bone.name());
            if (m == null) continue;
            if (bone.hasModel()) submitBone(bone.name(), m, poseStack, collector, light);
            if (!player.isInvisible() && (ARM_RIGHT.equals(bone.name()) || ARM_LEFT.equals(bone.name()))) {
                HumanoidArm side = ARM_RIGHT.equals(bone.name()) ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
                poseStack.pushPose();
                poseStack.mulPose(BonePose.pivotFrame(bone, m));
                renderArmAtBone(player, side, poseStack, collector, light);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }

    private static void submitBone(String name, Matrix4f m, PoseStack poseStack, SubmitNodeCollector collector,
            int light) {
        RigMesh mesh = PistolAssets.mesh(name);
        if (mesh.quadCount() == 0) return;
        // Scala nulla (caricatore assente, tappo nascosto): niente da vedere e normali non invertibili.
        if (Math.abs(m.determinant3x3()) < 1e-9f) return;
        poseStack.pushPose();
        poseStack.mulPose(m);
        collector.submitCustomGeometry(poseStack, PistolAssets.renderType(),
                (pose, buffer) -> mesh.render(pose, buffer, light, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }

    /** {@code ItemInHandRenderer.renderPlayerArm} con colpo di braccio nullo: posa della mano vuota. */
    private static void vanillaArmPose(PoseStack poseStack, float invert, float equip) {
        poseStack.translate(invert * 0.64000005f, -0.6f + equip * -0.6f, -0.71999997f);
        poseStack.mulPose(Axis.YP.rotationDegrees(invert * 45.0f));
        poseStack.translate(invert * -1.0f, 3.6f, 3.5f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(invert * 120.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(invert * -135.0f));
        poseStack.translate(invert * 5.6f, 0.0f, 0.0f);
    }

    /**
     * Braccio appeso al perno di un bone. Il modello del giocatore ha Y verso il basso e X specchiata (Rz 180°);
     * {@code renderHand} rimette il braccio nella sua posa iniziale (perno della spalla) e lo inclina di ±0,1 rad su
     * Z: entrambe le cose si annullano qui, così la spalla coincide con il perno del bone.
     */
    private static void renderArmAtBone(AbstractClientPlayer player, HumanoidArm side, PoseStack poseStack,
            SubmitNodeCollector collector, int light) {
        AvatarRenderer<AbstractClientPlayer> renderer =
                Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player);
        ModelPart part = side == HumanoidArm.RIGHT ? renderer.getModel().rightArm : renderer.getModel().leftArm;
        float zRot = side == HumanoidArm.RIGHT ? 0.1f : -0.1f;
        PartPose initial = part.getInitialPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
        poseStack.mulPose(Axis.ZP.rotation(-zRot));
        poseStack.translate(-initial.x() / 16f, -initial.y() / 16f, -initial.z() / 16f);
        renderArm(player, side, poseStack, collector, light);
    }

    /** Disegno vanilla del braccio del giocatore: lancia {@code RenderArmEvent} (skin di Wardrobe). */
    private static void renderArm(AbstractClientPlayer player, HumanoidArm side, PoseStack poseStack,
            SubmitNodeCollector collector, int light) {
        AvatarRenderer<AbstractClientPlayer> renderer =
                Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player);
        Identifier skin = player.getSkin().body().texturePath();
        if (side == HumanoidArm.RIGHT) {
            renderer.renderRightHand(poseStack, collector, light, skin,
                    player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE), player);
        } else {
            renderer.renderLeftHand(poseStack, collector, light, skin,
                    player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE), player);
        }
    }
}
