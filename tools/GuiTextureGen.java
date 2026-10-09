import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Genera le texture del tema QRM "urbano moderno scuro" (deterministico, solo JDK).
 * Uso (dalla radice del repo): java tools/GuiTextureGen.java [cartella-gui]
 * Le dimensioni devono coincidere con assets/qrm/theme/default.json (lo verifica ThemeAssetsTest).
 */
public class GuiTextureGen {
    static final Color BG_TOP = new Color(0x1A2330);
    static final Color BG_BOTTOM = new Color(0x121923);
    static final Color EDGE = new Color(0x2F3D52);
    static final Color CYAN = new Color(0x3EC9E6);
    static final Color AMBER = new Color(0xF2B13C);
    static final Color GREY = new Color(0x2A3441);
    static final Color TEXT = new Color(0xE6EDF5);

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : "qrm-neoforge/src/main/resources/assets/qrm/textures/gui");
        Path common = root.resolve("common");
        Path character = root.resolve("character");
        Path hud = root.resolve("hud");
        Path hub = root.resolve("hub");
        Files.createDirectories(common);
        Files.createDirectories(character);
        Files.createDirectories(hud);
        Files.createDirectories(hub);

        write(common, "panel.png", panel(48, 48, 6));
        write(common, "button_normal.png", button(0x2AA9C4, 0x1B7F95, 0x0E4A59, false));
        write(common, "button_hover.png", button(0x4FD6F0, 0x2397B0, 0x0E4A59, false));
        write(common, "button_pressed.png", button(0x15677A, 0x104F5E, 0x0A3340, true));
        write(common, "button_disabled.png", button(0x2A3441, 0x222B36, 0x161C24, false));
        write(common, "button_confirm_normal.png", outlineButton(0x0F2A33, 0x3EC9E6, 0x1B7F95));
        write(common, "button_confirm_hover.png", outlineButton(0x163F4C, 0x7FE3F7, 0x2AA9C4));
        write(common, "button_confirm_pressed.png", outlineButton(0x0A1C22, 0x2397B0, 0x0E4A59));
        write(common, "button_danger_normal.png", outlineButton(0x2A1416, 0xE08A3C, 0x8A4A1E));
        write(common, "button_danger_hover.png", outlineButton(0x432024, 0xF2B13C, 0xC87A3A));
        write(common, "button_danger_pressed.png", outlineButton(0x1C0E10, 0xB8702F, 0x5C3114));
        write(common, "icon_fingerprint.png", iconFingerprint());
        write(common, "icon_play.png", iconPlay());
        write(common, "icon_plus.png", iconPlus());
        write(common, "icon_lock.png", iconLock());
        write(common, "icon_archive.png", iconArchive());

        write(character, "card.png", card(false, false, false));
        write(character, "card_selected.png", card(true, false, false));
        write(character, "card_new.png", card(false, true, false));
        write(character, "card_locked.png", card(false, false, true));
        write(character, "avatar.png", avatar(48, 56));
        write(character, "id_card.png", idCard(48, 48));
        write(character, "photo_frame.png", framed(24, 24, 0x0F1620, CYAN, 2));
        write(character, "field.png", framed(24, 16, 0x0B1119, new Color(0x2F8FA5), 1));
        write(character, "field_focus.png", framed(24, 16, 0x0B1119, CYAN, 2));
        write(character, "toggle.png", framed(20, 16, 0x161E29, new Color(0x3A4757), 1));
        write(character, "toggle_selected.png", framed(20, 16, 0x103A46, CYAN, 2));
        write(hud, "panel.png", hudPanel());
        write(hub, "tab.png", framed(40, 16, 0x161E29, new Color(0x3A4757), 1));
        write(hub, "tab_selected.png", framed(40, 16, 0x103A46, CYAN, 2));
        System.out.println("Textures written under " + root.toAbsolutePath());
    }

    static void write(Path dir, String name, BufferedImage img) throws IOException {
        ImageIO.write(img, "png", dir.resolve(name).toFile());
    }

    static BufferedImage image(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    static Graphics2D g(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    /** Pannello a vetro opaco: gradiente scuro, bordo sottile, filo di luce in alto. */
    static BufferedImage panel(int w, int h, int radius) {
        BufferedImage img = image(w, h);
        Graphics2D g = g(img);
        g.setPaint(new GradientPaint(0, 0, alpha(BG_TOP, 238), 0, h, alpha(BG_BOTTOM, 238)));
        g.fill(new RoundRectangle2D.Float(0, 0, w, h, radius * 2, radius * 2));
        g.setColor(EDGE);
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, radius * 2, radius * 2));
        g.setColor(alpha(Color.WHITE, 26));
        g.drawLine(radius, 1, w - radius, 1);
        g.dispose();
        return img;
    }

    static BufferedImage button(int top, int bottom, int edge, boolean inset) {
        int w = 32, h = 20;
        BufferedImage img = image(w, h);
        Graphics2D g = g(img);
        g.setPaint(new GradientPaint(0, 0, new Color(top), 0, h, new Color(bottom)));
        g.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));
        g.setColor(new Color(edge));
        g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 8, 8));
        g.setColor(alpha(inset ? Color.BLACK : Color.WHITE, inset ? 60 : 55));
        g.drawLine(4, 1, w - 5, 1);
        g.dispose();
        return img;
    }

    /** Pulsante a contorno: fondo scuro, bordo colorato, leggero alone interno (nine-slice 4). */
    static BufferedImage outlineButton(int fill, int border, int glow) {
        int w = 32, h = 20;
        BufferedImage img = image(w, h);
        Graphics2D g = g(img);
        g.setColor(new Color(fill));
        g.fillRect(0, 0, w, h);
        g.setColor(alpha(new Color(glow), 120));
        g.drawRect(1, 1, w - 3, h - 3);
        g.setColor(new Color(border));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(1, 1, w - 2, h - 2);
        g.dispose();
        return img;
    }

    /** Rettangolo pieno con bordo di {@code border} pixel (nine-slice con slice = border + 1): campi, toggle, foto. */
    static BufferedImage framed(int w, int h, int fill, Color edge, int border) {
        BufferedImage img = image(w, h);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(fill));
        g.fillRect(0, 0, w, h);
        g.setColor(edge);
        for (int i = 0; i < border; i++) g.drawRect(i, i, w - 1 - 2 * i, h - 1 - 2 * i);
        g.dispose();
        return img;
    }

    /** Carta d'identità: fondo a gradiente, bordo a doppia linea e quattro LED agli angoli (nine-slice 10). */
    static BufferedImage idCard(int w, int h) {
        BufferedImage img = image(w, h);
        Graphics2D g = g(img);
        g.setPaint(new GradientPaint(0, 0, new Color(0x14202E), 0, h, new Color(0x0E1620)));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(0x3A5570));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(1, 1, w - 2, h - 2);
        g.setColor(alpha(CYAN, 70));
        g.setStroke(new BasicStroke(1f));
        g.drawRect(4, 4, w - 9, h - 9);
        g.setColor(CYAN);
        g.fillRect(3, 3, 3, 3);
        g.fillRect(w - 6, h - 6, 3, 3);
        g.setColor(AMBER);
        g.fillRect(w - 6, 3, 3, 3);
        g.fillRect(3, h - 6, 3, 3);
        g.dispose();
        return img;
    }

    /** Icona impronta digitale: archi concentrici ciano su riquadro scuro. */
    static BufferedImage iconFingerprint() {
        int s = 20;
        BufferedImage img = image(s, s);
        Graphics2D g = g(img);
        g.setColor(new Color(0x14222E));
        g.fillRoundRect(0, 0, s, s, 4, 4);
        g.setColor(alpha(CYAN, 120));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(0, 0, s - 1, s - 1, 4, 4);
        g.setColor(CYAN);
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int r = 2; r <= 7; r += 2) {
            g.drawArc(s / 2 - r, s / 2 - r - 1, 2 * r, 2 * r + 2, 20, 320);
        }
        g.drawLine(s / 2, s / 2 - 1, s / 2, s - 4);
        g.dispose();
        return img;
    }

    /** Pannello dell'HUD 32x32 (nine-slice 6): fondo scuro appena trasparente, bordo azzurro e filo ciano in alto. */
    static BufferedImage hudPanel() {
        int w = 32, h = 32;
        BufferedImage img = image(w, h);
        Graphics2D g = img.createGraphics();
        g.setColor(alpha(new Color(0x0E1620), 215));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(0x3A5570));
        g.setStroke(new BasicStroke(1f));
        g.drawRect(0, 0, w - 1, h - 1);
        g.setColor(CYAN);
        g.fillRect(1, 1, w - 2, 2);
        g.dispose();
        return img;
    }

    static BufferedImage iconPlay() {
        BufferedImage img = image(16, 16);
        Graphics2D g = g(img);
        Path2D p = new Path2D.Float();
        p.moveTo(4, 2);
        p.lineTo(13, 8);
        p.lineTo(4, 14);
        p.closePath();
        g.setColor(TEXT);
        g.fill(p);
        g.dispose();
        return img;
    }

    static BufferedImage iconPlus() {
        BufferedImage img = image(16, 16);
        Graphics2D g = g(img);
        g.setColor(AMBER);
        g.fillRoundRect(7, 2, 2, 12, 1, 1);
        g.fillRoundRect(2, 7, 12, 2, 1, 1);
        g.dispose();
        return img;
    }

    static BufferedImage iconLock() {
        BufferedImage img = image(16, 16);
        Graphics2D g = g(img);
        g.setColor(new Color(0x8A98A8));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new RoundRectangle2D.Float(4.5f, 1.5f, 7, 8, 6, 6));
        g.fillRoundRect(2, 7, 12, 8, 3, 3);
        g.setColor(new Color(0x121923));
        g.fillOval(7, 9, 2, 2);
        g.dispose();
        return img;
    }

    static BufferedImage iconArchive() {
        BufferedImage img = image(16, 16);
        Graphics2D g = g(img);
        g.setColor(TEXT);
        g.fillRoundRect(1, 2, 14, 4, 2, 2);
        g.fillRoundRect(2, 6, 12, 8, 2, 2);
        g.setColor(new Color(0x121923));
        g.fillRoundRect(6, 8, 4, 2, 1, 1);
        g.dispose();
        return img;
    }

    /**
     * Carta del carosello 120x160 (nine-slice 8) nello stile della carta d'identità: bordo a doppia linea e LED agli
     * angoli. Normale, selezionata (bordo ciano con alone), "nuovo" (tratteggiata ambra) o bloccata (grigia).
     */
    static BufferedImage card(boolean selected, boolean isNew, boolean locked) {
        int w = 120, h = 160;
        BufferedImage img = image(w, h);
        Graphics2D g = img.createGraphics();
        if (isNew) {
            g.setColor(alpha(new Color(0x0E1620), 170));
            g.fillRect(0, 0, w, h);
            g.setColor(alpha(AMBER, 210));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {6f, 4f}, 0f));
            g.drawRect(1, 1, w - 3, h - 3);
            g.dispose();
            return img;
        }
        Color top = locked ? new Color(0x181E27) : new Color(0x14202E);
        Color bottom = locked ? new Color(0x11161D) : new Color(0x0E1620);
        g.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        g.fillRect(0, 0, w, h);
        g.setStroke(new BasicStroke(1f));
        if (selected) {
            g.setColor(alpha(CYAN, 45));
            g.drawRect(2, 2, w - 5, h - 5);
            g.setColor(alpha(CYAN, 90));
            g.drawRect(3, 3, w - 7, h - 7);
        } else {
            g.setColor(alpha(locked ? GREY : CYAN, locked ? 90 : 70));
            g.drawRect(4, 4, w - 9, h - 9);
        }
        g.setColor(selected ? CYAN : locked ? GREY : new Color(0x3A5570));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(1, 1, w - 2, h - 2);
        Color led1 = locked ? GREY : alpha(CYAN, selected ? 255 : 150);
        Color led2 = locked ? GREY : alpha(AMBER, selected ? 255 : 150);
        g.setColor(led1);
        g.fillRect(3, 3, 3, 3);
        g.fillRect(w - 6, h - 6, 3, 3);
        g.setColor(led2);
        g.fillRect(w - 6, 3, 3, 3);
        g.fillRect(3, h - 6, 3, 3);
        g.dispose();
        return img;
    }

    /** Riquadro foto con silhouette segnaposto. */
    static BufferedImage avatar(int w, int h) {
        BufferedImage img = image(w, h);
        Graphics2D g = g(img);
        g.setPaint(new GradientPaint(0, 0, new Color(0x26364A), 0, h, new Color(0x1C2735)));
        g.fillRoundRect(0, 0, w, h, 6, 6);
        g.setColor(new Color(0x52667F));
        g.fill(new Ellipse2D.Float(w / 2f - 9, 10, 18, 18));
        g.fill(new Ellipse2D.Float(w / 2f - 17, 31, 34, 36));
        g.setColor(new Color(0x34475F));
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 6, 6));
        g.dispose();
        return img;
    }
}
