package dev.qrm.immersion.net;

import dev.qrm.immersion.client.ImmersionClientBridge;
import dev.qrm.immersion.mc.ImmersionServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Canale opzionale del Module. */
public final class ImmersionNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private ImmersionNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        // Le lambda caricano il ponte client solo quando il pacchetto arriva davvero: mai sul server dedicato.
        registrar.playToClient(ImmersionPayloads.PoseUpdate.TYPE, ImmersionPayloads.PoseUpdate.CODEC,
                (p, ctx) -> ImmersionClientBridge.onPose(p.entityId(), p.pose()));
        registrar.playToClient(ImmersionPayloads.Known.TYPE, ImmersionPayloads.Known.CODEC,
                (p, ctx) -> ImmersionClientBridge.onKnown(p.entries()));
        registrar.playToServer(ImmersionPayloads.PoseRequest.TYPE, ImmersionPayloads.PoseRequest.CODEC,
                (p, ctx) -> ImmersionServer.onPoseRequest((ServerPlayer) ctx.player(), p.kind()));
    }
}
