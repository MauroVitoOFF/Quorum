package dev.qrm.medical.net;

import dev.qrm.medical.client.MedicalClientBridge;
import dev.qrm.medical.mc.MedicalServer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Canale opzionale del Module. */
public final class MedicalNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private MedicalNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        // Le lambda caricano il ponte client solo quando il pacchetto arriva davvero: mai sul server dedicato.
        registrar.playToClient(MedicalPayloads.Status.TYPE, MedicalPayloads.Status.CODEC,
                (p, ctx) -> MedicalClientBridge.onStatus(p.state(), p.bleedOutTicks(), p.downedTicks(), p.rcp()));
        registrar.playToServer(MedicalPayloads.Action.TYPE, MedicalPayloads.Action.CODEC,
                (p, ctx) -> MedicalServer.onAction(ctx.player(), p.kind()));
    }
}
