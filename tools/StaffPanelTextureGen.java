import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Genera le texture del pannello staff HUD (cornice dorata, placche delle righe, riquadro, pillola e gemme di sezione),
 * pixel-art originale, deterministico, solo JDK.
 * Uso (dalla radice del repo): java tools/StaffPanelTextureGen.java [cartella] [foglio-anteprima.png]
 * Le dimensioni devono coincidere con assets/qrm/theme/default.json (lo verifica ThemeAssetsTest).
 */
public class StaffPanelTextureGen {
    static final Color INK = new Color(0x120B1C);
    static final Color GOLD = new Color(0xF5C75B);
    static final Color GOLD_HI = new Color(0xFFE6A6);
    static final Color GOLD_DARK = new Color(0xB07A1E);
    static final Color GOLD_DEEP = new Color(0x6B4710);
    static final Color CREAM = new Color(0xFFF3D6);

    /** Id sezione, colore della gemma e glifo 8x8 ('#' = crema, 'o' = ombra del colore, '.' = vuoto). */
    record Gem(String id, int color, String[] glyph) {}

    static final Gem[] GEMS = {
        new Gem("players", 0x37C8E8, new String[] {
            "...##...", "..####..", "..####..", "...##...", ".######.", "########", "########", "........"}),
        new Gem("player_management", 0x4FCB6B, new String[] {
            "########", "#......#", "#.##.###", "#.##....", "#....###", "#......#", "########", "........"}),
        new Gem("server_management", 0xF2913B, new String[] {
            "########", "#.....o#", "########", "#.....o#", "########", "#.....o#", "########", "........"}),
        new Gem("moderation", 0xE5555F, new String[] {
            "########", "#oooooo#", "#oooooo#", ".#oooo#.", ".#oooo#.", "..#oo#..", "...##...", "........"}),
        new Gem("economy", 0xF5C75B, new String[] {
            "..####..", ".#oooo#.", "#oo##oo#", "#oo##oo#", "#oo##oo#", "#oo##oo#", ".#oooo#.", "..####.."}),
        new Gem("organizations", 0x9B72E8, new String[] {
            "#.#..#.#", "########", "########", "##o##o##", "########", "##o##o##", "########", "########"}),
        new Gem("logs", 0x5A8FE8, new String[] {
            "########", "#......#", "#.####.#", "#......#", "#.####.#", "#......#", "#.###..#", "########"}),
        new Gem("quick_actions", 0xF2DC4A, new String[] {
            "....###.", "...###..", "..###...", ".######.", "...###..", "..###...", ".###....", ".##....."}),
    };

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args.length > 0 ? args[0]
                : "qrm-neoforge/src/main/resources/assets/qrm/textures/gui/staffpanel");
        Files.createDirectories(dir);
        Map<String, BufferedImage> out = new LinkedHashMap<>();
        out.put("frame.png", frame());
        out.put("row.png", plate(32, 16, 0x33264A, 0x271C3A, 0x65508A, 0x44345F));
        out.put("row_selected.png", plate(32, 16, 0x5A4283, 0x45306A, 0xF5C75B, 0x8D6A2A));
        out.put("row_soon.png", plate(32, 16, 0x261D36, 0x201829, 0x3F3454, 0x2D2540));
        out.put("row_danger.png", plate(32, 16, 0x4F2236, 0x3C1828, 0x9A4256, 0x6B2D3C));
        out.put("inset.png", inset());
        out.put("chip.png", chip());
        for (Gem gem : GEMS) out.put("gem_" + gem.id() + ".png", gem(gem));
        for (Map.Entry<String, BufferedImage> e : out.entrySet()) {
            ImageIO.write(e.getValue(), "png", dir.resolve(e.getKey()).toFile());
        }
        System.out.println("Textures written under " + dir.toAbsolutePath());
        if (args.length > 1) preview(out, Path.of(args[1]));
    }

    // ---- primitive -----------------------------------------------------------------------------------------

    static BufferedImage image(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    static Graphics2D g(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    static Color mix(Color a, Color b, double t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static void px(BufferedImage img, int x, int y, Color c) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) img.setRGB(x, y, c.getRGB() | 0xFF000000);
    }

    static void clear(BufferedImage img, int x, int y) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) img.setRGB(x, y, 0);
    }

    // ---- cornice -------------------------------------------------------------------------------------------

    /** Cornice a nove fette (slice 12): fondo viola scuro, doppio bordo dorato, borchie a rombo negli angoli. */
    static BufferedImage frame() {
        int s = 48;
        BufferedImage img = image(s, s);
        Graphics2D g = g(img);
        g.setPaint(new GradientPaint(0, 0, new Color(0x2E1F45), 0, s, new Color(0x1B1230)));
        g.fillRect(0, 0, s, s);
        // anelli del bordo, dall'esterno: inchiostro, oro scuro, oro, oro chiaro, oro scuro, ombra, filetto interno
        Color[] rings = {INK, GOLD_DEEP, GOLD_DARK, GOLD, GOLD_HI, GOLD_DARK, new Color(0x0F0818), new Color(0x4A3670)};
        for (int i = 0; i < rings.length; i++) {
            g.setColor(rings[i]);
            g.drawRect(i, i, s - 1 - 2 * i, s - 1 - 2 * i);
        }
        // alone caldo appena dentro il bordo
        g.setColor(new Color(0x3A2A55));
        g.drawRect(8, 8, s - 17, s - 17);
        g.dispose();
        // angoli: smusso esterno e borchia a rombo
        for (int c = 0; c < 4; c++) {
            boolean right = (c & 1) == 1;
            boolean bottom = (c & 2) == 2;
            int cx = right ? s - 1 : 0;
            int cy = bottom ? s - 1 : 0;
            int dx = right ? -1 : 1;
            int dy = bottom ? -1 : 1;
            clear(img, cx, cy);
            clear(img, cx + dx, cy);
            clear(img, cx, cy + dy);
            diamond(img, cx + dx * 5, cy + dy * 5, 4);
        }
        return img;
    }

    /** Rombo pieno di raggio r con contorno scuro e riflesso. */
    static void diamond(BufferedImage img, int cx, int cy, int r) {
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                int d = Math.abs(x) + Math.abs(y);
                if (d > r) continue;
                Color c = d == r ? GOLD_DEEP : d == r - 1 ? GOLD_DARK : (x < 0 && y < 0 ? GOLD_HI : GOLD);
                px(img, cx + x, cy + y, c);
            }
        }
        px(img, cx, cy, CREAM);
    }

    // ---- placche, riquadro, pillola ------------------------------------------------------------------------

    /** Placca arrotondata: sfumatura verticale, bordo di 1 pixel e filo di luce in alto. */
    static BufferedImage plate(int w, int h, int top, int bottom, int border, int innerLight) {
        BufferedImage img = image(w, h);
        Graphics2D g = g(img);
        g.setPaint(new GradientPaint(0, 0, new Color(top), 0, h, new Color(bottom)));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(border));
        g.drawRect(0, 0, w - 1, h - 1);
        g.setColor(new Color(innerLight));
        g.drawLine(2, 1, w - 3, 1);
        g.dispose();
        roundCorners(img, new Color(border));
        return img;
    }

    static void roundCorners(BufferedImage img, Color border) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[][] corners = {{0, 0, 1, 1}, {w - 1, 0, -1, 1}, {0, h - 1, 1, -1}, {w - 1, h - 1, -1, -1}};
        for (int[] c : corners) {
            clear(img, c[0], c[1]);
            clear(img, c[0] + c[2], c[1]);
            clear(img, c[0], c[1] + c[3]);
            px(img, c[0] + c[2], c[1] + c[3], border);
        }
    }

    /** Riquadro incassato (slice 5): fondo molto scuro, bordo incavato. */
    static BufferedImage inset() {
        int s = 24;
        BufferedImage img = image(s, s);
        Graphics2D g = g(img);
        g.setColor(new Color(0x150E22));
        g.fillRect(0, 0, s, s);
        g.setColor(new Color(0x0A0612));
        g.drawLine(0, 0, s - 1, 0);
        g.drawLine(0, 0, 0, s - 1);
        g.setColor(new Color(0x45325F));
        g.drawLine(0, s - 1, s - 1, s - 1);
        g.drawLine(s - 1, 0, s - 1, s - 1);
        g.setColor(new Color(0x1D1330));
        g.drawRect(1, 1, s - 3, s - 3);
        g.dispose();
        return img;
    }

    /** Pillola (slice 4) per le etichette brevi come "presto". */
    static BufferedImage chip() {
        BufferedImage img = plate(24, 12, 0x453759, 0x32264A, 0x7B67A0, 0x5A4878);
        return img;
    }

    // ---- gemme ---------------------------------------------------------------------------------------------

    /** Gemma 14x14: quadrato arrotondato nel colore della sezione, con riflesso, e glifo 8x8 al centro. */
    static BufferedImage gem(Gem gem) {
        int s = 14;
        BufferedImage img = image(s, s);
        Color base = new Color(gem.color());
        Color dark = mix(base, Color.BLACK, 0.45);
        Color shade = mix(base, Color.BLACK, 0.35);
        Color light = mix(base, Color.WHITE, 0.45);
        Graphics2D g = g(img);
        g.setPaint(new GradientPaint(0, 0, light, 0, s, mix(base, Color.BLACK, 0.12)));
        g.fillRect(0, 0, s, s);
        g.setColor(dark);
        g.drawRect(0, 0, s - 1, s - 1);
        g.setColor(mix(base, Color.WHITE, 0.7));
        g.drawLine(2, 1, 5, 1);
        g.dispose();
        roundCorners(img, dark);
        px(img, 1, 1, light);
        String[] glyph = gem.glyph();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                char c = glyph[y].charAt(x);
                if (c == '#') {
                    px(img, 3 + x, 3 + y + 1, mix(dark, Color.BLACK, 0.4));     // ombra portata
                }
            }
        }
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                char c = glyph[y].charAt(x);
                if (c == '#') px(img, 3 + x, 3 + y, CREAM);
                else if (c == 'o') px(img, 3 + x, 3 + y, shade);
            }
        }
        return img;
    }

    // ---- anteprima -----------------------------------------------------------------------------------------

    static void nineSlice(Graphics2D g, BufferedImage src, int sl, int st, int sr, int sb, int x, int y, int w, int h) {
        int tw = src.getWidth();
        int th = src.getHeight();
        int[] sx = {0, sl, tw - sr, tw};
        int[] sy = {0, st, th - sb, th};
        int[] dx = {x, x + sl, x + w - sr, x + w};
        int[] dy = {y, y + st, y + h - sb, y + h};
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 3; i++) {
                g.drawImage(src, dx[i], dy[j], dx[i + 1], dy[j + 1], sx[i], sy[j], sx[i + 1], sy[j + 1], null);
            }
        }
    }

    /** Foglio con un pannello d'esempio (Home e scheda giocatore) composto con le texture, ingrandito 4x. */
    static void preview(Map<String, BufferedImage> t, Path file) throws IOException {
        int scale = 4;
        BufferedImage sheet = image(2 * 170, 236);
        Graphics2D g = g(sheet);
        g.setColor(new Color(0x4C6B3A));                 // sfondo "mondo" per vedere i bordi
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 8);
        g.setFont(font);
        String[] home = {"Players", "Player Management", "Server Management", "Moderation", "Economy", "Organizations",
            "Logs & Transactions", "Quick Actions"};
        String[] ids = {"players", "player_management", "server_management", "moderation", "economy", "organizations",
            "logs", "quick_actions"};
        int[] counts = {127};
        drawPanel(g, t, 6, 6, 150, 168, "QRM STAFF", false, home, ids, counts, 1, null);
        String[] actions = {"Teleport to", "Bring here", "Freeze", "Heal", "Give item", "View permissions"};
        drawPanel(g, t, 176, 6, 150, 224, "Marco", true, actions, null, new int[0], 5, new int[] {2});
        g.dispose();
        BufferedImage big = image(sheet.getWidth() * scale, sheet.getHeight() * scale);
        Graphics2D bg = g(big);
        bg.drawImage(sheet, 0, 0, big.getWidth(), big.getHeight(), null);
        bg.dispose();
        ImageIO.write(big, "png", file.toFile());
        System.out.println("Preview written to " + file.toAbsolutePath());
    }

    static void drawPanel(Graphics2D g, Map<String, BufferedImage> t, int x, int y, int w, int h, String title,
            boolean card, String[] rows, String[] gemIds, int[] counts, int selected, int[] soon) {
        nineSlice(g, t.get("frame.png"), 12, 12, 12, 12, x, y, w, h);
        int pad = 8;
        g.setColor(GOLD);
        g.drawString(title, x + (w - g.getFontMetrics().stringWidth(title)) / 2, y + pad + 7);
        diamondSmall(g, x + w / 2 - g.getFontMetrics().stringWidth(title) / 2 - 8, y + pad + 4);
        diamondSmall(g, x + w / 2 + g.getFontMetrics().stringWidth(title) / 2 + 8, y + pad + 4);
        int ry = y + pad + 13;
        g.setColor(GOLD_DARK);
        g.drawLine(x + pad + 2, ry, x + w - pad - 2, ry);
        diamondSmall(g, x + w / 2, ry);
        ry += 4;
        if (card) {
            nineSlice(g, t.get("inset.png"), 5, 5, 5, 5, x + pad, ry, w - 2 * pad, 40);
            String[][] info = {{"Online:", "Yes"}, {"Character:", "Marco Rossi"}, {"Job:", "Mechanic"}, {"Organization:", "Benny's"}};
            for (int i = 0; i < info.length; i++) {
                g.setColor(GOLD);
                g.drawString(info[i][0], x + pad + 4, ry + 9 + i * 9);
                g.setColor(CREAM);
                g.drawString(info[i][1], x + pad + 54, ry + 9 + i * 9);
            }
            ry += 44;
        }
        for (int i = 0; i < rows.length; i++) {
            boolean sel = i == selected;
            final int idx = i;
            boolean isSoon = soon != null && i < 5 && java.util.Arrays.stream(soon).noneMatch(v -> v == idx) && card;
            String key = sel ? "row_selected.png" : isSoon ? "row_soon.png" : "row.png";
            nineSlice(g, t.get(key), 6, 6, 6, 6, x + pad, ry, w - 2 * pad, 16);
            int tx = x + pad + 5;
            if (gemIds != null) {
                g.drawImage(t.get("gem_" + gemIds[i] + ".png"), x + pad + 3, ry + 1, null);
                tx = x + pad + 21;
            }
            g.setColor(isSoon ? new Color(0x8E82A6) : CREAM);
            g.drawString(rows[i], tx, ry + 11);
            if (isSoon) {
                nineSlice(g, t.get("chip.png"), 4, 4, 4, 4, x + w - pad - 32, ry + 3, 28, 10);
                g.setColor(new Color(0xCFC2E6));
                g.drawString("presto", x + w - pad - 29, ry + 11);
            } else if (i == 0 && counts.length > 0) {
                g.setColor(new Color(0xA89BB8));
                g.drawString("" + counts[0], x + w - pad - 22, ry + 11);
            } else if (!card) {
                g.setColor(new Color(0xA89BB8));
                g.drawString(">", x + w - pad - 8, ry + 11);
            }
            ry += 17;
        }
        nineSlice(g, t.get("inset.png"), 5, 5, 5, 5, x + pad, y + h - pad - 13, w - 2 * pad, 11);
        g.setColor(new Color(0xA89BB8));
        g.drawString("^v move  > open  < back", x + pad + 4, y + h - pad - 4);
    }

    static void diamondSmall(Graphics2D g, int cx, int cy) {
        g.setColor(GOLD);
        g.fillRect(cx - 1, cy - 1, 3, 3);
        g.setColor(GOLD_HI);
        g.fillRect(cx, cy - 2, 1, 1);
        g.fillRect(cx, cy + 2, 1, 1);
        g.fillRect(cx - 2, cy, 1, 1);
        g.fillRect(cx + 2, cy, 1, 1);
    }
}
