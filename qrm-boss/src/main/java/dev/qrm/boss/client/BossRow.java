package dev.qrm.boss.client;

import dev.qrm.client.api.theme.ThemeRenderer;
import dev.qrm.client.api.widget.TextFit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Riga di un elenco del menu boss: testo a sinistra, nota opzionale a destra, stato selezionato e hover. */
final class BossRow extends AbstractWidget {
    private final String left;
    private final String right;
    private final boolean selected;
    private final Runnable onPress;

    BossRow(int x, int y, int w, int h, String left, String right, boolean selected, Runnable onPress) {
        super(x, y, w, h, Component.literal(left));
        this.left = left;
        this.right = right;
        this.selected = selected;
        this.onPress = onPress;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        String key = selected ? "qrm_boss.row_selected" : isHoveredOrFocused() ? "qrm_boss.row_hover" : "qrm_boss.row";
        ThemeRenderer.draw(g, key, getX(), getY(), getWidth(), getHeight());
        var font = Minecraft.getInstance().font;
        int y = getY() + (getHeight() - font.lineHeight) / 2;
        int rightW = right.isEmpty() ? 0 : Math.min(font.width(right), getWidth() / 2);
        String r = TextFit.fit(font, right, rightW);
        g.text(font, TextFit.fit(font, left, getWidth() - rightW - 16), getX() + 6, y, ThemeRenderer.color("qrm_boss.ink"), false);
        if (!right.isEmpty()) {
            g.text(font, r, getX() + getWidth() - 6 - font.width(r), y, ThemeRenderer.color("qrm_boss.ink_soft"), false);
        }
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
