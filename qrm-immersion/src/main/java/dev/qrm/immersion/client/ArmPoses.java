package dev.qrm.immersion.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * Le due pose di braccio aggiunte a {@code HumanoidModel.ArmPose} (vedi META-INF/enumextensions.json).
 * I campi {@code *_PARAMS} sono i parametri del costruttore dell'enum: niente Mixin.
 */
public final class ArmPoses {
    /** Spalle: braccia in alto e appena aperte. L'angolo dell'asse X e' -150 gradi. */
    private static final float UP_X = -2.6F;
    private static final float UP_SPREAD = 0.25F;

    private static final IArmPoseTransformer HANDS_UP_TRANSFORM = (model, state, arm) -> {
        if (arm == HumanoidArm.RIGHT) {
            model.rightArm.xRot = UP_X;
            model.rightArm.yRot = 0.0F;
            model.rightArm.zRot = -UP_SPREAD;
        } else {
            model.leftArm.xRot = UP_X;
            model.leftArm.yRot = 0.0F;
            model.leftArm.zRot = UP_SPREAD;
        }
    };

    /** Il braccio destro dritto in avanti, che segue la testa: inclinazione e direzione. */
    private static final IArmPoseTransformer POINTING_TRANSFORM = (model, state, arm) -> {
        if (arm != HumanoidArm.RIGHT) return;
        model.rightArm.xRot = (float) (-Math.PI / 2) + model.head.xRot;
        model.rightArm.yRot = model.head.yRot;
        model.rightArm.zRot = 0.0F;
    };

    // twoHanded=false, affectsOffhandPose=false: ogni braccio riceve la propria posa.
    public static final EnumProxy<HumanoidModel.ArmPose> HANDS_UP_PARAMS =
            new EnumProxy<>(HumanoidModel.ArmPose.class, false, false, HANDS_UP_TRANSFORM);
    public static final EnumProxy<HumanoidModel.ArmPose> POINTING_PARAMS =
            new EnumProxy<>(HumanoidModel.ArmPose.class, false, false, POINTING_TRANSFORM);

    private ArmPoses() {}

    static HumanoidModel.ArmPose handsUp() {
        return HANDS_UP_PARAMS.getValue();
    }

    static HumanoidModel.ArmPose pointing() {
        return POINTING_PARAMS.getValue();
    }

}
