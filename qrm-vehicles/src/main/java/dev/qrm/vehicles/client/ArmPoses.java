package dev.qrm.vehicles.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/** La posa di braccio aggiunta a {@code HumanoidModel.ArmPose} (vedi META-INF/enumextensions.json): mani sullo sterzo. */
public final class ArmPoses {
    /** Braccia tese in avanti e verso il basso, appena chiuse verso il volante: fisse, non seguono la testa. */
    private static final float WHEEL_X = -1.2F;
    private static final float WHEEL_IN = 0.2F;

    private static final IArmPoseTransformer ON_WHEEL_TRANSFORM = (model, state, arm) -> {
        if (arm == HumanoidArm.RIGHT) {
            model.rightArm.xRot = WHEEL_X;
            model.rightArm.yRot = -WHEEL_IN;
            model.rightArm.zRot = 0.0F;
        } else {
            model.leftArm.xRot = WHEEL_X;
            model.leftArm.yRot = WHEEL_IN;
            model.leftArm.zRot = 0.0F;
        }
    };

    // twoHanded=false, affectsOffhandPose=false: ogni braccio riceve la propria posa.
    public static final EnumProxy<HumanoidModel.ArmPose> ON_WHEEL_PARAMS =
            new EnumProxy<>(HumanoidModel.ArmPose.class, false, false, ON_WHEEL_TRANSFORM);

    private ArmPoses() {}

    static HumanoidModel.ArmPose onWheel() {
        return ON_WHEEL_PARAMS.getValue();
    }
}
