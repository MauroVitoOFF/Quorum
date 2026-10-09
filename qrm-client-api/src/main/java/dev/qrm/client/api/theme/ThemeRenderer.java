package dev.qrm.client.api.theme;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Disegno delle texture del tema: stirata o nine-slice, a seconda del manifesto. */
public final class ThemeRenderer {
    private ThemeRenderer() {}

    public static int color(String key) {
        return QrmTheme.current().color(key);
    }

    public static void draw(GuiGraphicsExtractor g, String textureKey, int x, int y, int w, int h) {
        draw(g, QrmTheme.current().texture(textureKey), x, y, w, h);
    }

    public static void draw(GuiGraphicsExtractor g, ThemeManifest.Texture t, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        Identifier tex = Identifier.parse(t.id());
        int tw = t.width();
        int th = t.height();
        if (t.slice().isEmpty()) {
            piece(g, tex, tw, th, x, y, w, h, 0, 0, tw, th);
            return;
        }
        ThemeManifest.Slice s = t.slice().get();
        int l = Math.min(s.left(), w / 2);
        int r = Math.min(s.right(), w / 2);
        int tp = Math.min(s.top(), h / 2);
        int b = Math.min(s.bottom(), h / 2);
        int midW = w - l - r;
        int midH = h - tp - b;
        int srcMidW = tw - s.left() - s.right();
        int srcMidH = th - s.top() - s.bottom();
        int rx = tw - s.right();
        int by = th - s.bottom();
        // riga alta
        piece(g, tex, tw, th, x, y, l, tp, 0, 0, s.left(), s.top());
        piece(g, tex, tw, th, x + l, y, midW, tp, s.left(), 0, srcMidW, s.top());
        piece(g, tex, tw, th, x + w - r, y, r, tp, rx, 0, s.right(), s.top());
        // riga centrale
        piece(g, tex, tw, th, x, y + tp, l, midH, 0, s.top(), s.left(), srcMidH);
        piece(g, tex, tw, th, x + l, y + tp, midW, midH, s.left(), s.top(), srcMidW, srcMidH);
        piece(g, tex, tw, th, x + w - r, y + tp, r, midH, rx, s.top(), s.right(), srcMidH);
        // riga bassa
        piece(g, tex, tw, th, x, y + h - b, l, b, 0, by, s.left(), s.bottom());
        piece(g, tex, tw, th, x + l, y + h - b, midW, b, s.left(), by, srcMidW, s.bottom());
        piece(g, tex, tw, th, x + w - r, y + h - b, r, b, rx, by, s.right(), s.bottom());
    }

    private static void piece(GuiGraphicsExtractor g, Identifier tex, int tw, int th, int x, int y, int w, int h,
            int u, int v, int srcW, int srcH) {
        if (w <= 0 || h <= 0 || srcW <= 0 || srcH <= 0) return;
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, (float) u, (float) v, w, h, srcW, srcH, tw, th);
    }
}
