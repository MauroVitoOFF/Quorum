package dev.qrm.bank.client;

import dev.qrm.client.api.theme.ThemeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Pulsante o tasto-linguetta dell'ATM, disegnati con le texture del modulo (chiavi {@code qrm_bank.ui.*}). */
final class AtmButton extends AbstractWidget {
    enum Style {
        DEFAULT("button"), CONFIRM("button_confirm"), DANGER("button_danger");

        private final String prefix;
        Style(String prefix) {
            this.prefix = prefix;
        }
    }

    static final int TAB_W = 28;
    static final int TAB_H = 20;

    private final Runnable onPress;
    private final Style style;
    private final String icon;
    private final boolean tab;
    private boolean iconOnly;
    private String graphic;
    private String trailing;
    private boolean selected;
    private boolean pressed;

    private AtmButton(int x, int y, int w, int h, Component label, Runnable onPress, Style style, String icon,
            boolean tab) {
        super(x, y, w, h, label);
        this.onPress = onPress;
        this.style = style;
        this.icon = icon;
        this.tab = tab;
    }

    static AtmButton button(int x, int y, int w, int h, Component label, String icon, Style style, Runnable onPress) {
        return new AtmButton(x, y, w, h, label, onPress, style, icon, false);
    }

    /** Tasto laterale 28x20 con la sola icona; il nome compare come suggerimento. */
    static AtmButton tab(int x, int y, Component label, String icon, boolean selected, Runnable onPress) {
        AtmButton b = new AtmButton(x, y, TAB_W, TAB_H, label, onPress, Style.DEFAULT, icon, true);
        b.selected = selected;
        return b;
    }

    /** Pulsante con la sola icona; il nome compare come suggerimento. */
    static AtmButton icon(int x, int y, int w, int h, Component label, String icon, Style style, Runnable onPress) {
        AtmButton b = new AtmButton(x, y, w, h, label, onPress, style, icon, false);
        b.iconOnly = true;
        return b;
    }

    /**
     * Pulsante la cui scritta è già nella texture ({@code qrm_bank.ui.<prefix>_normal/hover/pressed/disabled}), alla
     * dimensione esatta della texture.
     */
    static AtmButton graphic(int x, int y, int w, int h, Component label, String prefix, Runnable onPress) {
        AtmButton b = new AtmButton(x, y, w, h, label, onPress, Style.DEFAULT, null, false);
        b.graphic = prefix;
        return b;
    }

    /** Icona a destra dell'etichetta (per esempio le frecce che ciclano la valuta). */
    AtmButton trailing(String icon) {
        this.trailing = icon;
        return this;
    }

    boolean isTab() {
        return tab;
    }

    /** Vero se il pulsante non mostra testo e il nome va dato come suggerimento. */
    boolean wantsTooltip() {
        return tab || iconOnly;
    }

    boolean hovered() {
        return this.isHoveredOrFocused();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (tab) {
            String key = selected ? "qrm_bank.ui.tab_selected"
                    : isHoveredOrFocused() ? "qrm_bank.ui.tab_hover" : "qrm_bank.ui.tab";
            ThemeRenderer.draw(g, key, getX(), getY(), getWidth(), getHeight());
            drawIcon(g, getX() + (getWidth() - 16) / 2, getY() + (getHeight() - 16) / 2);
            return;
        }
        if (graphic != null) {
            String state = !this.active ? "disabled" : pressed && isHovered() ? "pressed"
                    : isHoveredOrFocused() ? "hover" : "normal";
            ThemeRenderer.draw(g, "qrm_bank.ui." + graphic + "_" + state, getX(), getY(), getWidth(), getHeight());
            return;
        }
        String key;
        if (!this.active) key = "qrm_bank.ui.button_disabled";
        else if (pressed && isHovered()) key = "qrm_bank.ui." + style.prefix + "_pressed";
        else if (isHoveredOrFocused()) key = "qrm_bank.ui." + style.prefix + "_hover";
        else key = "qrm_bank.ui." + style.prefix + "_normal";
        ThemeRenderer.draw(g, key, getX(), getY(), getWidth(), getHeight());
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
