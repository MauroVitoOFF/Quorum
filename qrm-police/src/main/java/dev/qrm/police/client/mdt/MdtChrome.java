package dev.qrm.police.client.mdt;

import static dev.qrm.police.client.mdt.MdtLayout.BEZEL;
import static dev.qrm.police.client.mdt.MdtLayout.H;
import static dev.qrm.police.client.mdt.MdtLayout.HEADER;
import static dev.qrm.police.client.mdt.MdtLayout.SIDE;
import static dev.qrm.police.client.mdt.MdtLayout.STATUS;
import static dev.qrm.police.client.mdt.MdtLayout.W;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.BG;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.BLUE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.GREEN;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.LINE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.PANEL;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.RED;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT;

import dev.qrm.police.client.mdt.ui.MdtWidgets;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** La cornice del tablet: scocca, barra di stato, intestazione e riflesso sul vetro. Nessuna pagina ci passa. */
final class MdtChrome {
    private static final int SHELL = 0xFF272C34;
    private static final int SHELL_EDGE = 0xFF3A404A;
    private static final int SHELL_RECESS = 0xFF0E1218;
    private static final int BUMPER = 0xFF1C1F24;
    private static final int HOME = 0xFF454C58;
    private static final int STATUS_BG = 0xFF0E131C;
    private static final int LED_ON = 0xFF5CC088;
    private static final int LED_ALERT_A = 0xFFDF6C6C;
    private static final int LED_ALERT_B = 0xFFE0A54E;
    private final Font font;
    private final int x0;
    private final int y0;

    MdtChrome(Font font, int x0, int y0) {
        this.font = font;
        this.x0 = x0;
        this.y0 = y0;
    }

    /** Disegna ciò che sta sotto le pagine: scocca, sfondo, barra di stato, intestazione e filetto laterale. */
    void renderBack(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean activeCalls, String officer) {
        renderBezel(g, mouseX, mouseY, activeCalls);
        MdtWidgets.box(g, x0, y0, W, H, LINE, BG);
        renderStatusBar(g);
        renderHeader(g, officer);
        g.fill(x0 + SIDE, y0 + STATUS + HEADER, x0 + SIDE + 1, y0 + H, LINE);
    }

    private static boolean isOnline() {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.getConnection().isConnected();
    }

    private void renderHeader(GuiGraphicsExtractor g, String officer) {
        int hy = y0 + STATUS;
        g.fill(x0, hy, x0 + W, hy + HEADER, PANEL);
        g.fill(x0, hy + HEADER - 1, x0 + W, hy + HEADER, BLUE);
        int ty = hy + (HEADER - 8) / 2;
        String title = "MDT // TERMINALE OPERATIVO";
        g.text(font, title, x0 + 10, ty, TEXT, false);
        int badgeX = x0 + 10 + font.width(title) + 8;
        boolean online = isOnline();
        String badge = online ? "RETE: GOV" : "RETE: OFFLINE";
        int accent = online ? BLUE : RED;
        int bw = scaledWidth(badge) + 15;
        MdtWidgets.box(g, badgeX, hy + 5, bw, 11, MdtWidgets.alpha(accent, 0x78), MdtWidgets.alpha(accent, 0x18));
        g.fill(badgeX + 4, hy + 9, badgeX + 7, hy + 12, online ? GREEN : RED);
        scaled(g, badge, badgeX + 11, hy + 8, accent);
        int room = (x0 + W - 28) - (badgeX + bw + 10);
        String who = MdtWidgets.fit(font, "Op. " + officer, room);
        g.text(font, who, x0 + W - 28 - font.width(who), ty, DIM, false);
    }

    /** Rettangolo con i 2x2 pixel di ogni angolo tagliati. */
    private static void chamfered(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x + 2, y, x + w - 2, y + h, color);
        g.fill(x, y + 2, x + w, y + h - 2, color);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
    }

    private int homeX() {
        return x0 + W / 2 - 20;
    }

    private int homeY() {
        return y0 + H + BEZEL - 8;
    }

    boolean overHome(double mx, double my) {
        return mx >= homeX() && mx < homeX() + 40 && my >= homeY() && my < homeY() + 4;
    }

    private void renderBezel(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean activeCalls) {
        int fx = x0 - BEZEL;
        int fy = y0 - BEZEL;
        int fw = W + 2 * BEZEL;
        int fh = H + 2 * BEZEL;
        chamfered(g, fx, fy, fw, fh, SHELL_EDGE);
        chamfered(g, fx + 1, fy + 1, fw - 2, fh - 2, SHELL);
        int b = 14;
        chamfered(g, fx, fy, b, b, BUMPER);
        chamfered(g, fx + fw - b, fy, b, b, BUMPER);
        chamfered(g, fx, fy + fh - b, b, b, BUMPER);
        chamfered(g, fx + fw - b, fy + fh - b, b, b, BUMPER);
        g.fill(x0 - 2, y0 - 2, x0 + W + 2, y0 + H + 2, SHELL_RECESS);

        int mid = x0 + W / 2;
        g.fill(mid - 8, fy + 4, mid - 4, fy + 8, 0xFF050505);
        g.fill(mid - 8, fy + 4, mid - 7, fy + 5, 0xFF8FB4D6);
        // Verde fisso a terminale acceso; rosso e ambra alternati se c'è almeno una chiamata attiva.
        int led = LED_ON;
        if (activeCalls) led = (System.currentTimeMillis() / 400) % 2 == 0 ? LED_ALERT_A : LED_ALERT_B;
        g.fill(mid + 2, fy + 4, mid + 5, fy + 7, led);

        g.fill(homeX(), homeY(), homeX() + 40, homeY() + 4, overHome(mouseX, mouseY) ? 0xFF677183 : HOME);
    }

    /** Fascia diagonale di luce sul vetro. */
    void renderGlare(GuiGraphicsExtractor g) {
        for (int r = 0; r < H; r++) {
            int x = x0 + (int) (W * 0.62) - r * 3 / 4;
            g.fill(Math.max(x0, x), y0 + r, Math.min(x0 + W, x + 46), y0 + r + 1, 0x05FFFFFF);
            g.fill(Math.max(x0, x + 56), y0 + r, Math.min(x0 + W, x + 62), y0 + r + 1, 0x05FFFFFF);
        }
    }

    private void scaled(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.75f, 0.75f);
        g.text(font, s, 0, 0, color, false);
        g.pose().popMatrix();
    }

    private int scaledWidth(String s) {
        return (int) Math.ceil(font.width(s) * 0.75f);
    }

    private static String worldTime() {
        var level = Minecraft.getInstance().level;
        if (level == null) return "--:--";
        long t = level.getDefaultClockTime() % 24000L;
        int hour = (int) ((t / 1000 + 6) % 24);
        int minute = (int) ((t % 1000) * 60 / 1000);
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    private void renderStatusBar(GuiGraphicsExtractor g) {
        g.fill(x0, y0, x0 + W, y0 + STATUS, STATUS_BG);
        int ty = y0 + 3;
        int x = x0 + 6;
        String a = "Q-OS  |  LTE ";
        scaled(g, a, x, ty, DIM);
        x += scaledWidth(a);
        for (int i = 0; i < 4; i++) {
            int h = 2 + i * 2;
            g.fill(x + i * 3, y0 + 9 - h, x + i * 3 + 2, y0 + 9, i < 3 ? GREEN : BLUE);
        }
        x += 14;
        scaled(g, " |  GPS", x, ty, DIM);

        int right = x0 + W - 6;
        int bx = right - 14;
        MdtWidgets.outline(g, bx, y0 + 3, 12, 6, DIM);
        g.fill(bx + 12, y0 + 5, bx + 14, y0 + 7, DIM);
        g.fill(bx + 1, y0 + 4, bx + 11, y0 + 8, GREEN);
        String time = worldTime();
        scaled(g, time, bx - 5 - scaledWidth(time), ty, TEXT);
    }
}
