package dev.qrm.neoforge.client.theme;

import static org.junit.jupiter.api.Assertions.*;
import dev.qrm.client.api.theme.ThemeManifest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Ogni texture del manifesto predefinito esiste nelle risorse ed ha le dimensioni dichiarate. */
class ThemeAssetsTest {
    @Test
    void everyBuiltInTextureExistsWithTheDeclaredSize() throws IOException {
        ThemeManifest theme = BuiltInTheme.manifest();
        assertFalse(theme.textureKeys().isEmpty());
        for (String key : theme.textureKeys()) {
            ThemeManifest.Texture t = theme.texture(key);
            String[] parts = t.id().split(":", 2);
            String path = "/assets/" + parts[0] + "/" + parts[1];
            try (InputStream in = ThemeAssetsTest.class.getResourceAsStream(path)) {
                assertNotNull(in, "missing texture for '" + key + "': " + path);
                BufferedImage img = ImageIO.read(in);
                assertNotNull(img, "not a PNG: " + path);
                assertEquals(t.width(), img.getWidth(), "width of " + path);
                assertEquals(t.height(), img.getHeight(), "height of " + path);
            }
        }
    }
}
