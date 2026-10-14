import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Genera le texture a nove fette della schermata dell'ATM (solo JDK, deterministico): cornice verde con borchie
 * dorate, pannelli, barra del titolo, campi e pulsanti. Le icone 16x16 non si toccano.
 * Uso (dalla radice del repo): java tools/AtmUiTextureGen.java
 */
public class AtmUiTextureGen {
    private static final Color NIGHT = new Color(0x0F161C);
    private static final Color BODY = new Color(0x1A2730);
    private static final Color PANEL = new Color(0x22323D);
    private static final Color WELL = new Color(0x131D24);
    private static final Color GREEN = new Color(0x3C9570);
    private static final Color GREEN_DARK = new Color(0x1F5440);
    private static final Color GOLD = new Color(0xE2B24A);
    private static final Color GOLD_DARK = new Color(0x8A5E16);
    private static final Color SLATE = new Color(0x34495A);
    private static final Color SLATE_HI = new Color(0x587489);
    private static final Color SLATE_LO = new Color(0x1E2D38);

    public static void main(String[] args) throws IOException {
        Path out = Path.of("qrm-bank/src/main/resources/assets/qrm_bank/textures/gui/atm");
        Files.createDirectories(out);
        save(out, "frame", frame());
        save(out, "panel", panel(24, 24, GREEN_DARK, PANEL, GREEN));
        save(out, "title_bar", panel(48, 24, GREEN_DARK, new Color(0x2A3C48), GREEN));
        save(out, "inset", panel(24, 24, NIGHT, WELL, new Color(0x0A1014)));
        save(out, "lcd_plate", panel(32, 22, NIGHT, new Color(0x0C2218), new Color(0x07140E)));
        save(out, "icon_job", icon(new String[] {
                "................", "................", ".....######.....", ".....#....#.....", ".....#....#.....",
                "..############..", "..#oooooooooo#..", "..#oooooooooo#..", "..############..", "..#oooo##oooo#..",
                "..#oooo##oooo#..", "..#oooooooooo#..", "..############..", "................", "................",
                "................"}, new Color(0xC08A3E)));
        save(out, "icon_vip", icon(new String[] {
                ".......##.......", ".......##.......", "......#oo#......", "......#oo#......", "..####oooo####..",
                ".#oooooooooooo#.", "..#oooooooooo#..", "...#oooooooo#...", "....#oooooo#....", "....#oooooo#....",
                "...#ooo##ooo#...", "...#oo#..#oo#...", "..#oo#....#oo#..", "..##........##..", "................",
                "................"}, GOLD));
        save(out, "field", field(false));
        save(out, "field_focus", field(true));
        for (String[] s : new String[][] {{"button", "normal"}, {"button", "hover"}, {"button", "pressed"}}) {
            save(out, s[0] + "_" + s[1], button(SLATE, SLATE_HI, SLATE_LO, NIGHT, s[1]));
            save(out, "button_confirm_" + s[1], button(new Color(0x3A3220), new Color(0xE2B24A), new Color(0x1E190C),
                    GOLD_DARK, s[1]));
            save(out, "button_danger_" + s[1], button(new Color(0x6A2A2A), new Color(0xC25454), new Color(0x2E1010),
                    NIGHT, s[1]));
        }
        save(out, "button_disabled", button(new Color(0x2A3640), new Color(0x2A3640), new Color(0x2A3640), NIGHT, "normal"));
    }

    private static void save(Path dir, String name, BufferedImage img) throws IOException {
        ImageIO.write(img, "png", dir.resolve(name + ".png").toFile());
    }

    private static BufferedImage image(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    private static void cutCorners(Graphics2D g, int w, int h) {
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, 1, 1);
        g.fillRect(w - 1, 0, 1, 1);
        g.fillRect(0, h - 1, 1, 1);
        g.fillRect(w - 1, h - 1, 1, 1);
        g.setComposite(AlphaComposite.SrcOver);
    }

    /** Cornice esterna 48x48 (fetta 8): scuro, filetto verde interno e una borchia dorata per angolo. */
    private static BufferedImage frame() {
        int s = 48;
        BufferedImage img = image(s, s);
        Graphics2D g = img.createGraphics();
        g.setColor(NIGHT);
        g.fillRect(0, 0, s, s);
        g.setColor(GREEN_DARK);
        g.fillRect(1, 1, s - 2, s - 2);
        g.setColor(GREEN);
        g.fillRect(2, 2, s - 4, s - 4);
        g.setColor(GREEN_DARK);
        g.fillRect(3, 3, s - 6, s - 6);
        g.setColor(BODY);
        g.fillRect(4, 4, s - 8, s - 8);
        g.setColor(NIGHT);
        g.fillRect(4, 4, s - 8, 1);
        g.fillRect(4, 4, 1, s - 8);
        for (int[] c : new int[][] {{0, 0}, {s - 8, 0}, {0, s - 8}, {s - 8, s - 8}}) {
            g.setColor(GOLD_DARK);
            g.fillRect(c[0], c[1], 8, 8);
            g.setColor(GOLD);
            g.fillRect(c[0] + 1, c[1] + 1, 6, 6);
            g.setColor(NIGHT);
            g.fillRect(c[0] + 3, c[1] + 3, 2, 2);
        }
        g.dispose();
        return img;
    }

    /** Pannello con bordo scuro, filetto colorato e riempimento. */
    private static BufferedImage panel(int w, int h, Color outer, Color fill, Color line) {
        BufferedImage img = image(w, h);
        Graphics2D g = img.createGraphics();
        g.setColor(outer);
        g.fillRect(0, 0, w, h);
        g.setColor(line);
        g.fillRect(1, 1, w - 2, h - 2);
        g.setColor(outer);
        g.fillRect(2, 2, w - 4, h - 4);
        g.setColor(fill);
        g.fillRect(3, 3, w - 6, h - 6);
        cutCorners(g, w, h);
        g.dispose();
        return img;
    }

    /** Icona 16x16 da una griglia: {@code #} contorno, {@code o} colore, il resto trasparente. */
    private static BufferedImage icon(String[] rows, Color fill) {
        BufferedImage img = image(16, 16);
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < 16; x++) {
                char c = rows[y].charAt(x);
                if (c == '#') img.setRGB(x, y, NIGHT.getRGB());
                else if (c == 'o') img.setRGB(x, y, fill.getRGB());
            }
        }
        return img;
    }

    private static BufferedImage field(boolean focus) {
        int w = 32, h = 20;
        BufferedImage img = image(w, h);
        Graphics2D g = img.createGraphics();
        g.setColor(focus ? GOLD : NIGHT);
        g.fillRect(0, 0, w, h);
        g.setColor(WELL);
        g.fillRect(1, 1, w - 2, h - 2);
        g.setColor(new Color(0x0A1014));
        g.fillRect(1, 1, w - 2, 1);
        g.fillRect(1, 1, 1, h - 2);
        g.setColor(focus ? new Color(0x1B2A30) : new Color(0x1C2A33));
        g.fillRect(1, h - 2, w - 2, 1);
        cutCorners(g, w, h);
        g.dispose();
        return img;
    }

    private static BufferedImage button(Color face, Color hi, Color lo, Color border, String state) {
        int w = 32, h = 20;
        BufferedImage img = image(w, h);
        Graphics2D g = img.createGraphics();
        boolean pressed = state.equals("pressed");
        boolean hover = state.equals("hover");
        g.setColor(border);
        g.fillRect(0, 0, w, h);
        Color body = hover ? lighter(face) : face;
        g.setColor(body);
        g.fillRect(1, 1, w - 2, h - 2);
        g.setColor(pressed ? lo : hi);
        g.fillRect(1, 1, w - 2, 1);
        g.fillRect(1, 1, 1, h - 2);
        g.setColor(pressed ? hi : lo);
        g.fillRect(1, h - 2, w - 2, 1);
        g.fillRect(w - 2, 1, 1, h - 2);
        if (pressed) {
            g.setColor(darker(body));
            g.fillRect(2, 2, w - 4, h - 4);
        }
        cutCorners(g, w, h);
        g.dispose();
        return img;
    }

    private static Color lighter(Color c) {
        return new Color(Math.min(255, c.getRed() + 18), Math.min(255, c.getGreen() + 18), Math.min(255, c.getBlue() + 18));
    }

    private static Color darker(Color c) {
        return new Color(Math.max(0, c.getRed() - 14), Math.max(0, c.getGreen() - 14), Math.max(0, c.getBlue() - 14));
    }
}
