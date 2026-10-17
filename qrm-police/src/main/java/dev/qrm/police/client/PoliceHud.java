package dev.qrm.police.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/** Bussola della chiamata presa in carico: una freccia, la distanza e il motivo. */
final class PoliceHud {
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
    private static final double ARRIVED = 8.0;

    private PoliceHud() {}

    static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        ClientPolice.Waypoint w = ClientPolice.waypoint();
        Minecraft mc = Minecraft.getInstance();
        if (w == null || mc.player == null) return;
        double dx = w.x() + 0.5 - mc.player.getX();
        double dz = w.z() + 0.5 - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < ARRIVED) {
            ClientPolice.clearWaypoint();
            return;
        }
        float target = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float rel = Mth.wrapDegrees(target - mc.player.getYRot());
        String arrow = ARROWS[Math.floorMod(Math.round(rel / 45f), 8)];
        String line = arrow + "  " + (int) dist + " m  -  " + w.label();
        int width = mc.font.width(line);
        int x = (g.guiWidth() - width) / 2;
        int y = 22;
        g.fill(x - 6, y - 4, x + width + 6, y + 12, 0xB00B1220);
        g.fill(x - 6, y - 4, x - 4, y + 12, 0xFF3B82F6);
        g.text(mc.font, line, x, y, 0xFFE5E7EB, false);
    }
}
