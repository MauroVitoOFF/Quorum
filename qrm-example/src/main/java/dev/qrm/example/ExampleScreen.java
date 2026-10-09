package dev.qrm.example;

import dev.qrm.client.api.screen.QrmScreen;
import dev.qrm.client.api.theme.ThemeRenderer;
import dev.qrm.client.api.widget.ThemedButton;
import dev.qrm.client.api.widget.ThemedField;
import dev.qrm.client.api.widget.ThemedToggle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Schermata dimostrativa: usa solo l'API client di QRM (sfondo, pannello, campo, casella e pulsante a tema). */
final class ExampleScreen extends QrmScreen {
    private static final int PANEL_W = 200;
    private static final int PANEL_H = 110;

    private boolean option;

    ExampleScreen() {
        super(Component.translatable("qrm_example.demo.title"));
    }

    @Override
    protected void init() {
        int x = (this.width - PANEL_W) / 2;
        int y = (this.height - PANEL_H) / 2;
        addRenderableWidget(new ThemedField(this.font, x + 12, y + 28, PANEL_W - 24, 18,
                Component.translatable("qrm_example.demo.field")));
        addRenderableWidget(new ThemedToggle(x + 12, y + 54, 90, 16, Component.translatable("qrm_example.demo.toggle"),
                () -> option, () -> option = !option, "hub.tab", "hub.tab.selected"));
        addRenderableWidget(new ThemedButton(x + 12, y + PANEL_H - 32, PANEL_W - 24, 20,
                Component.translatable("qrm_example.demo.close"), this::onClose, ThemedButton.Style.DANGER));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = (this.width - PANEL_W) / 2;
        int y = (this.height - PANEL_H) / 2;
        ThemeRenderer.draw(g, "character.idcard", x, y, PANEL_W, PANEL_H);
        g.centeredText(this.font, this.title, this.width / 2, y + 10, ThemeRenderer.color("qrm_example.accent"));
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }
}
