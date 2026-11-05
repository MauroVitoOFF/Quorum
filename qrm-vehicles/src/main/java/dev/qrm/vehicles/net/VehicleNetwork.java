package dev.qrm.vehicles.net;

import dev.qrm.vehicles.mc.VehicleServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Canale opzionale del Module. */
public final class VehicleNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private VehicleNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        registrar.playToServer(VehiclePayloads.Drive.TYPE, VehiclePayloads.Drive.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) VehicleServer.onDrive(sp, p);
        });
        registrar.playToServer(VehiclePayloads.Action.TYPE, VehiclePayloads.Action.CODEC, (p, ctx) -> {
            // Ordinale fuori intervallo da un client modificato: kind nullo, ignorato.
            if (p.kind() != null && ctx.player() instanceof ServerPlayer sp) VehicleServer.onAction(sp, p.kind());
        });
    }
}
