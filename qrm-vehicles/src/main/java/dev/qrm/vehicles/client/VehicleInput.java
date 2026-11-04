package dev.qrm.vehicles.client;

import dev.qrm.vehicles.mc.QrmVehicleEntity;
import dev.qrm.vehicles.net.VehiclePayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Campiona i tasti di chi guida e li manda al server a ogni tick. */
public final class VehicleInput {
    private VehicleInput() {}

    /** Da registrare sul bus di NeoForge, lato client. */
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean driving = player != null && player.getVehicle() instanceof QrmVehicleEntity v
                && v.getControllingPassenger() == player;
        if (!driving) {
            // Si svuotano le pressioni accumulate, così non scattano al prossimo salto in macchina.
            while (VehicleKeys.IGNITION.consumeClick()) { }
            while (VehicleKeys.LIGHTS.consumeClick()) { }
            while (VehicleKeys.HORN.consumeClick()) { }
            return;
        }
        Input k = player.input.keyPresses;
        float throttle = (k.forward() ? 1f : 0f) - (k.backward() ? 1f : 0f);
        float steer = (k.right() ? 1f : 0f) - (k.left() ? 1f : 0f);
        ClientPacketDistributor.sendToServer(new VehiclePayloads.Drive(throttle, steer, k.jump()));
        while (VehicleKeys.IGNITION.consumeClick()) {
            ClientPacketDistributor.sendToServer(new VehiclePayloads.Action(VehiclePayloads.Action.Kind.IGNITION));
        }
        while (VehicleKeys.LIGHTS.consumeClick()) {
            ClientPacketDistributor.sendToServer(new VehiclePayloads.Action(VehiclePayloads.Action.Kind.LIGHTS));
        }
        while (VehicleKeys.HORN.consumeClick()) {
            ClientPacketDistributor.sendToServer(new VehiclePayloads.Action(VehiclePayloads.Action.Kind.HORN));
        }
    }
}
