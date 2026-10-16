package dev.qrm.bank.client;

import dev.qrm.client.api.theme.ThemeRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Casella di testo dell'ATM: carta da registro con inchiostro, bordo d'ottone quando ha il focus. */
final class AtmField extends EditBox {
    private static final int TEXT_INSET = 5;
    private static final int ICON_INSET = 26;

    private final int frameX;
    private final int frameY;
    private final int frameW;
    private final int frameH;
    private final String icon;
    private final Component label;

    /** Campo con un'icona a sinistra al posto di un'etichetta; {@code label} è il suggerimento al passaggio. */
    AtmField(Font font, int x, int y, int width, int height, Component label, String icon) {
        super(font, x + (icon == null ? TEXT_INSET : ICON_INSET), y + (height - font.lineHeight) / 2,
                Math.max(1, width - (icon == null ? TEXT_INSET : ICON_INSET) - TEXT_INSET), font.lineHeight, label);
        this.icon = icon;
        this.label = label;
        this.frameX = x;
        this.frameY = y;
        this.frameW = width;
        this.frameH = height;
        setBordered(false);
        setTextShadow(false);
        setTextColor(AtmStyle.color("qrm_bank.ink"));
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        AtmStyle.box(graphics, frameX, frameY, frameW, frameH, isFocused() ? AtmStyle.GOLD : AtmStyle.LINE,
                AtmStyle.FIELD_BG);
        if (icon != null) ThemeRenderer.draw(graphics, icon, frameX + 5, frameY + (frameH - 16) / 2, 16, 16);
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
    }

    /** Nome del campo, mostrato come suggerimento quando è vuoto e il mouse è sopra. */
    Component label() {
        return label;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= frameX && mouseX < frameX + frameW && mouseY >= frameY && mouseY < frameY + frameH;
    }
}
