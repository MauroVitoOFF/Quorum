package dev.qrm.vehicles.client;

import dev.qrm.vehicles.logic.VehicleParts;
import dev.qrm.vehicles.mc.QrmVehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Il mirino del gioco, al posto di quello vanilla (una croce spessa che copre la scena): un puntino bianco appena
 * visibile. Quando si guarda una parte del veicolo che si può usare (portiera, chiave, cambio...) intorno al punto si
 * apre un piccolo quadratino, e basta: il mirino "risponde" a ciò che si ha davanti senza scrivere niente.
 * Solo in prima persona, come il vanilla; chi lo cancella prima (il mirino dell'arma) ha la precedenza.
 */
public final class VehicleCrosshair {
    private VehicleCrosshair() {}

    /** Bianco al 45% circa: come il punto del mirino dell'arma, si vede senza dare fastidio. */
    private static final int DOT = 0x73FFFFFF;
    private static final int RING_MAX_ALPHA = 0xA0;
    /** Distanza degli angoli dal punto centrale e lunghezza dei bracci, in pixel. */
    private static final int CORNER = 3;
    private static final int ARM = 2;
    /** Frazione della strada percorsa a ogni disegno verso l'apertura o la chiusura del quadratino. */
    private static final float EASE = 0.3f;
    private static float open;

    /** Da registrare sul bus di NeoForge, lato client, con priorità bassa. */
    public static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
        if (!VanillaGuiLayers.CROSSHAIR.equals(event.getName())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
        event.setCanceled(true);

        open += ((usable() ? 1f : 0f) - open) * EASE;
        GuiGraphicsExtractor g = event.getGuiGraphics();
        int cx = g.guiWidth() / 2;
        int cy = g.guiHeight() / 2;
        g.fill(cx, cy, cx + 1, cy + 1, DOT);
        int alpha = Math.round(open * RING_MAX_ALPHA);
        if (alpha < 8) return;
        // Solo i quattro angoli di un quadrato di 7x7 pixel centrato sul punto, ognuno con due pixel per lato.
        int color = (alpha << 24) | 0xFFFFFF;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                // Pixel d'angolo a (cx + sx*3, cy + sy*3); i bracci vanno verso il centro.
                int x = cx + sx * CORNER;
                int y = cy + sy * CORNER;
                int xa = Math.min(x, x - sx * (ARM - 1));
                int xb = Math.max(x, x - sx * (ARM - 1));
                int ya = Math.min(y, y - sy * (ARM - 1));
                int yb = Math.max(y, y - sy * (ARM - 1));
                g.fill(xa, y, xb + 1, y + 1, color);
                g.fill(x, ya, x + 1, yb + 1, color);
            }
        }
    }

    /** Si sta guardando una parte del veicolo che il clic destro usa? */
    private static boolean usable() {
        QrmVehicleEntity v = VehicleTargeting.targetVehicle();
        String zone = VehicleTargeting.targetZone();
        if (v == null || zone == null) return false;
        return zone.startsWith("hit_seat") || zone.equals("hit_ignition_key") || zone.equals("hit_gear_shifter")
                || zone.equals("hit_light_switch") || VehicleParts.bitOfZone(zone) != 0;
    }
}
