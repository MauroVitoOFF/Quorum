package dev.qrm.vehicles.client;

import dev.qrm.vehicles.mc.VehicleEntities;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Parte client del Module: tasti, invio dell'input, rendering e cruscotto. Caricata solo sul client. */
@Mod(value = "qrm_vehicles", dist = Dist.CLIENT)
public final class VehiclesClient {
    private static final Identifier HUD = Identifier.fromNamespaceAndPath("qrm_vehicles", "hud");

    public VehiclesClient(IEventBus modBus) {
        modBus.addListener(VehicleKeys::onRegisterKeys);
        modBus.addListener(VehicleAssets::onAddReloadListeners);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
                e.registerEntityRenderer(VehicleEntities.JEEP.get(), VehicleRenderer::new));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(HUD, VehicleHud::render));
        NeoForge.EVENT_BUS.addListener(VehicleInput::onClientTick);
    }
}
