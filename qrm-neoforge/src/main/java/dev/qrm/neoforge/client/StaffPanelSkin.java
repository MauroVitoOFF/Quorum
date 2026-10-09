package dev.qrm.neoforge.client;

import dev.qrm.client.api.theme.ThemeRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Accesso sicuro al tema del pannello staff e piccoli decori disegnati a mano (rombi, filo ornamentale). Se il manifesto di
 * un resource pack non ha le chiavi {@code staffpanel.*} / {@code staff*} si ripiega su aspetti semplici invece di fallire.
 */
final class StaffPanelSkin {
    private StaffPanelSkin() {}

    /** Disegna la texture del tema; se la chiave manca ripiega su un riempimento piatto. */
    static void draw(GuiGraphicsExtractor g, String key, int x, int y, int w, int h, int fallbackArgb) {
        try {
            ThemeRenderer.draw(g, key, x, y, w, h);
        } catch (IllegalArgumentException e) {
            g.fill(x, y, x + w, y + h, fallbackArgb);
        }
    }

    /** Come {@link #draw} ma ripiega sul pannello HUD (per la cornice). */
    static void drawFrame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        try {
            ThemeRenderer.draw(g, "staffpanel.frame", x, y, w, h);
        } catch (IllegalArgumentException e) {
            ThemeRenderer.draw(g, "hud.panel", x, y, w, h);
        }
    }

    /** Disegna la gemma di una sezione (14x14); se manca non disegna nulla. Restituisce true se l'ha disegnata. */
    static boolean gem(GuiGraphicsExtractor g, String sectionId, int x, int y) {
        try {
            ThemeRenderer.draw(g, "staffpanel.gem." + sectionId, x, y, 14, 14);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static int color(String key, String fallbackKey) {
        try {
            return ThemeRenderer.color(key);
        } catch (IllegalArgumentException e) {
            return ThemeRenderer.color(fallbackKey);
        }
    }

    static int gold() {
        return color("staffGold", "accent2");
    }

    static int goldDark() {
        return color("staffGoldDark", "accent2");
    }

    static int cream() {
        return color("staffCream", "text");
    }

    static int muted() {
        return color("staffMuted", "muted");
    }

    static int danger() {
        return color("staffDanger", "error");
    }

    /** Rombo pieno di raggio {@code r}. */
    static void diamond(GuiGraphicsExtractor g, int cx, int cy, int r, int argb) {
        for (int dy = -r; dy <= r; dy++) {
            int half = r - Math.abs(dy);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, argb);
        }
    }

    /** Filo orizzontale con un rombo al centro e due puntini vicino agli estremi. */
    static void ornamentRule(GuiGraphicsExtractor g, int x1, int x2, int y) {
        int mid = (x1 + x2) / 2;
        int dark = goldDark();
        int gold = gold();
        g.fill(x1, y, mid - 5, y + 1, dark);
        g.fill(mid + 6, y, x2, y + 1, dark);
        diamond(g, mid, y, 2, gold);
        g.fill(mid - 8, y, mid - 7, y + 1, gold);
        g.fill(mid + 8, y, mid + 9, y + 1, gold);
    }
}
