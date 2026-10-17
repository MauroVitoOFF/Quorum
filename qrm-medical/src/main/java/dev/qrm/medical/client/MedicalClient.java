package dev.qrm.medical.client;

import dev.qrm.medical.logic.MedicalRules;
import dev.qrm.medical.net.MedicalPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

/** Parte client del Module: HUD, battito, tasti, posa. Caricata solo sul client. */
@Mod(value = "qrm_medical", dist = Dist.CLIENT)
public final class MedicalClient {
    private static final Identifier HUD = Identifier.fromNamespaceAndPath("qrm_medical", "hud");
    private static int surrenderHold;
    private static int heartbeatIn;

    public MedicalClient(IEventBus modBus) {
        modBus.addListener(MedicalClient::onRegisterKeys);
        modBus.addListener(MedicalClient::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(MedicalClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(MedicalClient::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(MedicalClient::onLoggedOut);
    }

    /** Per il HUD: da quanti tick è tenuto premuto il tasto di resa. */
    static int surrenderHold() {
        return surrenderHold;
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(MedicalKeys.CATEGORY);
        event.register(MedicalKeys.SURRENDER);
        event.register(MedicalKeys.ALERT);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(HUD, MedicalHud::render);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientMedical.tick();
        if (mc.player == null || !ClientMedical.incapacitated()) {
            surrenderHold = 0;
            heartbeatIn = 0;
            return;
        }
        // Il giocatore locale ricalcola la posa da solo: qui la si tiene a terra.
        mc.player.setPose(Pose.SWIMMING);

        while (MedicalKeys.ALERT.consumeClick()) {
            ClientPacketDistributor.sendToServer(new MedicalPayloads.Action(MedicalPayloads.ACTION_ALERT));
        }
        if (ClientMedical.canSurrender() && MedicalKeys.SURRENDER.isDown()) {
            if (++surrenderHold >= MedicalRules.SURRENDER_HOLD_TICKS) {
                surrenderHold = 0;
                ClientPacketDistributor.sendToServer(new MedicalPayloads.Action(MedicalPayloads.ACTION_SURRENDER));
            }
        } else {
            surrenderHold = 0;
        }
        heartbeat(mc);
    }

    /** Più il tempo scende, più il battito è ravvicinato. */
    private static void heartbeat(Minecraft mc) {
        if (--heartbeatIn > 0) return;
        double left = (double) ClientMedical.bleedOutTicks() / MedicalRules.BLEED_OUT_TICKS;
        heartbeatIn = (int) Math.round(12 + 28 * left);
        mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.PLAYERS, 1.0F, 1.0F, false);
    }

    /** Chi è ferito non apre l'inventario né altri contenitori. */
    private static void onScreenOpening(ScreenEvent.Opening event) {
        if (ClientMedical.incapacitated() && event.getNewScreen() instanceof AbstractContainerScreen<?>) {
            event.setCanceled(true);
        }
    }

    private static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMedical.reset();
    }
}
