package dev.qrm.arrival.net;

import dev.qrm.arrival.client.ArrivalClientBridge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Canale opzionale del Module: chi non ha il Module sul client entra e gioca comunque. */
public final class ArrivalNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private ArrivalNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        // La lambda carica il ponte client solo quando il pacchetto arriva davvero: mai sul server dedicato.
        registrar.playToClient(ArrivalPayloads.PhaseUpdate.TYPE, ArrivalPayloads.PhaseUpdate.CODEC,
                (p, ctx) -> ArrivalClientBridge.onPhase(p.phase(), p.elapsed(), p.lyingTicks(), p.risingTicks()));
    }
}
