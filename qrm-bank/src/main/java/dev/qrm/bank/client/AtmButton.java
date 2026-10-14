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

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        String key;
        if (!this.active) key = "qrm_bank.ui.button_disabled";
        else if (pressed && isHovered()) key = "qrm_bank.ui." + style.prefix + "_pressed";
        else if (isHoveredOrFocused()) key = "qrm_bank.ui." + style.prefix + "_hover";
        else key = "qrm_bank.ui." + style.prefix + "_normal";
        ThemeRenderer.draw(g, key, getX(), getY(), getWidth(), getHeight());
        drawGlow(g);
        if (iconOnly) {
            drawIcon(g, getX() + (getWidth() - 16) / 2, getY() + (getHeight() - 16) / 2);
            return;
        }

        var font = Minecraft.getInstance().font;
        // Testo crema su smalto scuro, senza ombra: resta nitido a ogni scala.
        int color = ThemeRenderer.color(this.active ? "qrm_bank.cream" : "qrm_bank.disabled");
        int textW = font.width(getMessage());
        int iconW = icon == null ? 0 : 18;
        int trailW = trailing == null ? 0 : 18;
        int startX = getX() + (getWidth() - textW - iconW - trailW) / 2;
        if (icon != null) drawIcon(g, startX, getY() + (getHeight() - 16) / 2);
        if (trailing != null) ThemeRenderer.draw(g, trailing, getX() + getWidth() - 20, getY() + (getHeight() - 16) / 2, 16, 16);
        g.text(font, getMessage().getString(), startX + iconW, getY() + (getHeight() - font.lineHeight) / 2, color, false);
    }

    /** Bagliore che entra e esce in circa un decimo di secondo al passaggio del mouse. */
    private void drawGlow(GuiGraphicsExtractor g) {
        long now = System.currentTimeMillis();
        float target = this.active && isHoveredOrFocused() ? 1f : 0f;
        float step = Math.min(1f, (now - glowAt) / 100f);
        glow += (target - glow) * step;
        glowAt = now;
        int alpha = (int) (glow * 0x30);
        if (alpha < 2) return;
        int rgb = style == Style.CONFIRM ? 0xFFE9A8 : style == Style.DANGER ? 0xFFB0B0 : 0xFFFFFF;
        g.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, (alpha << 24) | rgb);
    }

    private void drawIcon(GuiGraphicsExtractor g, int x, int y) {
        if (icon != null) ThemeRenderer.draw(g, icon, x, y, 16, 16);
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
