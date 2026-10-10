package dev.qrm.locker.net;

import dev.qrm.locker.client.LockerClientBridge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Canale opzionale dell'armadietto, con protocollo proprio. */
public final class LockerNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private LockerNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        // La lambda carica il ponte client solo quando il pacchetto arriva davvero: mai sul server dedicato.
        registrar.playToClient(LockerSkinPayload.TYPE, LockerSkinPayload.CODEC,
                (payload, context) -> LockerClientBridge.arm());
    }
}
