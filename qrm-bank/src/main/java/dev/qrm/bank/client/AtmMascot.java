package dev.qrm.bank.client;

import dev.qrm.client.api.theme.QrmTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Il cassiere a blocchi: uno spritesheet verticale di 8 frame da 32x40 (lancia una moneta e la riprende, pollice in
 * su con il sorriso sull'ultimo frame). La schermata sceglie il frame dal tempo; non serve un {@code .mcmeta}.
 */
final class AtmMascot {
    static final int WIDTH = 32;
    static final int HEIGHT = 40;
    static final int FRAMES = 8;
    static final long FRAME_MS = 140;

    private AtmMascot() {}

    /** Frame da mostrare all'istante dato. */
    static int frameAt(long millis) {
        return (int) ((millis / FRAME_MS) % FRAMES);
    }

    /** Frame del sorriso: la mascotte lo tiene fisso quando un'operazione è riuscita. */
    static final int HAPPY_FRAME = FRAMES - 1;

    /** Disegna il frame corrente (o quello felice) con l'angolo in alto a sinistra in (x, y), senza scalarlo. */
    static void draw(GuiGraphicsExtractor g, int x, int y, boolean happy) {
        var texture = QrmTheme.current().texture("qrm_bank.ui.mascot");
        g.blit(RenderPipelines.GUI_TEXTURED, Identifier.parse(texture.id()), x, y, 0F,
                (float) ((happy ? HAPPY_FRAME : frameAt(System.currentTimeMillis())) * HEIGHT), WIDTH, HEIGHT, WIDTH, HEIGHT, texture.width(),
                texture.height());
    }
}
