package dev.qrm.boss.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Casella di testo del menu boss, come quella della banca: fondo scuro e contorno, oro quando ha il focus. */
final class BossField extends EditBox {
    private static final int INSET = 5;

    private final int fx, fy, fw, fh;

    BossField(Font font, int x, int y, int width, int height, Component label) {
        super(font, x + INSET, y + (height - font.lineHeight) / 2, Math.max(1, width - 2 * INSET), font.lineHeight, label);
        this.fx = x;
        this.fy = y;
        this.fw = width;
        this.fh = height;
        setBordered(false);
        setTextShadow(false);
        setTextColor(BossStyle.color("qrm_boss.ink"));
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        BossStyle.box(g, fx, fy, fw, fh, isFocused() ? BossStyle.GOLD : BossStyle.LINE, BossStyle.FIELD_BG);
        super.extractWidgetRenderState(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= fx && mouseX < fx + fw && mouseY >= fy && mouseY < fy + fh;
    }
}
