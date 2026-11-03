package dev.qrm.arrival.client;

import dev.qrm.arrival.logic.EyeEffect;
import dev.qrm.arrival.logic.Stance;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Parte client del Module: la posa a terra del giocatore locale e l'effetto degli occhi. Caricata solo sul client. */
@Mod(value = "qrm_arrival", dist = Dist.CLIENT)
public final class ArrivalClient {
    private static final Identifier HUD = Identifier.fromNamespaceAndPath("qrm_arrival", "eyes");

    public ArrivalClient(IEventBus modBus) {
        modBus.addListener(ArrivalClient::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(ArrivalClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ArrivalClient::onLoggedOut);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(HUD, ArrivalClient::renderEyes);
    }

    private static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientArrival.clear();
    }

    /** Il giocatore locale ricalcola la posa da solo: qui la si tiene come vuole il risveglio. */
    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ClientArrival.active()) return;
        Stance stance = Stance.of(ClientArrival.phase(), ClientArrival.elapsed(), ClientArrival.lyingTicks(),
                ClientArrival.risingTicks());
        mc.player.setPose(switch (stance) {
            case LYING -> Pose.SWIMMING;
            case CROUCHING -> Pose.CROUCHING;
            case STANDING -> Pose.STANDING;
        });
    }

    /** Velo scuro che si schiarisce e palpebre che si aprono. Non muove la camera. */
    private static void renderEyes(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!ClientArrival.active()) return;
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int total = ClientArrival.lyingTicks() + ClientArrival.risingTicks();
        int veil = Math.round(240 * EyeEffect.veil(ClientArrival.elapsed(), total));
        if (veil > 0) g.fill(0, 0, w, h, veil << 24);
        int lid = Math.round(h / 2.0f * EyeEffect.eyelids(ClientArrival.elapsed(), ClientArrival.lyingTicks()));
        if (lid > 0) {
            g.fill(0, 0, w, lid, 0xFF000000);
            g.fill(0, h - lid, w, h, 0xFF000000);
        }
    }
}
