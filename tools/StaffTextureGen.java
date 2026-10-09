import java.awt.BasicStroke;
import java.awt.Color;
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
 * Genera le texture del menu staff (cornice, cartiglio, tessere colorate, slot e icone pixel-art), deterministico, solo JDK.
 * Uso (dalla radice del repo): java tools/StaffTextureGen.java [cartella-staff] [foglio-anteprima.png]
 * Le dimensioni devono coincidere con assets/qrm/theme/default.json (lo verifica ThemeAssetsTest).
 */
public class StaffTextureGen {
    static final Color INK = new Color(0x10161F);
    static final Color WHITE = new Color(0xF4F8FF);
    static final Color AMBER = new Color(0xF2B13C);
    static final Color AMBER_DARK = new Color(0x8A5A12);

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args.length > 0 ? args[0] : "qrm-neoforge/src/main/resources/assets/qrm/textures/gui/staff");
        Files.createDirectories(dir);
        Map<String, BufferedImage> out = new LinkedHashMap<>();
        out.put("frame.png", frame());
        out.put("plate.png", plate());
        out.put("tile_player.png", tile(0x4FD6F0, 0x1B7F95));
        out.put("tile_jobs.png", tile(0xF7C25A, 0xB8761C));
        out.put("tile_orgs.png", tile(0xA98BFF, 0x5B3FB8));
        out.put("tile_perms.png", tile(0xFF8A7A, 0xB23A33));
        out.put("tile_off.png", tile(0x4A5566, 0x2A3441));
        out.put("slot.png", slot());
        out.put("icon_player.png", iconPlayer());
        out.put("icon_jobs.png", iconJobs());
        out.put("icon_orgs.png", iconOrgs());
        out.put("icon_perms.png", iconPerms());
        out.put("icon_back.png", iconBack());
        out.put("icon_refresh.png", iconRefresh());
        out.put("icon_close.png", iconClose());
        for (Map.Entry<String, BufferedImage> e : out.entrySet()) ImageIO.write(e.getValue(), "png", dir.resolve(e.getKey()).toFile());
        System.out.println("Textures written under " + dir.toAbsolutePath());
        if (args.length > 1) sheet(out, Path.of(args[1]));
    }

    static BufferedImage image(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    /** Grafica senza antialiasing: bordi netti, come la pixel-art della reference. */
    static Graphics2D g(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    static Color mix(Color a, Color b, double t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** Cornice a nove fette (slice 10): fondo blu scuro, doppio bordo ambra, rifinitura d'angolo. */
    static BufferedImage frame() {
        int s = 48;
        BufferedImage img = image(s, s);
        Graphics2D g = g(img);
        g.setPaint(new GradientPaint(0, 0, new Color(0x232F44), 0, s, new Color(0x151D2B)));
        g.fillRect(0, 0, s, s);
        g.setColor(INK);
        g.fillRect(0, 0, s, 2);
        g.fillRect(0, s - 2, s, 2);
        g.fillRect(0, 0, 2, s);
        g.fillRect(s - 2, 0, 2, s);
        g.setColor(AMBER);
        g.fillRect(2, 2, s - 4, 3);
        g.fillRect(2, s - 5, s - 4, 3);
        g.fillRect(2, 2, 3, s - 4);
        g.fillRect(s - 5, 2, 3, s - 4);
        g.setColor(AMBER_DARK);
        g.fillRect(5, 5, s - 10, 1);
        g.fillRect(5, s - 6, s - 10, 1);
        g.fillRect(5, 5, 1, s - 10);
        g.fillRect(s - 6, 5, 1, s - 10);
        g.setColor(new Color(0xFFE59A));
        g.fillRect(2, 2, s - 4, 1);
        g.fillRect(2, 2, 1, s - 4);
        // angoli rinforzati
        g.setColor(AMBER);
        for (int[] c : new int[][] {{0, 0}, {s - 9, 0}, {0, s - 9}, {s - 9, s - 9}}) g.fillRect(c[0], c[1], 9, 9);
        g.setColor(INK);
        for (int[] c : new int[][] {{0, 0}, {s - 9, 0}, {0, s - 9}, {s - 9, s - 9}}) {
            g.drawRect(c[0], c[1], 8, 8);
        }
        g.setColor(new Color(0xFFE59A));
        for (int[] c : new int[][] {{1, 1}, {s - 8, 1}, {1, s - 8}, {s - 8, s - 8}}) g.fillRect(c[0], c[1], 5, 2);
        g.dispose();
        return img;
    }

    /** Cartiglio del titolo, 48x20, slice (10, 6, 10, 6). */
    static BufferedImage plate() {
        int w = 48, h = 20;
        BufferedImage img = image(w, h);
        Graphics2D g = g(img);
        g.setColor(INK);
        g.fillRect(0, 0, w, h);
        g.setColor(AMBER);
        g.fillRect(1, 1, w - 2, h - 2);
        g.setColor(new Color(0xFFE59A));
        g.fillRect(2, 2, w - 4, 1);
        g.setColor(AMBER_DARK);
        g.fillRect(2, h - 3, w - 4, 1);
        g.setPaint(new GradientPaint(0, 4, new Color(0x1E2A3E), 0, h - 4, new Color(0x121A28)));
        g.fillRect(4, 4, w - 8, h - 8);
        g.setColor(INK);
        g.drawRect(4, 4, w - 9, h - 9);
        g.dispose();
        return img;
    }

    /** Tessera 32x32 (slice 6): sfumatura, bordo scuro, filo di luce e ombra interna. */
    static BufferedImage tile(int top, int bottom) {
        int s = 32;
        BufferedImage img = image(s, s);
        Graphics2D g = g(img);
        g.setColor(INK);
        g.fillRect(0, 0, s, s);
        g.setPaint(new GradientPaint(0, 2, new Color(top), 0, s - 2, new Color(bottom)));
        g.fillRect(2, 2, s - 4, s - 4);
        Color light = mix(new Color(top), Color.WHITE, 0.55);
        g.setColor(light);
        g.fillRect(2, 2, s - 4, 1);
        g.fillRect(2, 2, 1, s - 4);
        g.setColor(mix(new Color(bottom), Color.BLACK, 0.45));
        g.fillRect(2, s - 3, s - 4, 1);
        g.fillRect(s - 3, 2, 1, s - 4);
        // angoli smussati
        g.setComposite(java.awt.AlphaComposite.Clear);
        g.fillRect(0, 0, 1, 1);
        g.fillRect(s - 1, 0, 1, 1);
        g.fillRect(0, s - 1, 1, 1);
        g.fillRect(s - 1, s - 1, 1, 1);
        g.dispose();
        return img;
    }

    /** Slot della barra rapida, 24x24 (slice 4). */
    static BufferedImage slot() {
        int s = 24;
        BufferedImage img = image(s, s);
        Graphics2D g = g(img);
        g.setColor(INK);
        g.fillRect(0, 0, s, s);
        g.setColor(AMBER_DARK);
        g.fillRect(1, 1, s - 2, s - 2);
        g.setPaint(new GradientPaint(0, 2, new Color(0x3A2A12), 0, s - 2, new Color(0x2A1D0B)));
        g.fillRect(2, 2, s - 4, s - 4);
        g.setColor(new Color(0xFFD27A));
        g.fillRect(2, 1, s - 4, 1);
        g.dispose();
        return img;
    }

    // ---- icone ---------------------------------------------------------------------------------------------

    /** Disegna le forme con contorno scuro (prima tutti i contorni, poi i riempimenti): leggibili su ogni tessera. */
    static void shapes(Graphics2D g, Color fill, int[][] rects) {
        g.setColor(INK);
        for (int[] r : rects) g.fillRect(r[0] - 1, r[1] - 1, r[2] + 2, r[3] + 2);
        g.setColor(fill);
        for (int[] r : rects) g.fillRect(r[0], r[1], r[2], r[3]);
    }

    static BufferedImage iconPlayer() {
        BufferedImage img = image(32, 32);
        Graphics2D g = g(img);
        shapes(g, WHITE, new int[][] {{12, 4, 8, 2}, {10, 6, 12, 9}, {12, 15, 8, 2}, {9, 19, 14, 2}, {7, 21, 18, 3}, {6, 24, 20, 4}});
        g.setColor(new Color(0xBFD0E8));
        g.fillRect(10, 12, 12, 3);
        g.fillRect(6, 25, 20, 3);
        g.dispose();
        return img;
    }

    static BufferedImage iconJobs() {
        BufferedImage img = image(32, 32);
        Graphics2D g = g(img);
        shapes(g, WHITE, new int[][] {{11, 5, 10, 2}, {11, 7, 2, 5}, {19, 7, 2, 5}, {4, 12, 24, 15}});
        g.setColor(new Color(0xBFD0E8));
        g.fillRect(4, 22, 24, 5);
        g.setColor(INK);
        g.fillRect(4, 17, 24, 2);
        g.setColor(AMBER);
        g.fillRect(13, 15, 6, 6);
        g.setColor(INK);
        g.drawRect(13, 15, 5, 5);
        g.dispose();
        return img;
    }

    static BufferedImage iconOrgs() {
        BufferedImage img = image(32, 32);
        Graphics2D g = g(img);
        shapes(g, WHITE, new int[][] {{7, 3, 3, 25}, {4, 27, 9, 2}, {10, 5, 17, 13}});
        g.setColor(new Color(0xBFD0E8));
        g.fillRect(10, 13, 17, 5);
        g.setColor(AMBER);
        g.fillRect(16, 7, 5, 5);
        g.setColor(INK);
        g.drawRect(16, 7, 4, 4);
        g.dispose();
        return img;
    }

    static BufferedImage iconPerms() {
        BufferedImage img = image(32, 32);
        Graphics2D g = g(img);
        shapes(g, WHITE, new int[][] {{6, 4, 20, 4}, {6, 8, 20, 9}, {8, 17, 16, 4}, {10, 21, 12, 3}, {13, 24, 6, 3}});
        g.setColor(new Color(0xBFD0E8));
        g.fillRect(6, 13, 20, 4);
        g.fillRect(8, 17, 16, 4);
        g.setColor(INK);
        g.fillRect(14, 9, 4, 4);
        g.fillRect(15, 13, 2, 7);
        g.setColor(AMBER);
        g.fillRect(15, 10, 2, 2);
        g.dispose();
        return img;
    }

    static BufferedImage iconBack() {
        BufferedImage img = image(16, 16);
        Graphics2D g = g(img);
        g.setColor(INK);
        g.fillPolygon(new int[] {0, 8, 8, 16, 16, 8, 8}, new int[] {8, 0, 4, 4, 12, 12, 16}, 7);
        g.setColor(WHITE);
        g.fillPolygon(new int[] {2, 8, 8, 14, 14, 8, 8}, new int[] {8, 2, 5, 5, 11, 11, 14}, 7);
        g.dispose();
        return img;
    }

    static BufferedImage iconRefresh() {
        BufferedImage img = image(16, 16);
        Graphics2D g = g(img);
        g.setStroke(new BasicStroke(5f));
        g.setColor(INK);
        g.drawArc(3, 3, 10, 10, 60, 270);
        g.setStroke(new BasicStroke(3f));
        g.setColor(WHITE);
        g.drawArc(3, 3, 10, 10, 60, 270);
        g.setColor(INK);
        g.fillPolygon(new int[] {8, 15, 12}, new int[] {0, 0, 8}, 3);
        g.setColor(WHITE);
        g.fillPolygon(new int[] {9, 14, 12}, new int[] {1, 1, 6}, 3);
        g.dispose();
        return img;
    }

    static BufferedImage iconClose() {
        BufferedImage img = image(16, 16);
        Graphics2D g = g(img);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.setColor(INK);
        g.drawLine(3, 3, 12, 12);
        g.drawLine(12, 3, 3, 12);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.setColor(new Color(0xFF8A7A));
        g.drawLine(3, 3, 12, 12);
        g.drawLine(12, 3, 3, 12);
        g.dispose();
        return img;
    }

    /** Foglio di anteprima (non è una risorsa): tessere con le icone sopra, ingrandite 4x. */
    static void sheet(Map<String, BufferedImage> imgs, Path file) throws IOException {
        int scale = 4;
        BufferedImage sheet = image(5 * (32 * scale + 8) + 8, 3 * 40 * scale);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(0x2C3A52));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        String[] tiles = {"tile_player.png", "tile_jobs.png", "tile_orgs.png", "tile_perms.png", "tile_off.png"};
        String[] icons = {"icon_player.png", "icon_jobs.png", "icon_orgs.png", "icon_perms.png"};
        for (int i = 0; i < tiles.length; i++) {
            g.drawImage(imgs.get(tiles[i]), 8 + i * (32 * scale + 8), 8, 32 * scale, 32 * scale, null);
            if (i < icons.length) g.drawImage(imgs.get(icons[i]), 8 + i * (32 * scale + 8), 8, 32 * scale, 32 * scale, null);
        }
        int y = 8 + 32 * scale + 8;
        g.drawImage(imgs.get("frame.png"), 8, y, 48 * scale, 48 * scale, null);
        g.drawImage(imgs.get("plate.png"), 8 + 52 * scale, y, 48 * scale, 20 * scale, null);
        g.drawImage(imgs.get("slot.png"), 8 + 52 * scale, y + 24 * scale, 24 * scale, 24 * scale, null);
        String[] small = {"icon_back.png", "icon_refresh.png", "icon_close.png"};
        for (int i = 0; i < small.length; i++) g.drawImage(imgs.get(small[i]), 8 + 78 * scale + i * 20 * scale, y + 24 * scale, 16 * scale, 16 * scale, null);
        g.dispose();
        ImageIO.write(sheet, "png", file.toFile());
    }
}
