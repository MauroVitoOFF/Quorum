package dev.qrm.bank.client;

import dev.qrm.client.api.theme.ThemeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Pulsante dell'ATM, disegnati con le texture del modulo (chiavi {@code qrm_bank.ui.*}). */
final class AtmButton extends AbstractWidget {
    enum Style {
        DEFAULT("button"), CONFIRM("button_confirm"), DANGER("button_danger");

        private final String prefix;
        Style(String prefix) {
            this.prefix = prefix;
        }
    }

    private final Runnable onPress;
    private final Style style;
    private final String icon;
    private boolean iconOnly;
    private String trailing;
    private boolean pressed;
    private float glow;
    private long glowAt;

    private AtmButton(int x, int y, int w, int h, Component label, Runnable onPress, Style style, String icon) {
        super(x, y, w, h, label);
        this.onPress = onPress;
        this.style = style;
        this.icon = icon;
    }

    static AtmButton button(int x, int y, int w, int h, Component label, String icon, Style style, Runnable onPress) {
        return new AtmButton(x, y, w, h, label, onPress, style, icon);
    }

    /** Pulsante con la sola icona; il nome compare come suggerimento. */
    static AtmButton icon(int x, int y, int w, int h, Component label, String icon, Style style, Runnable onPress) {
        AtmButton b = new AtmButton(x, y, w, h, label, onPress, style, icon);
        b.iconOnly = true;
        return b;
    }

    /** Icona a destra dell'etichetta (per esempio le frecce che ciclano la valuta). */
    AtmButton trailing(String icon) {
        this.trailing = icon;
        return this;
    }

    /** Vero se il pulsante non mostra testo e il nome va dato come suggerimento. */
    boolean wantsTooltip() {
        return iconOnly;
    }

    boolean hovered() {
        return this.isHoveredOrFocused();
    }

    private int lineColor() {
        return switch (style) {
            case CONFIRM -> AtmStyle.GOLD;
            case DANGER -> AtmStyle.RED;
            default -> AtmStyle.BLUE;
        };
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float target = this.active && isHoveredOrFocused() ? 1f : 0f;
        glow += (target - glow) * Math.min(1f, (now - glowAt) / 100f);
        glowAt = now;
        int base = lineColor();
        int line;
        int tint;
        if (!this.active) {
            line = AtmStyle.alpha(base, 0x40);
            tint = 0;
        } else if (pressed && isHovered()) {
            line = base;
            tint = AtmStyle.alpha(base, 0x44);
        } else {
            line = AtmStyle.alpha(base, 0x90 + (int) (glow * 0x6F));
            tint = AtmStyle.alpha(base, 0x12 + (int) (glow * 0x1E));
        }
        AtmStyle.box(g, getX(), getY(), getWidth(), getHeight(), line, tint);

        int size = Math.min(16, getHeight() - 4);
        if (iconOnly) {
            drawIcon(g, getX() + (getWidth() - size) / 2, getY() + (getHeight() - size) / 2, size);
            return;
        }
        var font = Minecraft.getInstance().font;
        int color = AtmStyle.color(this.active ? "qrm_bank.cream" : "qrm_bank.disabled");
        if (!this.active) color = AtmStyle.alpha(color, 0x77);
        int textW = AtmStyle.width(font, getMessage().getString());
        int iconW = icon == null ? 0 : size + 2;
        int trailW = trailing == null ? 0 : size + 2;
        int startX = getX() + (getWidth() - textW - iconW - trailW) / 2;
        if (icon != null) drawIcon(g, startX, getY() + (getHeight() - size) / 2, size);
        if (trailing != null) {
            ThemeRenderer.draw(g, trailing, getX() + getWidth() - size - 3, getY() + (getHeight() - size) / 2, size, size);
        }
        AtmStyle.text(g, font, getMessage().getString(), startX + iconW, getY() + (getHeight() - AtmStyle.TEXT_H) / 2 + 1, color);
    }

    private void drawIcon(GuiGraphicsExtractor g, int x, int y, int size) {
        if (icon != null) ThemeRenderer.draw(g, icon, x, y, size, size);
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
