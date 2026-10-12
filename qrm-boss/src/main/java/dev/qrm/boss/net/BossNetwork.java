package dev.qrm.boss.net;

import dev.qrm.boss.client.BossClientBridge;
import dev.qrm.boss.server.BossServer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Canale opzionale del menu boss, con protocollo proprio (indipendente da quello di QRM). */
public final class BossNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private BossNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        // Le lambda caricano il ponte client solo quando il pacchetto arriva davvero: mai sul server dedicato.
        registrar.playToClient(BossStatePayload.TYPE, BossStatePayload.CODEC,
                (payload, context) -> BossClientBridge.onState(payload.reply()));
        registrar.playToClient(BossOrgListPayload.TYPE, BossOrgListPayload.CODEC,
                (payload, context) -> BossClientBridge.onOrgList(payload.list()));
        registrar.playToServer(BossActionPayload.TYPE, BossActionPayload.CODEC, BossServer::onAction);
    }
}
