package dev.qrm.wardrobe.client;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Parte client di Wardrobe. Caricata solo sul client. */
@Mod(value = "qrm_wardrobe", dist = Dist.CLIENT)
public final class WardrobeClient {
    public WardrobeClient(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(WardrobeClient::onRenderPlayer);
        NeoForge.EVENT_BUS.addListener(WardrobeClient::onRenderArm);
        NeoForge.EVENT_BUS.addListener(WardrobeClient::onLoggedOut);
    }

    /** Prima di disegnare un giocatore con la divisa, la sua skin viene sostituita con quella ricomposta. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        AvatarRenderState state = (AvatarRenderState) event.getRenderState();
        if (Minecraft.getInstance().level == null) return;
        Entity entity = Minecraft.getInstance().level.getEntity(state.id);
        if (!(entity instanceof Player player)) return;
        Optional<String> outfit = ClientOutfits.wornBy(player.getUUID());
        if (outfit.isEmpty()) return;
        OutfitTextures.skinFor(player.getUUID(), outfit.get(), state.skin).ifPresent(skin -> state.skin = skin);
    }

    /**
     * Il braccio in prima persona: l'evento non permette di cambiare la texture, quindi si annulla il disegno del
     * gioco e si disegna lo stesso braccio con la skin della divisa, come fa il gioco (stesso tipo di rendering).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void onRenderArm(RenderArmEvent event) {
        if (!(event.getAvatar() instanceof AbstractClientPlayer player)) return;
        Optional<String> outfit = ClientOutfits.wornBy(player.getUUID());
        if (outfit.isEmpty()) return;
        Optional<PlayerSkin> skin = OutfitTextures.skinFor(player.getUUID(), outfit.get(), player.getSkin());
        if (skin.isEmpty()) return;
        event.setCanceled(true);
        event.getSubmitNodeCollector().submitModelPart(event.getArmPart(), event.getPoseStack(),
                RenderTypes.entityTranslucent(skin.get().body().texturePath()), event.getLightCoords(),
                OverlayTexture.NO_OVERLAY, null);
    }

    private static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientOutfits.reset();
        OutfitTextures.clear();
    }
}
