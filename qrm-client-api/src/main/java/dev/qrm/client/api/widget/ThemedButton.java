package dev.qrm.client.api.widget;

import dev.qrm.client.api.theme.ThemeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Pulsante disegnato con le texture del tema. */
public final class ThemedButton extends AbstractWidget {
    /** Stile: prefisso delle texture del tema e colore del testo. Lo stato disabilitato è comune. */
    public enum Style {
        DEFAULT("button", "text"),
        CONFIRM("button.confirm", "accent"),
        DANGER("button.danger", "accent2");

        private final String prefix;
        private final String textColor;

        Style(String prefix, String textColor) {
            this.prefix = prefix;
            this.textColor = textColor;
        }
    }

    private final Runnable onPress;
    private final Style style;
    private boolean pressed;

    public ThemedButton(int x, int y, int width, int height, Component label, Runnable onPress) {
        this(x, y, width, height, label, onPress, Style.DEFAULT);
    }

    public ThemedButton(int x, int y, int width, int height, Component label, Runnable onPress, Style style) {
        super(x, y, width, height, label);
        this.onPress = onPress;
        this.style = style;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        String key;
        if (!this.active) key = "button.disabled";
        else if (this.pressed && this.isHovered()) key = style.prefix + ".pressed";
        else if (this.isHoveredOrFocused()) key = style.prefix + ".hover";
        else key = style.prefix + ".normal";
        ThemeRenderer.draw(graphics, key, getX(), getY(), getWidth(), getHeight());
        var font = Minecraft.getInstance().font;
        int color = ThemeRenderer.color(this.active ? style.textColor : "muted");
        graphics.centeredText(font, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - font.lineHeight) / 2, color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        if (handled) this.pressed = true;
        return handled;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.pressed = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.isActive()) return false;
        if (event.isSelection()) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onPress.run();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
