package dev.qrm.police.weapon.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Pannello di messa a punto della posa della pistola ({@code /pistolpose}): un cursore per parametro in una colonna a
 * sinistra, mentre l'arma resta visibile a destra e cambia mentre si trascina. Rotella del mouse sul cursore =
 * ritocco fine. "Copia" mette negli appunti la riga di valori da incollare a Claude.
 */
public final class WeaponPoseScreen extends Screen {
    private static final int W = 150;
    private static final int H = 16;
    private static boolean openRequested;

    private WeaponPoseScreen() {
        super(Component.literal("Posa pistola"));
    }

    /** Chiede l'apertura al prossimo tick: la chat che ha lanciato il comando si chiude subito dopo. */
    static void requestOpen() {
        openRequested = true;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!openRequested) return;
        openRequested = false;
        Minecraft.getInstance().setScreenAndShow(new WeaponPoseScreen());
    }

    @Override
    protected void init() {
        int y = 6;
        for (String name : WeaponPose.NAMES) {
            addRenderableWidget(new PoseSlider(6, y, name));
            y += H + 2;
        }
        y += 4;
        addRenderableWidget(Button.builder(Component.literal("Copia"), b -> copy()).bounds(6, y, 72, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            WeaponPose.reset();
            rebuildWidgets();
        }).bounds(6 + 78, y, 72, 20).build());
    }

    private static void copy() {
        Minecraft.getInstance().keyboardHandler.setClipboard(WeaponPose.summary());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Nessuno sfondo: l'arma deve restare ben visibile. */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // intenzionalmente vuoto
    }

    private static float[] range(String name) {
        return switch (name) {
            case "x", "y", "z" -> new float[] {-24f, 24f, 0.1f};
            case "scale" -> new float[] {0.3f, 1.5f, 0.01f};
            case "armx", "army", "armz" -> new float[] {-16f, 16f, 0.1f};
            default -> new float[] {-180f, 180f, 1f}; // rotazioni
        };
    }

    private static final class PoseSlider extends AbstractSliderButton {
        private final String name;
        private final float min;
        private final float max;
        private final float step;

        PoseSlider(int x, int y, String name) {
            super(x, y, W, H, Component.empty(), 0);
            this.name = name;
            float[] r = range(name);
            this.min = r[0];
            this.max = r[1];
            this.step = r[2];
            this.value = (WeaponPose.get(name) - min) / (max - min);
            updateMessage();
        }

        private float real() {
            return WeaponPose.get(name);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(String.format(Locale.ROOT, "%s: %.2f", name, real())));
        }

        @Override
        protected void applyValue() {
            float v = min + (float) value * (max - min);
            WeaponPose.set(name, Math.round(v / step) * step);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
            if (!isMouseOver(mx, my)) return false;
            float v = Math.max(min, Math.min(max, real() + (scrollY > 0 ? step : -step)));
            WeaponPose.set(name, v);
            this.value = (v - min) / (max - min);
            updateMessage();
            return true;
        }
    }
}
