package dev.qrm.immersion.client;

import dev.qrm.immersion.logic.ImmersionRules;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Il nome di chi conosci come un pensiero: piccolo, in corsivo, sotto il mirino, solo se lo guardi un momento, e
 * svanisce piano. Gli sconosciuti non hanno etichetta. Compare solo entro 5 blocchi, guardando la persona sul corpo
 * e con la linea di vista libera.
 */
final class LabelHud {
    private static final int BELOW_CROSSHAIR = 12;
    /** La meta' della dimensione normale del testo. */
    private static final float SCALE = 0.5F;
    /** Il pensiero arriva dopo uno sguardo che si ferma, sale piano e se ne va piano. */
    private static final long FADE_DELAY_MS = 450;
    private static final long FADE_IN_MS = 500;
    private static final long FADE_OUT_MS = 700;
    private static final int MAX_ALPHA = 0xB0;
    /** Quanto si allarga il corpo per capire se lo si guarda, in blocchi. */
    private static final double AIM_MARGIN = 0.25;
    private static final double FOCUSED_MARGIN = 0.5;

    private static UUID gazeTarget;
    private static long gazeSince;
    /** Cio' che si sta dissolvendo: l'ultimo nome mostrato e quando si e' smesso di guardarlo. */
    private static String shown;
    private static long shownLostAt;

    private LabelHud() {}

    static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (me == null || mc.level == null || mc.gui.screen() != null) return;
        long now = System.currentTimeMillis();

        String known = null;
        Avatar target = lookedAt(mc, me);
        if (target != null) {
            // Il nome lo decide il server: dall'UUID del giocatore non si deve poter dedurre niente.
            known = ClientImmersion.knownName(target.getUUID());
            if (!target.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) known = null;
        }

        double strength;
        if (known != null) {
            if (!target.getUUID().equals(gazeTarget)) {
                gazeTarget = target.getUUID();
                gazeSince = now;
            }
            shown = known;
            shownLostAt = 0;
            strength = Math.max(0.0, Math.min(1.0, (double) (now - gazeSince - FADE_DELAY_MS) / FADE_IN_MS));
        } else {
            // Uno sconosciuto non ha etichetta: il silenzio basta a dirlo.
            gazeTarget = null;
            if (shown == null) return;
            if (shownLostAt == 0) shownLostAt = now;
            strength = 1.0 - (double) (now - shownLostAt) / FADE_OUT_MS;
            if (strength <= 0) {
                shown = null;
                return;
            }
        }
        int alpha = (int) (MAX_ALPHA * strength);
        if (alpha < 4) return; // sotto questa soglia il motore disegna testo pieno invece che trasparente

        Font font = mc.font;
        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int y = mc.getWindow().getGuiScaledHeight() / 2 + BELOW_CROSSHAIR;
        Component thought = Component.literal(shown).withStyle(ChatFormatting.ITALIC);
        g.pose().pushMatrix();
        g.pose().translate(cx, y);
        g.pose().scale(SCALE, SCALE);
        g.text(font, thought, -font.width(thought) / 2, 0, (alpha << 24) | 0xC8C8C8, false);
        g.pose().popMatrix();
    }

    /**
     * La persona piu' vicina lungo lo sguardo che passa tutte le condizioni, o nulla. Conta il corpo, non gli occhi:
     * da vicino basta guardare il petto, e un cono verso la testa farebbe entrare e uscire il bersaglio. Chi e' gia'
     * a fuoco ha un margine piu' largo, cosi' l'etichetta non vibra sul bordo.
     */
    private static Avatar lookedAt(Minecraft mc, LocalPlayer me) {
        Vec3 eye = me.getEyePosition();
        Vec3 end = eye.add(me.getViewVector(1.0F).scale(ClientImmersion.labelRange()));
        Avatar best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof Avatar a) || a == me || a.isInvisible() || a.isSpectator()) continue;
            if (!ImmersionRules.withinRange(me.distanceToSqr(a), ClientImmersion.labelRange())) continue;
            double margin = a.getUUID().equals(gazeTarget) ? FOCUSED_MARGIN : AIM_MARGIN;
            AABB box = a.getBoundingBox().inflate(margin);
            Optional<Vec3> hit = box.clip(eye, end);
            if (hit.isEmpty() && !box.contains(eye)) continue;
            double dist = hit.map(eye::distanceToSqr).orElse(0.0);
            if (dist >= bestDist || !me.hasLineOfSight(a)) continue;
            best = a;
            bestDist = dist;
        }
        return best;
    }
}
