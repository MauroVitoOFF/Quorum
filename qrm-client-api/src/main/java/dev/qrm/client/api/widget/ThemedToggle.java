package dev.qrm.client.api.widget;

import dev.qrm.client.api.theme.ThemeRenderer;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Casella a scelta singola (es. M / F) disegnata con le texture del tema. Lo stato appartiene al chiamante. */
public final class ThemedToggle extends AbstractWidget {
    private final BooleanSupplier selected;
    private final Runnable onPress;
    private final String normalKey;
    private final String selectedKey;

    public ThemedToggle(int x, int y, int width, int height, Component label, BooleanSupplier selected,
            Runnable onPress) {
        this(x, y, width, height, label, selected, onPress, "character.toggle", "character.toggle.selected");
    }

    public ThemedToggle(int x, int y, int width, int height, Component label, BooleanSupplier selected,
            Runnable onPress, String normalKey, String selectedKey) {
        super(x, y, width, height, label);
        this.selected = selected;
        this.onPress = onPress;
        this.normalKey = normalKey;
        this.selectedKey = selectedKey;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean on = selected.getAsBoolean();
        ThemeRenderer.draw(graphics, on ? selectedKey : normalKey, getX(), getY(),
                getWidth(), getHeight());
        var font = Minecraft.getInstance().font;
        int color = ThemeRenderer.color(on || this.isHoveredOrFocused() ? "accent" : "muted");
        graphics.centeredText(font, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - font.lineHeight) / 2,
                color);
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
