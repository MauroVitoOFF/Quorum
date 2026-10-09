import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Genera le texture del modulo banca (solo JDK, deterministico): blocco e banconota 16x16 e, per il menu staff,
 * la tessera e l'icona 32x32 in {@code textures/gui/staff}.
 * Uso (dalla radice del repo): java tools/BankTextureGen.java
 */
public class BankTextureGen {
    public static void main(String[] args) throws IOException {
        Path res = Path.of("qrm-bank/src/main/resources/assets/qrm_bank/textures");
        Files.createDirectories(res.resolve("block"));
        Files.createDirectories(res.resolve("item"));
        ImageIO.write(atm(), "png", res.resolve("block/atm.png").toFile());
        ImageIO.write(cash(), "png", res.resolve("item/cash.png").toFile());
        Files.createDirectories(res.resolve("gui/staff"));
        ImageIO.write(staffTile(), "png", res.resolve("gui/staff/tile.png").toFile());
        ImageIO.write(staffIcon(), "png", res.resolve("gui/staff/icon.png").toFile());
    }

    private static Graphics2D g(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        return g;
    }

    private static final Color INK = new Color(0x10161F);
    private static final Color WHITE = new Color(0xF4F8FF);

    /** Tessera 32x32 (slice 6) blu acciaio, nello stesso stile delle tessere {@code staff.tile.*} di QRM. */
    private static BufferedImage staffTile() {
        int s = 32;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = g(img);
        g.setColor(INK);
        g.fillRect(0, 0, s, s);
        g.setPaint(new java.awt.GradientPaint(0, 2, new Color(0x7FB4E8), 0, s - 2, new Color(0x2C5C94)));
        g.fillRect(2, 2, s - 4, s - 4);
        g.setColor(new Color(0xC4DFF7));
        g.fillRect(2, 2, s - 4, 1);
        g.fillRect(2, 2, 1, s - 4);
        g.setColor(new Color(0x183456));
        g.fillRect(2, s - 3, s - 4, 1);
        g.fillRect(s - 3, 2, 1, s - 4);
        g.setComposite(java.awt.AlphaComposite.Clear);
        g.fillRect(0, 0, 1, 1);
        g.fillRect(s - 1, 0, 1, 1);
        g.fillRect(0, s - 1, 1, 1);
        g.fillRect(s - 1, s - 1, 1, 1);
        g.dispose();
        return img;
    }

    private static void shapes(Graphics2D g, Color fill, int[][] rects) {
        g.setColor(INK);
        for (int[] r : rects) g.fillRect(r[0] - 1, r[1] - 1, r[2] + 2, r[3] + 2);
        g.setColor(fill);
        for (int[] r : rects) g.fillRect(r[0], r[1], r[2], r[3]);
    }

    /** Icona 32x32: edificio con frontone, colonne e gradini. */
    private static BufferedImage staffIcon() {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = g(img);
        // frontone a gradini, colonne, cornice e gradini
        shapes(g, WHITE, new int[][] {
            {14, 4, 4, 2}, {11, 6, 10, 2}, {8, 8, 16, 2}, {5, 10, 22, 2},
            {5, 13, 22, 2},
            {7, 16, 3, 8}, {12, 16, 3, 8}, {17, 16, 3, 8}, {22, 16, 3, 8},
            {5, 25, 22, 2}, {3, 27, 26, 2}});
        g.setColor(new Color(0xBFD0E8));
        g.fillRect(5, 10, 22, 2);
        g.fillRect(9, 16, 1, 8);
        g.fillRect(14, 16, 1, 8);
        g.fillRect(19, 16, 1, 8);
        g.fillRect(24, 16, 1, 8);
        g.setColor(new Color(0xF2B13C));
        g.fillRect(15, 8, 2, 2);
        g.dispose();
        return img;
    }

    private static BufferedImage atm() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = g(img);
        g.setColor(new Color(0x2F3D52));
        g.fillRect(0, 0, 16, 16);
        g.setColor(new Color(0x1A2330));
        g.drawRect(0, 0, 15, 15);
        g.setColor(new Color(0x3EC9E6));
        g.fillRect(3, 2, 10, 5);          // schermo
        g.setColor(new Color(0x121923));
        g.fillRect(4, 3, 8, 3);
        g.setColor(new Color(0xE6EDF5));
        for (int r = 0; r < 2; r++) for (int c = 0; c < 3; c++) g.fillRect(4 + c * 3, 9 + r * 3, 2, 2); // tastiera
        g.setColor(new Color(0xF2B13C));
        g.fillRect(12, 9, 2, 5);          // fessura contanti
        g.dispose();
        return img;
    }

    private static BufferedImage cash() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = g(img);
        g.setColor(new Color(0x2E7D4F));
        g.fillRect(1, 4, 14, 8);
        g.setColor(new Color(0x1B5233));
        g.drawRect(1, 4, 13, 7);
        g.setColor(new Color(0xC9F0D5));
        g.fillOval(6, 6, 4, 4);
        g.fillRect(3, 6, 1, 1);
        g.fillRect(12, 9, 1, 1);
        g.dispose();
        return img;
    }
}
