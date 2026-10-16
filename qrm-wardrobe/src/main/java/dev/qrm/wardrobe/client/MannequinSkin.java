package dev.qrm.wardrobe.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import dev.qrm.wardrobe.logic.SkinComposer;
import dev.qrm.wardrobe.logic.WardrobeWire;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/** Il manichino grigio dell'armadietto: la divisa sul corpo e una testa neutra, senza leggere la skin del giocatore. */
final class MannequinSkin {
    private static final int GREY = 0xFF8C8C8C;

    private record Cached(int catalogGeneration, RemotePlayer player, Identifier texture) {}

    private static final Map<String, Cached> CACHE = new HashMap<>();

    private MannequinSkin() {}

    /** Il manichino con la divisa indicata ({@code ""} = abito civile, tutto grigio). */
    static Optional<RemotePlayer> playerFor(String outfitId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return Optional.empty();
        Cached cached = CACHE.get(outfitId);
        if (cached != null && cached.catalogGeneration() == ClientOutfits.catalogGeneration()) {
            return Optional.of(cached.player());
        }
        release(outfitId);
        Optional<WardrobeWire.CatalogEntry> outfit = outfitId.isEmpty() ? Optional.empty() : ClientOutfits.entry(outfitId);
        if (!outfitId.isEmpty() && outfit.isEmpty()) return Optional.empty();
        int[] body = new int[SkinComposer.SIZE * SkinComposer.SIZE];
        Arrays.fill(body, GREY);
        if (outfit.isPresent()) {
            try (NativeImage image = NativeImage.read(outfit.get().png())) {
                if (image.getWidth() != SkinComposer.SIZE || image.getHeight() != SkinComposer.SIZE) return Optional.empty();
                body = image.getPixels();
            } catch (IOException | RuntimeException e) {
                return Optional.empty();
            }
        }
        int[] composed = SkinComposer.compose(body, SkinComposer.flatHead(GREY));
        NativeImage out = new NativeImage(SkinComposer.SIZE, SkinComposer.SIZE, false);
        for (int y = 0; y < SkinComposer.SIZE; y++) {
            for (int x = 0; x < SkinComposer.SIZE; x++) out.setPixel(x, y, composed[y * SkinComposer.SIZE + x]);
        }
        Identifier id = Identifier.fromNamespaceAndPath("qrm_wardrobe",
                "mannequin/" + (outfitId.isEmpty() ? "civil" : outfitId));
        mc.getTextureManager().register(id, new DynamicTexture(id::toString, out));
        boolean slim = outfit.map(WardrobeWire.CatalogEntry::slim).orElse(false);
        PlayerSkin skin = new PlayerSkin(new ClientAsset.ResourceTexture(id), null, null,
                slim ? PlayerModelType.SLIM : PlayerModelType.WIDE, true);
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("qrm-mannequin:" + outfitId).getBytes()), "mannequin");
        RemotePlayer dummy = new RemotePlayer(mc.level, profile) {
            @Override
            public PlayerSkin getSkin() {
                return skin;
            }
        };
        // Fuori dal mondo l'entità non riceve un id: serve a chi la disegna (id negativo, mai in conflitto).
        dummy.setId(-1 - (outfitId.hashCode() & 0xFFFFF));
        CACHE.put(outfitId, new Cached(ClientOutfits.catalogGeneration(), dummy, id));
        return Optional.of(dummy);
    }

    private static void release(String outfitId) {
        Cached old = CACHE.remove(outfitId);
        if (old != null) Minecraft.getInstance().getTextureManager().release(old.texture());
    }

    static void clear() {
        for (String id : java.util.List.copyOf(CACHE.keySet())) release(id);
    }
}
