package dev.qrm.police.client;

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
final class MdtWidgets {
    static final int BG = 0xFF0B1220;
    static final int PANEL = 0xFF111C30;
    static final int PANEL_HI = 0xFF18263F;
    static final int LINE = 0xFF1E3A5F;
    static final int BLUE = 0xFF3B82F6;
    static final int RED = 0xFFEF4444;
    static final int AMBER = 0xFFF59E0B;
    static final int GREEN = 0xFF22C55E;
    static final int TEXT = 0xFFE5E7EB;
    static final int DIM = 0xFF8EA0B8;

    private MdtWidgets() {}

    static int alpha(int rgb, int a) {
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int line, int fill) {
        g.fill(x, y, x + w, y + h, fill);
        outline(g, x, y, w, h, line);
    }

    /** Riquadro incassato che contiene una lista. */
    static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        box(g, x, y, w, h, 0xFF162238, 0xFF080D16);
    }

    /** Taglia il testo con i puntini se non sta nella larghezza data. */
    static String fit(Font font, String s, int width) {
        if (font.width(s) <= width) return s;
        String cut = s;
        while (cut.length() > 1 && font.width(cut + "...") > width) cut = cut.substring(0, cut.length() - 1);
        return cut + "...";
    }

    static void click() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.7F));
    }

    /** Disegno libero di un widget: riga di lista, scheda con più testi. */
    @FunctionalInterface
    interface Painter {
        void paint(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hovered, boolean active);
    }

    /** Pulsante o riga cliccabile. */
    static final class Btn extends AbstractWidget {
        private final Runnable onPress;
        private final int accent;
        private final Painter painter;
        private final boolean centered;

        Btn(int x, int y, int w, int h, Component label, int accent, Runnable onPress) {
            super(x, y, w, h, label);
            this.accent = accent;
            this.onPress = onPress;
            this.painter = null;
            this.centered = true;
        }

        Btn(int x, int y, int w, int h, Painter painter, Runnable onPress) {
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
            int line = this.active ? alpha(accent, hot ? 0xFF : 0xA0) : alpha(accent, 0x40);
            int fill = this.active ? alpha(accent, hot ? 0x40 : 0x1C) : alpha(accent, 0x0C);
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
    static final class Field extends EditBox {
        private final int fx, fy, fw, fh;

        Field(Font font, int x, int y, int w, int h, Component hint) {
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
            box(g, fx, fy, fw, fh, isFocused() ? BLUE : LINE, 0xFF0A1424);
            super.extractWidgetRenderState(g, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= fx && mouseX < fx + fw && mouseY >= fy && mouseY < fy + fh;
        }
    }
}
