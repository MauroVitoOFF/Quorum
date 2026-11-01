package dev.qrm.police.weapon.client;

import dev.qrm.police.weapon.logic.WeaponDef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Mirino dell'arma: al posto di quello vanilla (una croce spessa che copre la scena) un discreto "-.-": un punto
 * e due piccoli trattini ai lati, bianchi e semitrasparenti, senza contorno. Si sostituisce solo con la pistola in
 * mano e in prima persona.
 */
public final class WeaponCrosshair {
    private static final int GAP = 4;
    /** Vuoto accovacciati: il mirino si stringe un poco (la dispersione è dimezzata). */
    private static final float CROUCH_GAP = 2f;
    /** Frazione dello spazio percorsa a ogni disegno verso il vuoto di destinazione. */
    private static final float EASE = 0.25f;
    private static float gap = GAP;
    private static final int LENGTH = 3;
    /** Bianco al 45% circa: si vede senza dare fastidio. */
    private static final int COLOR = 0x73FFFFFF;

    private WeaponCrosshair() {}

    /** Da registrare sul bus di NeoForge, lato client. */
    public static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
        if (!VanillaGuiLayers.CROSSHAIR.equals(event.getName())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
        WeaponDef def = HeldWeapon.of(mc.player.getMainHandItem()).orElse(null);
        if (def == null || !def.showCrosshair()) return;
        event.setCanceled(true);

        GuiGraphicsExtractor g = event.getGuiGraphics();
        gap += ((mc.player.isCrouching() ? CROUCH_GAP : GAP) - gap) * EASE;
        int space = Math.round(gap);
        int cx = g.guiWidth() / 2;
        int cy = g.guiHeight() / 2;
        // punto centrale e trattini a sinistra e a destra: -.-
        box(g, cx, cy, cx + 1, cy + 1);
        box(g, cx - space - LENGTH, cy, cx - space, cy + 1);
        box(g, cx + 1 + space, cy, cx + 1 + space + LENGTH, cy + 1);
    }

    private static void box(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
        g.fill(x0, y0, x1, y1, COLOR);
    }
}
