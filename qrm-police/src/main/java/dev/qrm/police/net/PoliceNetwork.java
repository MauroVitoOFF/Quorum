package dev.qrm.police.net;

import dev.qrm.police.client.PoliceClientBridge;
import dev.qrm.police.mc.PoliceServer;
import dev.qrm.police.weapon.client.PistolClientBridge;
import dev.qrm.police.weapon.mc.PistolServer;
import dev.qrm.police.weapon.net.PistolPayloads;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Canale opzionale del Module. */
public final class PoliceNetwork {
    public static final String PROTOCOL_VERSION = "3";

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
        registerPistol(registrar);
    }

    private static void registerPistol(PayloadRegistrar registrar) {
        registrar.playToServer(PistolPayloads.Input.TYPE, PistolPayloads.Input.STREAM_CODEC,
                (p, ctx) -> {
                    // Ordinale fuori intervallo da un client modificato: kind nullo, ignorato.
                    if (p.kind() != null && ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        PistolServer.onInput(sp, p.kind());
                    }
                });
        registrar.playToClient(PistolPayloads.ActionStarted.TYPE, PistolPayloads.ActionStarted.STREAM_CODEC,
                (p, ctx) -> {
                    if (p.action() != null) PistolClientBridge.onActionStarted(p);
                });
        registrar.playToClient(PistolPayloads.ActionEnded.TYPE, PistolPayloads.ActionEnded.STREAM_CODEC,
                (p, ctx) -> PistolClientBridge.onActionEnded(p));
        registrar.playToClient(PistolPayloads.Shot.TYPE, PistolPayloads.Shot.STREAM_CODEC,
                (p, ctx) -> PistolClientBridge.onShot(p));
        registrar.playToClient(PistolPayloads.Aim.TYPE, PistolPayloads.Aim.STREAM_CODEC,
                (p, ctx) -> PistolClientBridge.onAim(p));
        registrar.playToClient(PistolPayloads.Rejected.TYPE, PistolPayloads.Rejected.STREAM_CODEC,
                (p, ctx) -> {
                    if (p.kind() != null) PistolClientBridge.onRejected(p);
                });
        registrar.playToClient(PistolPayloads.Identity.TYPE, PistolPayloads.Identity.STREAM_CODEC,
                (p, ctx) -> PistolClientBridge.onIdentity(p));
    }
}
