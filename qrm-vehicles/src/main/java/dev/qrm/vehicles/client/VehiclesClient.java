package dev.qrm.vehicles.client;

import dev.qrm.vehicles.mc.QrmVehicleEntity;
import dev.qrm.vehicles.mc.VehicleEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** Parte client del Module: tasti, invio dell'input, rendering, cruscotto e impostazioni della visuale. */
@Mod(value = "qrm_vehicles", dist = Dist.CLIENT)
public final class VehiclesClient {
    private static final Identifier HUD = Identifier.fromNamespaceAndPath("qrm_vehicles", "hud");

    public VehiclesClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, VehicleClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        QrmVehicleEntity.VIEW_OFFSET = VehicleView::offsetFor;

        modBus.addListener(VehicleKeys::onRegisterKeys);
        modBus.addListener(VehicleAssets::onAddReloadListeners);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
                VehicleEntities.ALL.forEach(type -> e.registerEntityRenderer(type.get(), VehicleRenderer::new)));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(HUD, VehicleHud::render));
        NeoForge.EVENT_BUS.addListener(VehicleInput::onClientTick);
        NeoForge.EVENT_BUS.addListener(SeatedPose::onRenderPlayer);
        // Il tasto apre le stesse impostazioni della lista dei mod, senza uscire dal mondo.
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            while (VehicleKeys.VIEW_SETTINGS.consumeClick()) {
                if (mc.gui.screen() == null) mc.setScreenAndShow(new ConfigurationScreen(container, null));
            }
        });
    }
}
