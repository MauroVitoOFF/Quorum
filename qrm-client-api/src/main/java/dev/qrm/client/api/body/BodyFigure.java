package dev.qrm.client.api.body;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * La sagoma del corpo, vista di fronte. Non sa nulla di medicina: riceve una {@link BodySkin} e un {@link BodyStatus}
 * e colora ogni zona con {@link BodyTint}. Serve sia al piccolo HUD sia ai menu; {@link #partAt} dice quale zona sta
 * sotto il cursore.
 */
public final class BodyFigure {
    private static final Map<Identifier, boolean[]> MASKS = new HashMap<>();

    private BodyFigure() {}

    /** Larghezza sullo schermo, in pixel GUI. */
    public static int width(BodySkin skin, int zoom) {
        return skin.width() * zoom;
    }

    /** Altezza sullo schermo, in pixel GUI. */
    public static int height(BodySkin skin, int zoom) {
        return skin.height() * zoom;
    }

    public static void render(GuiGraphicsExtractor g, BodySkin skin, BodyStatus status, int x, int y, int zoom) {
        int w = skin.width() * zoom;
        int h = skin.height() * zoom;
        for (BodyPart part : BodyPart.values()) {
            int color = BodyTint.color(status.severity(part));
            g.blit(RenderPipelines.GUI_TEXTURED, skin.texture(part), x, y, 0f, 0f, w, h,
                    skin.width(), skin.height(), skin.width(), skin.height(), color);
        }
    }

    /** La zona sotto il punto (in pixel GUI) o {@code null}. La forma è quella della texture, non un rettangolo. */
    public static BodyPart partAt(BodySkin skin, int x, int y, int zoom, double mouseX, double mouseY) {
        int px = (int) Math.floor((mouseX - x) / zoom);
        int py = (int) Math.floor((mouseY - y) / zoom);
        if (px < 0 || py < 0 || px >= skin.width() || py >= skin.height()) return null;
        for (BodyPart part : BodyPart.values()) {
            if (mask(skin, part)[py * skin.width() + px]) return part;
        }
        return null;
    }

    /** Da chiamare quando le risorse si ricaricano. */
    public static void clearCache() {
        MASKS.clear();
    }

    private static boolean[] mask(BodySkin skin, BodyPart part) {
        return MASKS.computeIfAbsent(skin.texture(part), id -> {
            boolean[] mask = new boolean[skin.width() * skin.height()];
            try (InputStream in = Minecraft.getInstance().getResourceManager().open(id);
                    NativeImage image = NativeImage.read(in)) {
                for (int py = 0; py < Math.min(image.getHeight(), skin.height()); py++) {
                    for (int px = 0; px < Math.min(image.getWidth(), skin.width()); px++) {
                        mask[py * skin.width() + px] = (image.getPixel(px, py) >>> 24) != 0;
                    }
                }
            } catch (Exception e) {
                // texture mancante: nessuna zona selezionabile
            }
            return mask;
        });
    }
}
