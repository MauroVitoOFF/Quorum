package dev.qrm.bank.client;

import dev.qrm.client.api.widget.TextFit;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Stile piatto dell'ATM: sfondo teal scuro, contorni di un pixel e riempimenti appena accennati. */
final class AtmStyle {
    static final int BG = 0xFF0C1E22;
    static final int LINE = 0xFF2E6B5C;
    static final int LINE_SOFT = 0xFF1F4A44;
    static final int BLUE = 0xFF4FB08A;
    static final int GOLD = 0xFFE2B24A;
    static final int RED = 0xFFE0675C;
    static final int FIELD_BG = 0x55000000;

    /** Colore di testo dell'ATM, preso dal tema del modulo. */
    static int color(String key) {
        return dev.qrm.client.api.theme.ThemeRenderer.color(key);
    }

    /** Scala del testo: più piccolo del normale, per un'interfaccia più sottile. */
    static final float TS = 0.75f;
    static final int TEXT_H = 6;

    private AtmStyle() {}

    static int width(Font font, String s) {
        return (int) Math.ceil(font.width(s) * TS);
    }

    static String fit(Font font, String s, int room) {
        return TextFit.fit(font, s, (int) (room / TS));
    }

    static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color, boolean ignored) {
        text(g, font, s, x, y, color);
    }

    static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(TS, TS);
        g.text(font, s, 0, 0, color, false);
        g.pose().popMatrix();
    }

    /** Sezione senza contorno: solo un velo appena più chiaro dello sfondo. */
    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0x16FFFFFF);
    }

    /** Solo il contorno di un pixel. */
    static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int line, int fill) {
        if ((fill >>> 24) != 0) g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        outline(g, x, y, w, h, line);
    }

    /** Colore con la trasparenza data (0-255). */
    static int alpha(int rgb, int a) {
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
