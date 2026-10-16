package dev.qrm.wardrobe.mc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;

/** Dove stanno le divise sul server: {@code config/qrm_wardrobe/outfits/}. */
final class WardrobeFiles {
    private WardrobeFiles() {}

    static Path outfitsDir() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(WardrobeMod.MOD_ID).resolve("outfits");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            WardrobeMod.LOG.warn("Impossibile creare {}: {}", dir, e.getMessage());
        }
        return dir;
    }
}
