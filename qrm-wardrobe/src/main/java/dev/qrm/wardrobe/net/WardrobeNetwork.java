package dev.qrm.wardrobe.net;

import dev.qrm.wardrobe.client.WardrobeClientBridge;
import dev.qrm.wardrobe.mc.WardrobeServer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Canale opzionale del Module: un client senza il mod entra e gioca. */
public final class WardrobeNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private WardrobeNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        // Le lambda caricano il ponte client solo quando il pacchetto arriva davvero: mai sul server dedicato.
        registrar.playToClient(WardrobePayloads.Catalog.TYPE, WardrobePayloads.Catalog.CODEC,
                (p, ctx) -> WardrobeClientBridge.onCatalog(p.total(), p.index(), p.entry()));
        registrar.playToClient(WardrobePayloads.Worn.TYPE, WardrobePayloads.Worn.CODEC,
                (p, ctx) -> WardrobeClientBridge.onWorn(p.data()));
        registrar.playToClient(WardrobePayloads.Open.TYPE, WardrobePayloads.Open.CODEC,
                (p, ctx) -> WardrobeClientBridge.onOpen(p.data()));
        registrar.playToServer(WardrobePayloads.Choose.TYPE, WardrobePayloads.Choose.CODEC,
                (p, ctx) -> WardrobeServer.onChoose(ctx.player(), p.data()));
    }
}
