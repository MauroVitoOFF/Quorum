package dev.qrm.medical.client;

import dev.qrm.client.api.body.BodyFigure;
import dev.qrm.client.api.body.BodySkin;
import dev.qrm.client.api.body.BodyStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.HumanoidArm;

/**
 * La piccola sagoma del corpo, a sinistra della hotbar e alta quanto lei: compare solo se attivata nella
 * configurazione e solo quando c'è una ferita.
 */
final class BodyHud {
    /** 11x22 pixel: la hotbar è alta 22. */
    static final BodySkin SKIN = new BodySkin("qrm_medical", "body", 11, 22);
    private static final int HOTBAR_HALF_WIDTH = 91;
    private static final int OFFHAND_SLOT = 29;
    private static final int GAP = 4;

    private BodyHud() {}

    static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!MedicalClientConfig.BODY_ENABLED.get()) return;
        BodyStatus status = ClientBody.status();
        if (!status.anyInjured()) return;
        Minecraft mc = Minecraft.getInstance();
        int zoom = MedicalClientConfig.BODY_ZOOM.get();
        int w = BodyFigure.width(SKIN, zoom);
        int h = BodyFigure.height(SKIN, zoom);
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int x = sw / 2 - HOTBAR_HALF_WIDTH - GAP - w;
        // con un oggetto nella mano secondaria, lo slot compare a sinistra della hotbar se si è destrorsi
        if (mc.player != null && !mc.player.getOffhandItem().isEmpty()
                && mc.options.mainHand().get() == HumanoidArm.RIGHT) {
            x -= OFFHAND_SLOT;
        }
        BodyFigure.render(g, SKIN, status, x, sh - h, zoom);
    }
}
