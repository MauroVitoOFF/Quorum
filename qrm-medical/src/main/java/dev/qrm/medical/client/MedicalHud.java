package dev.qrm.medical.client;

import dev.qrm.medical.logic.MedicalRules;
import dev.qrm.medical.logic.MedicalState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Vignettatura pulsante, testo del tempo rimasto e comandi, per chi è a terra o svenuto. */
final class MedicalHud {
    private static final int BANDS = 28;
    private static final int BAND_PX = 4;

    private MedicalHud() {}

    static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!ClientMedical.incapacitated()) return;
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        MedicalState state = ClientMedical.state();

        if (state == MedicalState.UNCONSCIOUS) {
            g.fill(0, 0, w, h, 0xF2000000); // 95% nero
        } else {
            vignette(g, w, h);
        }
        text(g, mc, w, h, state);
    }

    /** Rosso ai bordi, che pulsa e diventa più intenso quando il tempo scende. */
    private static void vignette(GuiGraphicsExtractor g, int w, int h) {
        double left = 1.0 - (double) ClientMedical.bleedOutTicks() / MedicalRules.BLEED_OUT_TICKS;
        double pulse = 0.65 + 0.35 * Math.sin(System.currentTimeMillis() / (420.0 - 280.0 * left));
        double strength = (0.35 + 0.55 * left) * pulse;
        for (int i = 0; i < BANDS; i++) {
            double fall = 1.0 - (double) i / BANDS;
            int alpha = (int) Math.round(255 * strength * fall * fall);
            if (alpha <= 0) continue;
            int color = (alpha << 24) | 0x8B0000;
            int o = i * BAND_PX;
            g.fill(o, o, w - o, o + BAND_PX, color);
            g.fill(o, h - o - BAND_PX, w - o, h - o, color);
            g.fill(o, o + BAND_PX, o + BAND_PX, h - o - BAND_PX, color);
            g.fill(w - o - BAND_PX, o + BAND_PX, w - o, h - o - BAND_PX, color);
        }
    }

    private static void text(GuiGraphicsExtractor g, Minecraft mc, int w, int h, MedicalState state) {
        Font font = mc.font;
        int seconds = ClientMedical.bleedOutTicks() / 20;
        int y = h - 78;
        centered(g, font, w, y, Component.translatable("qrm_medical.hud.hurt", seconds / 60,
                String.format("%02d", seconds % 60)), 0xFFFF5555);
        y += 12;
        if (ClientMedical.rcp()) {
            centered(g, font, w, y, Component.translatable("qrm_medical.hud.rcp"), 0xFF8EE08E);
            y += 12;
        }
        centered(g, font, w, y, Component.translatable("qrm_medical.hud.alert",
                MedicalKeys.ALERT.getTranslatedKeyMessage()), 0xFFFFFFFF);
        y += 12;
        if (ClientMedical.canSurrender()) {
            int pct = Math.min(100, MedicalClient.surrenderHold() * 100 / MedicalRules.SURRENDER_HOLD_TICKS);
            centered(g, font, w, y, Component.translatable("qrm_medical.hud.surrender",
                    MedicalKeys.SURRENDER.getTranslatedKeyMessage(), pct), 0xFFFFFFFF);
        } else {
            int wait = Math.max(0, (MedicalRules.SURRENDER_AFTER_TICKS - ClientMedical.downedTicks()) / 20);
            centered(g, font, w, y, Component.translatable("qrm_medical.hud.surrender_wait", wait), 0xFFAAAAAA);
        }
    }

    private static void centered(GuiGraphicsExtractor g, Font font, int w, int y, Component c, int color) {
        String s = c.getString();
        g.text(font, s, (w - font.width(s)) / 2, y, color);
    }
}
