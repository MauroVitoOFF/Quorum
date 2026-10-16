package dev.qrm.boss.client;

import dev.qrm.client.api.theme.ThemeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Pulsante del menu boss, con lo stesso aspetto di quelli della banca: contorno colorato, riempimento appena
 * accennato e un bagliore che sale al passaggio del mouse. La scheda di sezione è lo stesso widget, ma solo testo con
 * una sottolineatura.
 */
final class BossButton extends AbstractWidget {
    enum Style {
        DEFAULT(BossStyle.BLUE), CONFIRM(BossStyle.GOLD), DANGER(BossStyle.RED);

        private final int color;
        Style(int color) {
            this.color = color;
        }
    }

    private final Runnable onPress;
    private final Style style;
    private final String icon;
    private final boolean tab;
    private boolean selected;
    private boolean pressed;
    private float glow;
    private long glowAt;

    private BossButton(int x, int y, int w, int h, Component label, Runnable onPress, Style style, String icon,
            boolean tab) {
        super(x, y, w, h, label);
        this.onPress = onPress;
        this.style = style;
        this.icon = icon;
        this.tab = tab;
    }

    static BossButton button(int x, int y, int w, int h, Component label, String icon, Style style, Runnable onPress) {
        return new BossButton(x, y, w, h, label, onPress, style, icon, false);
    }

    /** Larghezza di una scheda: solo il testo, con un po' d'aria ai lati. */
    static int tabWidth(String label) {
        return BossStyle.width(Minecraft.getInstance().font, label) + 14;
    }

    static BossButton tab(int x, int y, int h, Component label, boolean selected, Runnable onPress) {
        BossButton b = new BossButton(x, y, tabWidth(label.getString()), h, label, onPress, Style.DEFAULT, null, true);
        b.selected = selected;
        return b;
    }

    /** Vero per un pulsante a interruttore acceso: prende l'accento oro. */
    BossButton selected(boolean value) {
        this.selected = value;
        return this;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float target = this.active && isHoveredOrFocused() ? 1f : 0f;
        glow += (target - glow) * Math.min(1f, (now - glowAt) / 100f);
        glowAt = now;
        var font = Minecraft.getInstance().font;
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        String label = getMessage().getString();

        if (tab) {
            int color = selected ? BossStyle.color("qrm_boss.gold")
                    : BossStyle.color(isHoveredOrFocused() ? "qrm_boss.cream" : "qrm_boss.ink_soft");
            int ty = y + (h - BossStyle.TEXT_H) / 2 - 1;
            BossStyle.centered(g, font, label, x + w / 2, ty, color);
            int textW = BossStyle.width(font, label);
            g.fill(x + (w - textW) / 2, ty + BossStyle.TEXT_H + 3, x + (w + textW) / 2, ty + BossStyle.TEXT_H + 4,
                    selected ? BossStyle.GOLD : BossStyle.alpha(BossStyle.BLUE, isHoveredOrFocused() ? 0xCC : 0x77));
            return;
        }

        int base = selected && style == Style.DEFAULT ? BossStyle.GOLD : style.color;
        int line;
        int tint;
        if (!this.active) {
            line = BossStyle.alpha(base, 0x40);
            tint = 0;
        } else if (pressed && isHovered()) {
            line = base;
            tint = BossStyle.alpha(base, 0x44);
        } else {
            line = BossStyle.alpha(base, 0x90 + (int) (glow * 0x6F));
            tint = BossStyle.alpha(base, (selected ? 0x30 : 0x12) + (int) (glow * 0x1E));
        }
        BossStyle.box(g, x, y, w, h, line, tint);

        int size = Math.min(16, h - 4);
        int color = BossStyle.color(this.active ? "qrm_boss.cream" : "qrm_boss.disabled");
        if (!this.active) color = BossStyle.alpha(color, 0x77);
        int textW = BossStyle.width(font, label);
        int iconW = icon == null ? 0 : size + 2;
        int startX = x + (w - textW - iconW) / 2;
        if (icon != null) ThemeRenderer.draw(g, icon, startX, y + (h - size) / 2, size, size);
        BossStyle.text(g, font, label, startX + iconW, y + (h - BossStyle.TEXT_H) / 2 + 1, color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        if (handled) pressed = true;
        return handled;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        pressed = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.isActive()) return false;
        if (event.isSelection()) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            onPress.run();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
