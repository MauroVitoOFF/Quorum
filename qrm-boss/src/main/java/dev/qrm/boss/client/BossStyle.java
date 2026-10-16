package dev.qrm.boss.client;

import dev.qrm.client.api.theme.ThemeRenderer;
import dev.qrm.client.api.widget.TextFit;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Stile del menu boss: lo stesso linguaggio piatto della banca (sfondo teal scuro, contorni di un pixel, riempimenti
 * appena accennati, testo a scala 0.75, oro per l'azione principale). Le primitive sono del modulo: non dipende
 * dall'ATM, ma i valori coincidono perché i due menu devono sembrare della stessa famiglia.
 */
final class BossStyle {
    static final int BG = 0xFF0C1E22;
    static final int LINE = 0xFF2E6B5C;
    static final int LINE_SOFT = 0xFF1F4A44;
    static final int BLUE = 0xFF4FB08A;
    static final int GOLD = 0xFFE2B24A;
    static final int RED = 0xFFE0675C;
    static final int FIELD_BG = 0x55000000;

    /** Scala del testo: più piccolo del normale, per un'interfaccia più sottile. */
    static final float TS = 0.75f;
    static final int TEXT_H = 6;

    private BossStyle() {}

    /** Colore di testo, preso dal tema del modulo. */
    static int color(String key) {
        return ThemeRenderer.color(key);
    }

    static int alpha(int rgb, int a) {
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    static int width(Font font, String s) {
        return (int) Math.ceil(font.width(s) * TS);
    }

    static String fit(Font font, String s, int room) {
        return TextFit.fit(font, s, (int) (room / TS));
    }

    /** Spezza il testo a capo parola in righe larghe al più {@code room} pixel. */
    static List<String> wrap(Font font, String s, int room) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && width(font, next) > room) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(TS, TS);
        g.text(font, s, 0, 0, color, false);
        g.pose().popMatrix();
    }

    static void centered(GuiGraphicsExtractor g, Font font, String s, int cx, int y, int color) {
        text(g, font, s, cx - width(font, s) / 2, y, color);
    }

    static void right(GuiGraphicsExtractor g, Font font, String s, int rx, int y, int color) {
        text(g, font, s, rx - width(font, s), y, color);
    }

    /**
     * La testa della skin del giocatore, a colori se è presente e al lavoro, spenta in grigio altrimenti. Chi non è
     * collegato non ha una skin nota al client: si mostra quella predefinita, comunque in grigio.
     */
    static void face(GuiGraphicsExtractor g, UUID player, boolean active, int x, int y, int size) {
        Minecraft mc = Minecraft.getInstance();
        PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(player);
        PlayerSkin skin = info != null ? info.getSkin() : DefaultPlayerSkin.get(player);
        PlayerFaceExtractor.extractRenderState(g, skin, x, y, size);
        if (!active) g.fill(x, y, x + size, y + size, 0xC0394240);
        outline(g, x - 1, y - 1, size + 2, size + 2, active ? BLUE : LINE_SOFT);
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
}
