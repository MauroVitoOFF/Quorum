package dev.qrm.vehicles.client;

import dev.qrm.vehicles.logic.SeatLook;
import dev.qrm.vehicles.mc.QrmVehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Chi sta a bordo resta fisso nel sedile: il corpo segue il veicolo e si muove solo la testa, in base a dove guarda.
 * Il guidatore tiene le mani sullo sterzo. Vale per tutti i giocatori visti dal client, non solo per il locale.
 */
public final class SeatedPose {
    private SeatedPose() {}

    /** Da registrare sul bus di NeoForge, lato client. */
    public static void onRenderPlayer(RenderPlayerEvent.Pre<?> event) {
        AvatarRenderState state = event.getRenderState();
        if (Minecraft.getInstance().level == null) return;
        Entity me = Minecraft.getInstance().level.getEntity(state.id);
        if (me == null || !(me.getVehicle() instanceof QrmVehicleEntity vehicle)) return;

        float partial = event.getPartialTick();
        float bodyYaw = vehicle.getYRot(partial);
        float lookYaw = Mth.rotLerp(partial, me.yRotO, me.getYRot());
        state.bodyRot = bodyYaw;
        state.yRot = SeatLook.headRelativeYaw(lookYaw, bodyYaw);
        if (vehicle.getControllingPassenger() == me) {
            state.rightArmPose = ArmPoses.onWheel();
            state.leftArmPose = ArmPoses.onWheel();
        }
    }
}
