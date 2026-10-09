package dev.qrm.client.api.screen;

import dev.qrm.client.api.theme.ThemeRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Schermata base di QRM: mondo di gioco a vetro appannato (blur nativo, se non disattivato nelle opzioni, più i colori
 * {@code worldDim} e {@code worldFrost} del tema); non mette in pausa il gioco.
 */
public abstract class QrmScreen extends Screen {
    protected QrmScreen(Component title) {
        super(title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        extractBlurredBackground(g);
        g.fill(0, 0, this.width, this.height, ThemeRenderer.color("worldDim"));
        g.fill(0, 0, this.width, this.height, ThemeRenderer.color("worldFrost"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
