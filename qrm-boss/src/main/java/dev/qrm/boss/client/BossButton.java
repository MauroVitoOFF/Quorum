package dev.qrm.boss.client;

import dev.qrm.client.api.theme.ThemeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Pulsante o linguetta del menu boss, disegnati con le texture del modulo (chiavi {@code qrm_boss.*}). */
final class BossButton extends AbstractWidget {
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
    private final boolean tab;
    private boolean selected;
    private boolean pressed;

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

    /** Linguetta 28x20 con la sola icona; il nome compare come suggerimento. */
    static BossButton tab(int x, int y, Component label, String icon, boolean selected, Runnable onPress) {
        BossButton b = new BossButton(x, y, 28, 20, label, onPress, Style.DEFAULT, icon, true);
        b.selected = selected;
        return b;
    }

    /** Vero per un pulsante a interruttore acceso: usa lo stile di conferma. */
    BossButton selected(boolean value) {
        this.selected = value;
        return this;
    }

    boolean hovered() {
        return this.isHoveredOrFocused();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (tab) {
            String key = selected ? "qrm_boss.tab_selected" : isHoveredOrFocused() ? "qrm_boss.tab_hover" : "qrm_boss.tab";
            ThemeRenderer.draw(g, key, getX(), getY(), getWidth(), getHeight());
            drawIcon(g, getX() + (getWidth() - 16) / 2, getY() + (getHeight() - 16) / 2);
            return;
        }
        Style effective = selected && style == Style.DEFAULT ? Style.CONFIRM : style;
        String key;
        if (!this.active) key = "qrm_boss.button_disabled";
        else if (pressed && isHovered()) key = "qrm_boss." + effective.prefix + "_pressed";
        else if (isHoveredOrFocused()) key = "qrm_boss." + effective.prefix + "_hover";
        else key = "qrm_boss." + effective.prefix + "_normal";
        ThemeRenderer.draw(g, key, getX(), getY(), getWidth(), getHeight());

        var font = Minecraft.getInstance().font;
        int color = ThemeRenderer.color(this.active ? "qrm_boss.cream" : "qrm_boss.disabled");
        int textW = font.width(getMessage());
        int iconW = icon == null ? 0 : 18;
        int startX = getX() + (getWidth() - textW - iconW) / 2;
        if (icon != null) drawIcon(g, startX, getY() + (getHeight() - 16) / 2);
        g.text(font, getMessage(), startX + iconW, getY() + (getHeight() - font.lineHeight) / 2, color);
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
