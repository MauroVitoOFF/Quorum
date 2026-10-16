package dev.qrm.wardrobe.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.qrm.wardrobe.logic.SkinComposer;
import dev.qrm.wardrobe.logic.WardrobeWire;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/** Le skin ricomposte (divisa + testa del giocatore), una per giocatore, create una volta e riusate. */
final class OutfitTextures {
    private record Entry(String outfit, Identifier original, int catalogGeneration, Identifier texture) {}

    private static final Map<UUID, Entry> CACHE = new HashMap<>();

    private OutfitTextures() {}

    /** La skin con la divisa addosso, oppure vuoto se non si può comporre (la skin originale resta in uso). */
    static Optional<PlayerSkin> skinFor(UUID player, String outfitId, PlayerSkin original) {
        Optional<WardrobeWire.CatalogEntry> outfit = ClientOutfits.entry(outfitId);
        if (outfit.isEmpty()) return Optional.empty();
        Identifier originalPath = original.body().texturePath();
        Entry cached = CACHE.get(player);
        if (cached == null || !cached.outfit().equals(outfitId) || !cached.original().equals(originalPath)
                || cached.catalogGeneration() != ClientOutfits.catalogGeneration()) {
            release(player);
            cached = build(player, outfit.get(), original, originalPath);
            if (cached == null) return Optional.empty();
            CACHE.put(player, cached);
        }
        return Optional.of(skin(outfit.get(), original, cached.texture()));
    }

    static PlayerSkin skin(WardrobeWire.CatalogEntry outfit, PlayerSkin original, Identifier texture) {
        return new PlayerSkin(new ClientAsset.ResourceTexture(texture), original.cape(), original.elytra(),
                outfit.slim() ? PlayerModelType.SLIM : PlayerModelType.WIDE, original.secure());
    }

    private static Entry build(UUID player, WardrobeWire.CatalogEntry outfit, PlayerSkin original, Identifier originalPath) {
        Optional<int[]> head = OriginalSkinReader.read(original);
        if (head.isEmpty()) return null;
        int[] body;
        try (NativeImage image = NativeImage.read(outfit.png())) {
            if (image.getWidth() != SkinComposer.SIZE || image.getHeight() != SkinComposer.SIZE) return null;
            body = image.getPixels();
        } catch (IOException | RuntimeException e) {
            return null;
        }
        int[] composed = SkinComposer.compose(body, head.get());
        NativeImage out = new NativeImage(SkinComposer.SIZE, SkinComposer.SIZE, false);
        for (int y = 0; y < SkinComposer.SIZE; y++) {
            for (int x = 0; x < SkinComposer.SIZE; x++) out.setPixel(x, y, composed[y * SkinComposer.SIZE + x]);
        }
        Identifier id = Identifier.fromNamespaceAndPath("qrm_wardrobe", "skin/" + player + "/" + outfit.id());
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, out));
        return new Entry(outfit.id(), originalPath, ClientOutfits.catalogGeneration(), id);
    }

    /** Libera le texture di chi non indossa più una divisa. */
    static void prune(Set<UUID> stillWearing) {
        Iterator<UUID> it = CACHE.keySet().iterator();
        while (it.hasNext()) {
            UUID p = it.next();
            if (!stillWearing.contains(p)) {
                Entry e = CACHE.get(p);
                it.remove();
                Minecraft.getInstance().getTextureManager().release(e.texture());
            }
        }
    }

    private static void release(UUID player) {
        Entry e = CACHE.remove(player);
        if (e != null) Minecraft.getInstance().getTextureManager().release(e.texture());
    }

    /** Fine del collegamento al server: libera tutto. */
    static void clear() {
        prune(Set.of());
    }
}
