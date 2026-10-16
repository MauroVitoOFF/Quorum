package dev.qrm.wardrobe.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.qrm.wardrobe.logic.SkinComposer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Legge i pixel (ARGB, 64 di larghezza) della skin originale di un giocatore. Le skin scaricate sono registrate come
 * {@link DynamicTexture} e tengono i pixel in memoria; quelle predefinite (Steve, Alex...) sono risorse del gioco.
 */
final class OriginalSkinReader {
    private OriginalSkinReader() {}

    static Optional<int[]> read(PlayerSkin skin) {
        Identifier path = skin.body().texturePath();
        Minecraft mc = Minecraft.getInstance();
        AbstractTexture texture = mc.getTextureManager().getTexture(path);
        if (texture instanceof DynamicTexture dynamic && dynamic.getPixels() != null) {
            return fromImage(dynamic.getPixels());
        }
        Optional<Resource> resource = mc.getResourceManager().getResource(path);
        if (resource.isEmpty()) return Optional.empty();
        try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            return fromImage(image);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Optional<int[]> fromImage(NativeImage image) {
        if (image.getWidth() != SkinComposer.SIZE) return Optional.empty();
        if (image.getHeight() != SkinComposer.SIZE && image.getHeight() != SkinComposer.SIZE / 2) return Optional.empty();
        return Optional.of(image.getPixels());
    }
}
