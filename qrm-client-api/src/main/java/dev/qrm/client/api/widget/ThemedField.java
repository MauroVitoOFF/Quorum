package dev.qrm.client.api.widget;

import dev.qrm.client.api.theme.ThemeRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Casella di testo con lo sfondo del tema: un {@link EditBox} senza bordo vanilla dentro un riquadro
 * ({@code character.field} / {@code character.field.focus}); l'intero riquadro è cliccabile.
 */
public final class ThemedField extends EditBox {
    private static final int TEXT_INSET = 5;

    private final int frameX;
    private final int frameY;
    private final int frameW;
    private final int frameH;

    public ThemedField(Font font, int x, int y, int width, int height, Component narration) {
        super(font, x + TEXT_INSET, y + (height - font.lineHeight) / 2, Math.max(1, width - 2 * TEXT_INSET),
                font.lineHeight, narration);
        this.frameX = x;
        this.frameY = y;
        this.frameW = width;
        this.frameH = height;
        setBordered(false);
        setTextColor(ThemeRenderer.color("text"));
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ThemeRenderer.draw(graphics, isFocused() ? "character.field.focus" : "character.field", frameX, frameY, frameW,
                frameH);
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= frameX && mouseX < frameX + frameW && mouseY >= frameY && mouseY < frameY + frameH;
    }
}
