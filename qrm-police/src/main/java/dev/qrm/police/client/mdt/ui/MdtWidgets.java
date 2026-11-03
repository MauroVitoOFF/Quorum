package dev.qrm.police.client.mdt.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Stile e widget dell'MDT: solo rettangoli colorati e testo, nessuna texture. Tema scuro, accenti blu. */
public final class MdtWidgets {
    public static final int BG = 0xFF131925;
    public static final int PANEL = 0xFF1A2230;
    public static final int PANEL_HI = 0xFF232D3F;
    public static final int LINE = 0xFF2C384B;
    public static final int BLUE = 0xFF5E92DE;
    public static final int RED = 0xFFDF6C6C;
    public static final int AMBER = 0xFFE0A54E;
    public static final int GREEN = 0xFF5CC088;
    public static final int ORANGE = 0xFFE08B4E;
    public static final int YELLOW = 0xFFD8B84E;
    public static final int TEXT = 0xFFDDE2E9;
    public static final int TEXT_SOFT = 0xFFB4BDCB;
    public static final int DIM = 0xFF8F9AAB;
    public static final int INSET_LINE = 0xFF252F40;
    public static final int INSET_FILL = 0xFF0F141E;
    public static final int FIELD_FILL = 0xFF111723;

    private MdtWidgets() {}

    public static int alpha(int rgb, int a) {
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int line, int fill) {
        g.fill(x, y, x + w, y + h, fill);
        outline(g, x, y, w, h, line);
    }

    /** Riquadro incassato che contiene una lista. */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        box(g, x, y, w, h, INSET_LINE, INSET_FILL);
    }

    /** Taglia il testo con i puntini se non sta nella larghezza data. */
    public static String fit(Font font, String s, int width) {
        if (font.width(s) <= width) return s;
        String cut = s;
        while (cut.length() > 1 && font.width(cut + "...") > width) cut = cut.substring(0, cut.length() - 1);
        return cut + "...";
    }

    public static void click() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.7F));
    }

    /** Disegno libero di un widget: riga di lista, scheda con più testi. */
    @FunctionalInterface
    public interface Painter {
        void paint(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hovered, boolean active);
    }

    /** Pulsante o riga cliccabile. */
    public static final class Btn extends AbstractWidget {
        private final Runnable onPress;
        private final int accent;
        private final Painter painter;
        private final boolean centered;

        public Btn(int x, int y, int w, int h, Component label, int accent, Runnable onPress) {
            super(x, y, w, h, label);
            this.accent = accent;
            this.onPress = onPress;
            this.painter = null;
            this.centered = true;
        }

        public Btn(int x, int y, int w, int h, Painter painter, Runnable onPress) {
            super(x, y, w, h, Component.empty());
            this.accent = BLUE;
            this.onPress = onPress;
            this.painter = painter;
            this.centered = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            boolean hot = this.active && isHoveredOrFocused();
            if (painter != null) {
                painter.paint(g, getX(), getY(), getWidth(), getHeight(), hot, this.active);
                return;
            }
            int line = this.active ? alpha(accent, hot ? 0xC8 : 0x78) : alpha(accent, 0x38);
            int fill = this.active ? alpha(accent, hot ? 0x34 : 0x16) : alpha(accent, 0x0A);
            box(g, getX(), getY(), getWidth(), getHeight(), line, fill);
            Font font = Minecraft.getInstance().font;
            String s = fit(font, getMessage().getString(), getWidth() - 6);
            int color = this.active ? TEXT : DIM;
            int tx = centered ? getX() + (getWidth() - font.width(s)) / 2 : getX() + 4;
            g.text(font, s, tx, getY() + (getHeight() - 8) / 2, color, false);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            click();
            onPress.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    /** Casella di testo scura con bordo blu quando ha il focus. */
    public static final class Field extends EditBox {
        private final int fx, fy, fw, fh;

        public Field(Font font, int x, int y, int w, int h, Component hint) {
            super(font, x + 4, y + (h - font.lineHeight) / 2 + 1, Math.max(1, w - 8), font.lineHeight, hint);
            this.fx = x;
            this.fy = y;
            this.fw = w;
            this.fh = h;
            setBordered(false);
            setTextShadow(false);
            setTextColor(TEXT);
            setHint(hint);
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            box(g, fx, fy, fw, fh, isFocused() ? BLUE : LINE, FIELD_FILL);
            super.extractWidgetRenderState(g, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= fx && mouseX < fx + fw && mouseY >= fy && mouseY < fy + fh;
        }
    }
}
