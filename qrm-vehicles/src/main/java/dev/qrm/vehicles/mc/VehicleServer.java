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

    public static void onAction(ServerPlayer player, VehiclePayloads.Action.Kind kind) {
        QrmVehicleEntity v = drivenBy(player);
        if (v == null) return;
        switch (kind) {
            case IGNITION -> v.toggleEngine();
            case LIGHTS -> v.toggleLights();
            case HORN -> v.honk();
        }
    }
}
