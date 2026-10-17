package dev.qrm.police.client;

import dev.qrm.police.mc.PoliceItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/** Parte client del Module: bussola della chiamata e posa di chi tiene il tablet. Caricata solo sul client. */
@Mod(value = "qrm_police", dist = Dist.CLIENT)
public final class PoliceClient {
    private static final Identifier HUD = Identifier.fromNamespaceAndPath("qrm_police", "hud");

    public PoliceClient(IEventBus modBus) {
        modBus.addListener(PoliceClient::onRegisterGuiLayers);
        modBus.addListener(PoliceClient::onRegisterExtensions);
        NeoForge.EVENT_BUS.addListener(PoliceClient::onLoggedOut);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(HUD, PoliceHud::render);
    }

    /** Chi tiene il tablet ha il braccio piegato davanti al petto, come chi legge: gli altri lo vedono. */
    private static void onRegisterExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.@Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand,
                    ItemStack stack) {
                return HumanoidModel.ArmPose.BLOCK;
            }
        }, PoliceItems.TABLET.get());
    }

    private static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPolice.clearWaypoint();
    }
}
