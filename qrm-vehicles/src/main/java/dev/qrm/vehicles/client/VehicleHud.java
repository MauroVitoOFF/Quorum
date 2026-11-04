package dev.qrm.vehicles.client;

import dev.qrm.vehicles.mc.QrmVehicleEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Il cruscotto: velocità, carburante, chilometri e stato del motore, per chi è a bordo. */
final class VehicleHud {
    private static final int WIDTH = 112;
    private static final int HEIGHT = 46;
    private static final double TICKS_TO_KMH = 20 * 3.6;

    private VehicleHud() {}

    static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !(player.getVehicle() instanceof QrmVehicleEntity v)) return;

        int x = g.guiWidth() - WIDTH - 8;
        int y = g.guiHeight() - HEIGHT - 8;
        g.fill(x, y, x + WIDTH, y + HEIGHT, 0xB00B1220);
        g.fill(x, y, x + 2, y + HEIGHT, v.engineOn() ? 0xFF22C55E : 0xFF6B7280);

        int kmh = (int) Math.round(Math.abs(v.speed()) * TICKS_TO_KMH);
        g.text(mc.font, Component.translatable("qrm_vehicles.hud.speed", kmh), x + 8, y + 5, 0xFFE5E7EB, false);
        g.text(mc.font, Component.translatable(v.engineOn() ? "qrm_vehicles.hud.engine_on"
                : "qrm_vehicles.hud.engine_off"), x + 62, y + 5, v.engineOn() ? 0xFF86EFAC : 0xFF9CA3AF, false);

        double fraction = v.def().fuelCapacity() > 0 ? Math.max(0, Math.min(1, v.fuelLevel() / v.def().fuelCapacity())) : 0;
        int barX = x + 8;
        int barY = y + 19;
        int barW = WIDTH - 16;
        g.fill(barX, barY, barX + barW, barY + 5, 0xFF1F2937);
        g.fill(barX, barY, barX + (int) Math.round(barW * fraction), barY + 5, fraction < 0.15 ? 0xFFEF4444 : 0xFFF59E0B);
        g.text(mc.font, Component.translatable("qrm_vehicles.hud.fuel", String.format("%.1f", v.fuelLevel())),
                x + 8, y + 28, 0xFFE5E7EB, false);
        g.text(mc.font, Component.translatable("qrm_vehicles.hud.odometer", String.format("%.1f", v.odometerKm())),
                x + 58, y + 28, 0xFF9CA3AF, false);
    }
}
