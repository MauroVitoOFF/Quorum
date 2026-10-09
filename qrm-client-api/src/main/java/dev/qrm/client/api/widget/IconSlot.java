package dev.qrm.client.api.widget;

import dev.qrm.client.api.theme.ThemeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Slot quadrato della barra rapida del menu staff: cornice {@code staff.slot} del tema e un'icona 16x16 (per esempio
 * {@code staff.icon.back}, {@code staff.icon.refresh}, {@code staff.icon.close}). Spento = scurito e non cliccabile.
 */
public final class IconSlot extends AbstractWidget {
    public static final int SIZE = 24;
    private static final int ICON = 16;

    private final String iconKey;
    private final Runnable onPress;

    public IconSlot(int x, int y, Component label, String iconKey, Runnable onPress) {
        super(x, y, SIZE, SIZE, label);
        this.iconKey = iconKey;
        this.onPress = onPress;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        ThemeRenderer.draw(g, "staff.slot", getX(), getY(), SIZE, SIZE);
        ThemeRenderer.draw(g, iconKey, getX() + (SIZE - ICON) / 2, getY() + (SIZE - ICON) / 2, ICON, ICON);
        if (!this.active) {
            g.fill(getX() + 2, getY() + 2, getX() + SIZE - 2, getY() + SIZE - 2, 0xAA1A1208);
        } else if (this.isHoveredOrFocused()) {
            g.fill(getX() + 2, getY() + 2, getX() + SIZE - 2, getY() + SIZE - 2, 0x33FFE59A);
        }
    }

    /** Vero se il mouse è sopra lo slot ed è attivo (per mostrarne l'etichetta come suggerimento). */
    public boolean hovered() {
        return this.active && this.isHovered();
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onPress.run();
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
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
