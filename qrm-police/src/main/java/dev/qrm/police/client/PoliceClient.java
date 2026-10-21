package dev.qrm.police.client;

import dev.qrm.police.mc.PoliceItems;
import dev.qrm.police.weapon.client.PistolAnimator;
import dev.qrm.police.weapon.client.PistolAssets;
import dev.qrm.police.weapon.client.PistolCamera;
import dev.qrm.police.weapon.client.PistolClientBridge;
import dev.qrm.police.weapon.client.PistolClientTick;
import dev.qrm.police.weapon.client.PistolFirstPerson;
import dev.qrm.police.weapon.client.PistolInput;
import dev.qrm.police.weapon.client.PistolKeys;
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

/**
 * Parte client del Module: bussola della chiamata, posa di chi tiene il tablet, pistola in prima persona, comandi,
 * mira e contraccolpo della pistola. Caricata solo sul client.
 */
@Mod(value = "qrm_police", dist = Dist.CLIENT)
public final class PoliceClient {
    private static final Identifier HUD = Identifier.fromNamespaceAndPath("qrm_police", "hud");

    public PoliceClient(IEventBus modBus) {
        modBus.addListener(PoliceClient::onRegisterGuiLayers);
        modBus.addListener(PoliceClient::onRegisterExtensions);
        modBus.addListener(PistolAssets::onAddReloadListeners);
        modBus.addListener(PistolKeys::onRegisterKeys);
        NeoForge.EVENT_BUS.addListener(PoliceClient::onLoggedOut);
        NeoForge.EVENT_BUS.addListener(PoliceClient::onClone);
        NeoForge.EVENT_BUS.addListener(PistolInput::onClientTick);
        NeoForge.EVENT_BUS.addListener(PistolInput::onInteractionKey);
        // L'animatore avanza prima dello smistatore, che così vede gli eventi del tick appena passato.
        NeoForge.EVENT_BUS.addListener(PistolAnimator::onClientTick);
        NeoForge.EVENT_BUS.addListener(PistolClientTick::onClientTick);
        NeoForge.EVENT_BUS.addListener(PistolCamera::onComputeCameraAngles);
        NeoForge.EVENT_BUS.addListener(PistolCamera::onComputeFov);
        NeoForge.EVENT_BUS.addListener(PistolFirstPerson::onRenderHand);
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
        // Pistola: in mira entrambe le braccia tese verso il bersaglio (anche gli altri, tramite Aim); altrimenti
        // la posa di un oggetto qualsiasi.
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.@Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand,
                    ItemStack stack) {
                return hand == InteractionHand.MAIN_HAND && PistolClientBridge.isAiming(entity)
                        ? HumanoidModel.ArmPose.BOW_AND_ARROW
                        : HumanoidModel.ArmPose.ITEM;
            }
        }, PoliceItems.PISTOL.get());
    }

    private static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPolice.clearWaypoint();
        PistolAnimator.local().reset();
        resetPistol();
    }

    /** Rinascita o cambio di dimensione: nuovo giocatore locale e nuovi id delle entità. */
    private static void onClone(ClientPlayerNetworkEvent.Clone event) {
        resetPistol();
    }

    private static void resetPistol() {
        PistolInput.reset();
        PistolCamera.reset();
        PistolClientBridge.clear();
    }
}
