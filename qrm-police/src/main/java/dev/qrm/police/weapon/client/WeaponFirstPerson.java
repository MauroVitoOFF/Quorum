package dev.qrm.police.weapon.client;

import dev.qrm.client.api.rig.RigMesh;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.qrm.police.weapon.logic.WeaponDef;
import dev.qrm.client.api.rig.BoneTransform;
import dev.qrm.police.weapon.logic.mesh.ArmSolver;
import dev.qrm.client.api.rig.BonePose;
import dev.qrm.client.api.rig.Rig;
import java.util.List;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Pistola animata in prima persona: sostituisce il disegno vanilla della mano principale quando impugna la pistola e
 * il rig è caricato.
 *
 * <p>Posizionamento: la stessa catena del vanilla per un oggetto in mano ({@code applyItemArmTransform} con
 * l'abbassamento del cambio d'oggetto; ondeggio della vista e del passo sono già nella PoseStack dell'evento), poi
 * l'equivalente costante di {@code firstperson_righthand}/{@code lefthand} di {@code models/item/police_pistol.json}
 * (traslazione ∓2/0,7/-10,3 sedicesimi, rotazione 2/0/0, scala 0,7; vedi WeaponPose) e lo spostamento di -0,5 che il vanilla applica
 * ai modelli a cubi. Il colpo di braccio (swing) non si applica: il tasto "attacca" spara.
 *
 * <p>Braccia: non sono geometria né bone animati. A ogni fotogramma {@link ArmSolver} ricava la cornice di ciascun
 * braccio da un punto di presa sull'arma (il pugno) e da una spalla fissa dietro la camera, secondo {@code hands} di
 * {@code weapons/<arma>.json}; la mano sinistra segue il programma della clip in corso. Il braccio vero passa da
 * {@code AvatarRenderer.renderRightHand/renderLeftHand}, che lancia {@code RenderArmEvent}: Wardrobe vi applica la
 * skin della divisa.
 */
public final class WeaponFirstPerson {

    private WeaponFirstPerson() {}

    private static Vector3f point(WeaponDef.Anchor a) {
        return new Vector3f(a.point()[0], a.point()[1], a.point()[2]).div(16f);
    }

    /** Da registrare sul bus di NeoForge, lato client. */
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        ItemStack stack = event.getItemStack();
        WeaponDef def = HeldWeapon.of(stack).orElse(null);
        if (def == null) return;
        // Il caricatore si disegna solo mentre lo si riempie: il resto del tempo resta il disegno vanilla.
        if (def.renderOnlyInActions() && WeaponAnimator.local().actionClip() == null) return;
        WeaponPose.select(def);
        Rig rig = WeaponAssets.rig(def);
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

        Map<String, BoneTransform> pose = WeaponAnimator.local().pose(event.getPartialTick());
        Map<String, Matrix4f> matrices = BonePose.compose(rig, pose);
        // Le mani seguono i bone senza le scale: un bone nascosto (scala 0) non trascina la mano sul suo perno.
        Map<String, Matrix4f> handMatrices = BonePose.compose(rig, ArmSolver.withoutScale(pose));
        WeaponAnimator animator = WeaponAnimator.local();
        String actionClip = animator.actionClip();
        double actionTime = animator.actionSeconds(event.getPartialTick());

        poseStack.pushPose();
        // ItemInHandRenderer.applyItemArmTransform
        poseStack.translate(invert * 0.56f, -0.52f + equip * -0.6f, -0.72f);
        // ItemTransform.apply di firstperson_righthand / lefthand
        poseStack.translate(invert * WeaponPose.x(), WeaponPose.y(), WeaponPose.z());
        poseStack.mulPose(new Quaternionf().rotationXYZ((float) Math.toRadians(WeaponPose.rotX()),
                (float) Math.toRadians(invert * WeaponPose.rotY()), (float) Math.toRadians(invert * WeaponPose.rotZ())));
        float scale = WeaponPose.scale();
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        for (Rig.Bone bone : rig.bones()) {
            Matrix4f m = matrices.get(bone.name());
            if (m == null) continue;
            if (bone.hasModel()) submitBone(def, bone.name(), m, poseStack, collector, light);
        }
        if (!player.isInvisible()) {
            List<ArmSolver.Step> leftSteps = actionClip == null ? List.of() : def.leftSchedule(actionClip);
            List<ArmSolver.Step> rightSteps = actionClip == null ? List.of() : def.rightSchedule(actionClip);
            ArmSolver.Hand right = ArmSolver.follow(def.handTargets(), def.rightDefault(), rightSteps, actionTime,
                    handMatrices);
            ArmSolver.Hand left = ArmSolver.follow(def.handTargets(), def.leftDefault(), leftSteps, actionTime,
                    handMatrices);
            renderHand(player, HumanoidArm.RIGHT, ArmSolver.frame(true, right), poseStack, collector, light);
            renderHand(player, HumanoidArm.LEFT, ArmSolver.frame(false, left), poseStack, collector, light);
        }
        Matrix4f slide = rig.bone(def.hintBone()).map(b -> {
            Matrix4f m = matrices.get(b.name());
            return m == null ? null : BonePose.pivotFrame(b, m);
        }).orElse(null);
        Matrix4f magM = matrices.get(def.magazineTop().bone());
        Vector3f magTop = magM == null ? null : magM.transformPosition(point(def.magazineTop()), new Vector3f());
        PistolHints.render(poseStack, slide, magTop, collector, event.getPartialTick());
        WeaponMuzzleFx.render(poseStack, matrices.get(def.muzzle().bone()), def, collector, event.getPartialTick());
        poseStack.popPose();
    }

    private static void submitBone(WeaponDef def, String name, Matrix4f m, PoseStack poseStack, SubmitNodeCollector collector,
            int light) {
        RigMesh mesh = WeaponAssets.mesh(def, name);
        if (mesh.quadCount() == 0) return;
        // Scala nulla (caricatore assente, tappo nascosto): niente da vedere e normali non invertibili.
        if (Math.abs(m.determinant3x3()) < 1e-9f) return;
        poseStack.pushPose();
        poseStack.mulPose(m);
        collector.submitCustomGeometry(poseStack, WeaponAssets.renderType(def),
                (pose, buffer) -> mesh.render(pose, buffer, light, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }

    private static void renderHand(AbstractClientPlayer player, HumanoidArm side, Matrix4f frame, PoseStack poseStack,
            SubmitNodeCollector collector, int light) {
        poseStack.pushPose();
        poseStack.mulPose(frame);
        renderArmAtFrame(player, side, poseStack, collector, light);
        poseStack.popPose();
    }

    /**
     * Braccio appeso alla cornice calcolata da {@link ArmSolver}. Il modello del giocatore ha Y verso il basso e X specchiata (Rz 180°);
     * {@code renderHand} rimette il braccio nella sua posa iniziale (perno della spalla) e lo inclina di ±0,1 rad su
     * Z: entrambe le cose si annullano qui, così la spalla coincide con l'origine della cornice.
     */
    private static void renderArmAtFrame(AbstractClientPlayer player, HumanoidArm side, PoseStack poseStack,
            SubmitNodeCollector collector, int light) {
        AvatarRenderer<AbstractClientPlayer> renderer =
                Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player);
        ModelPart part = side == HumanoidArm.RIGHT ? renderer.getModel().rightArm : renderer.getModel().leftArm;
        float zRot = side == HumanoidArm.RIGHT ? 0.1f : -0.1f;
        PartPose initial = part.getInitialPose();
        poseStack.translate(WeaponPose.armX() / 16f, WeaponPose.armY() / 16f, WeaponPose.armZ() / 16f);
        poseStack.mulPose(Axis.XP.rotationDegrees(WeaponPose.armRotX()));
        poseStack.mulPose(Axis.YP.rotationDegrees(WeaponPose.armRotY()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(WeaponPose.armRotZ()));
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
