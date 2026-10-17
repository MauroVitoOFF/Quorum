package dev.qrm.police.net;

import dev.qrm.police.client.PoliceClientBridge;
import dev.qrm.police.mc.PoliceServer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Canale opzionale del Module. */
public final class PoliceNetwork {
    public static final String PROTOCOL_VERSION = "2";

    private PoliceNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        // Le lambda caricano il ponte client solo quando il pacchetto arriva davvero: mai sul server dedicato.
        registrar.playToClient(PolicePayloads.Snapshot.TYPE, PolicePayloads.Snapshot.CODEC,
                (p, ctx) -> PoliceClientBridge.onSnapshot(p));
        registrar.playToServer(PolicePayloads.Action.TYPE, PolicePayloads.Action.CODEC,
                (p, ctx) -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp) PoliceServer.onAction(sp, p.kind(), p.args());
                });
    }
}
