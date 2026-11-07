package dev.qrm.vehicles.mc;

import dev.qrm.vehicles.logic.DriveInput;
import dev.qrm.vehicles.net.VehiclePayloads;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/** Lato server dei comandi del guidatore: li accetta solo da chi è davvero al volante di un veicolo. */
public final class VehicleServer {
    private VehicleServer() {}

    private static @Nullable QrmVehicleEntity drivenBy(ServerPlayer player) {
        if (player.getVehicle() instanceof QrmVehicleEntity v && v.getControllingPassenger() == player) return v;
        return null;
    }

    public static void onDrive(ServerPlayer player, VehiclePayloads.Drive p) {
        QrmVehicleEntity v = drivenBy(player);
        if (v == null) return;
        v.setDriverInput(new DriveInput(p.throttle(), p.steer(), p.brake()).clamped());
    }

    /** Distanza massima (blocchi) a cui si può usare una zona di un veicolo. */
    public static final double INTERACT_REACH = 4.5;

    /**
     * Il giocatore ha usato una zona. Il server rifà il puntamento dal suo occhio e dalla sua direzione: la zona
     * dichiarata dal client conta solo se è davvero quella che sta guardando, entro la portata.
     */
    public static void onInteract(ServerPlayer player, int entityId, String zone, boolean hold) {
        if (!(player.level().getEntity(entityId) instanceof QrmVehicleEntity v) || v.isRemoved()) return;
        var eye = player.getEyePosition();
        var look = player.getLookAngle();
        // Basta che il giocatore stia guardando quella zona (con un po' di tolleranza): il client ha scelto la
        // sua, il server non la contesta per un'animazione che si è mossa di qualche centimetro.
        if (!v.hitboxes().hitsZone(zone, eye.x, eye.y, eye.z, look.x, look.y, look.z, INTERACT_REACH + 0.5, v.getX(),
                v.getY(), v.getZ(), v.getYRot(), v.hitPose(), dev.qrm.vehicles.logic.VehicleHitboxes.SERVER_MARGIN)) {
            return;
        }
        v.useZone(player, zone, hold);
    }

    public static void onAction(ServerPlayer player, VehiclePayloads.Action.Kind kind) {
        QrmVehicleEntity v = drivenBy(player);
        if (v == null) return;
        switch (kind) {
            case IGNITION -> {
                if (v.keyInserted()) v.toggleEngine();
                else player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("msg.qrm_vehicles.no_key"), true);
            }
            case LIGHTS -> v.toggleLights();
            case HORN -> v.honk();
            case PARKING_BRAKE -> v.toggleParkingBrake();
            case SHIFT_UP -> v.shiftUp();
            case SHIFT_DOWN -> v.shiftDown();
        }
    }
}
