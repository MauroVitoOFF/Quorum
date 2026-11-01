package dev.qrm.police.client;

import dev.qrm.police.mc.PoliceItems;
import dev.qrm.police.mc.PoliceParticles;
import dev.qrm.police.weapon.client.GunSmokeParticle;
import dev.qrm.police.weapon.client.WeaponAnimator;
import dev.qrm.police.weapon.client.TracerParticle;
import dev.qrm.police.weapon.client.WeaponAssets;
import dev.qrm.police.weapon.client.PistolCamera;
import dev.qrm.police.weapon.client.ClientIdentity;
import dev.qrm.police.weapon.client.PistolClientBridge;
import dev.qrm.police.weapon.client.WeaponCrosshair;
import dev.qrm.police.weapon.client.PistolEffects;
import dev.qrm.police.weapon.client.PistolClientTick;
import dev.qrm.police.weapon.client.WeaponFirstPerson;
import dev.qrm.police.weapon.client.PistolHints;
import dev.qrm.police.weapon.client.PistolInput;
import dev.qrm.police.weapon.client.PistolKeys;
import dev.qrm.police.weapon.client.WeaponMuzzleFx;
import dev.qrm.police.weapon.client.WeaponPoseCommand;
import dev.qrm.police.weapon.client.WeaponPoseScreen;
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
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
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
        modBus.addListener(WeaponAssets::onAddReloadListeners);
        modBus.addListener(PistolKeys::onRegisterKeys);
        modBus.addListener((RegisterParticleProvidersEvent e) -> {
            e.registerSpriteSet(PoliceParticles.GUN_SMOKE.get(), sprites -> (options, level, x, y, z, xd, yd, zd, random) ->
                    new GunSmokeParticle(level, x, y, z, xd, yd, zd, sprites));
            e.registerSpriteSet(PoliceParticles.TRACER.get(), sprites -> (options, level, x, y, z, xd, yd, zd, random) ->
                    new TracerParticle(level, x, y, z, xd, sprites));
        });
        NeoForge.EVENT_BUS.addListener(PoliceClient::onLoggedOut);
        NeoForge.EVENT_BUS.addListener(PoliceClient::onClone);
        NeoForge.EVENT_BUS.addListener(PistolInput::onClientTick);
        NeoForge.EVENT_BUS.addListener(PistolInput::onInteractionKey);
        // L'animatore avanza prima dello smistatore, che così vede gli eventi del tick appena passato.
        NeoForge.EVENT_BUS.addListener(WeaponAnimator::onClientTick);
        NeoForge.EVENT_BUS.addListener(PistolClientTick::onClientTick);
        // Dopo lo smistatore: un commit "check" del tick appena passato conta già in questo tick.
        NeoForge.EVENT_BUS.addListener(PistolHints::onClientTick);
        PistolHints.init();
        PistolEffects.init();
        ClientIdentity.init();
        NeoForge.EVENT_BUS.addListener(PistolEffects::onClientTick);
        NeoForge.EVENT_BUS.addListener(WeaponMuzzleFx::onClientTick);
        NeoForge.EVENT_BUS.addListener(PistolCamera::onComputeCameraAngles);
        NeoForge.EVENT_BUS.addListener(PistolCamera::onComputeFov);
        NeoForge.EVENT_BUS.addListener(WeaponFirstPerson::onRenderHand);
        NeoForge.EVENT_BUS.addListener(WeaponPoseCommand::register);
        NeoForge.EVENT_BUS.addListener(WeaponCrosshair::onRenderLayer);
        NeoForge.EVENT_BUS.addListener(WeaponPoseScreen::onClientTick);
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
        WeaponAnimator.local().reset();
        ClientIdentity.clear();
        resetPistol(false);
    }

    /** Rinascita o cambio di dimensione: nuovo giocatore locale e nuovi id delle entità. */
    private static void onClone(ClientPlayerNetworkEvent.Clone event) {
        resetPistol(true);
    }

    /** @param connected vero se la connessione resta (rinascita, cambio di dimensione): allora si manda AIM_OFF */
    private static void resetPistol(boolean connected) {
        PistolInput.reset(connected);
        PistolCamera.reset();
        PistolClientBridge.clear();
        PistolHints.reset();
    }
}
