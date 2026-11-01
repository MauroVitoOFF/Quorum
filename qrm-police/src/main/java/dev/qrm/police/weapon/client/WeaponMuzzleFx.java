package dev.qrm.police.weapon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.qrm.police.weapon.logic.WeaponDef;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Vampa e fumo dello sparo del giocatore locale, in prima persona.
 *
 * <p>Vampa: una stella pixel art ({@code textures/effect/muzzle_flash.png}) ferma sulla volata della canna e rivolta
 * all'indietro, solidale con l'arma, disegnata per {@link #FLASH_TICKS} tick, a piena luce, che si rimpicciolisce e
 * svanisce; a ogni sparo ruota di un angolo a caso.
 *
 * <p>Fumo: 4-5 pixel chiari che escono dalla canna, dove sta la vampa, salgono veloci e svaniscono subito sfumando.
 * Si disegnano nella stessa passata della mano, nello spazio della vista (non nel mondo).
 */
public final class WeaponMuzzleFx {
    private static final Identifier FLASH_TEXTURE =
            Identifier.fromNamespaceAndPath("qrm_police", "textures/effect/muzzle_flash.png");
    private static final int FLASH_TICKS = 8;
    /** Lato massimo della vampa, in blocchi di vista. */
    private static final float FLASH_SIZE = 0.90f;
    /** Tick, dopo lo sparo, in cui escono i pixel di fumo. */
    private static final int SMOKE_TICKS = 1;
    private static final int PIXELS_PER_TICK = 5;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final Random RANDOM = new Random();
    private static final Identifier SMOKE_PIXEL =
            Identifier.fromNamespaceAndPath("qrm_police", "textures/effect/smoke_pixel.png");
    /** Lato di un pixel di fumo, in blocchi di vista (variabile tra i due estremi). */
    private static final float PIXEL_MIN = 0.012f;
    private static final float PIXEL_MAX = 0.028f;
    private static final float PIXEL_ALPHA = 0.85f;

    /** Pixel di fumo nello spazio della vista della mano (blocchi), con velocità per tick. */
    private static final class Puff {
        final Vector3f pos;
        final Vector3f vel;
        final float size = PIXEL_MIN + RANDOM.nextFloat() * (PIXEL_MAX - PIXEL_MIN);
        final int life = 5 + RANDOM.nextInt(4);
        int age;

        Puff(Vector3f pos, Vector3f vel) {
            this.pos = pos;
            this.vel = vel;
        }
    }

    private static final List<Puff> PUFFS = new ArrayList<>();
    private static int flashAge = FLASH_TICKS;
    private static float flashRoll;
    private static int smokeLeft;
    /** Volata nello spazio della vista, all'ultimo disegno. */
    private static Vector3f lastMuzzle;

    private WeaponMuzzleFx() {}

    /** Volata nello spazio del modello (blocchi): centro della bocca, un po' davanti alla canna. */
    private static Vector3f muzzle(WeaponDef def) {
        float[] p = def.muzzle().point();
        return new Vector3f(p[0], p[1], p[2]).div(16f);
    }

    /** Sparo del giocatore locale (evento {@code muzzle_flash} della clip). */
    static void trigger() {
        flashAge = 0;
        flashRoll = RANDOM.nextFloat() * 360f;
        smokeLeft = SMOKE_TICKS;
    }

    /** Da registrare sul bus di NeoForge, lato client. */
    public static void onClientTick(ClientTickEvent.Post event) {
        if (flashAge < FLASH_TICKS) flashAge++;
        for (Iterator<Puff> it = PUFFS.iterator(); it.hasNext();) {
            Puff p = it.next();
            p.pos.add(p.vel);
            if (++p.age >= p.life) it.remove();
        }
        if (smokeLeft <= 0) return;
        smokeLeft--;
        if (lastMuzzle == null) return;
        for (int i = 0; i < PIXELS_PER_TICK; i++) {
            Vector3f at = new Vector3f(lastMuzzle).add(jitter(0.008f), jitter(0.008f), jitter(0.008f));
            // salgono veloci, con pochissima dispersione
            Vector3f vel = new Vector3f(jitter(0.004f), 0.014f + RANDOM.nextFloat() * 0.018f,
                    -0.001f - RANDOM.nextFloat() * 0.004f);
            PUFFS.add(new Puff(at, vel));
        }
    }

    private static float jitter(float amount) {
        return (RANDOM.nextFloat() - 0.5f) * 2f * amount;
    }

    /**
     * Da chiamare con la PoseStack già nello spazio del modello della pistola.
     *
     * @param barrel matrice del bone della canna nello spazio del modello, o {@code null}
     */
    static void render(PoseStack modelPose, Matrix4f barrel, WeaponDef def, SubmitNodeCollector collector,
            float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        Vector3f muzzle = muzzle(def);
        Vector3f local = new Vector3f(muzzle);
        if (barrel != null) barrel.transformPosition(local);
        Vector3f view = modelPose.last().pose().transformPosition(local, new Vector3f());
        lastMuzzle = view;
        renderSmoke(collector, partialTick);

        float age = flashAge + partialTick;
        if (age >= FLASH_TICKS) return;
        float life = age / FLASH_TICKS;
        float size = FLASH_SIZE * (1.0f - 0.55f * life);
        int alpha = Math.round(255f * (1f - life * life));
        if (alpha <= 8) return;

        // Solidale con l'arma: nello spazio del modello, sulla volata e perpendicolare alla canna (rivolta a chi
        // spara), così segue posizione e inclinazione dell'arma e non dipende dai movimenti del giocatore.
        PoseStack pose = new PoseStack();
        pose.mulPose(modelPose.last().pose());
        if (barrel != null) pose.mulPose(barrel);
        pose.translate(muzzle.x, muzzle.y, muzzle.z);
        pose.mulPose(Axis.ZP.rotationDegrees(flashRoll));
        size /= WeaponPose.scale(); // un blocco del modello è `scale` blocchi di vista
        float h = size / 2f;
        int color = (alpha << 24) | 0xFFFFFF;
        RenderType type = RenderTypes.entityTranslucent(FLASH_TEXTURE);
        collector.submitCustomGeometry(pose, type, (p, buffer) -> {
            quad(buffer, p, -h, -h, 0f, 1f, color);
            quad(buffer, p, h, -h, 1f, 1f, color);
            quad(buffer, p, h, h, 1f, 0f, color);
            quad(buffer, p, -h, h, 0f, 0f, color);
        });
    }

    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose, float x, float y,
            float u, float v, int color) {
        buffer.addVertex(pose, x, y, 0f).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT).setNormal(pose, 0f, 0f, 1f);
    }

    private static void renderSmoke(SubmitNodeCollector collector, float partialTick) {
        for (Puff p : PUFFS) {
            float t = Math.min(1f, (p.age + partialTick) / p.life);
            int alpha = Math.round(255f * PIXEL_ALPHA * (1f - t * t));
            if (alpha <= 8) continue;
            PoseStack pose = new PoseStack();
            pose.translate(p.pos.x + p.vel.x * partialTick, p.pos.y + p.vel.y * partialTick,
                    p.pos.z + p.vel.z * partialTick);
            float h = p.size / 2f;
            int color = (alpha << 24) | 0xFFFFFF;
            collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(SMOKE_PIXEL), (q, buffer) -> {
                quad(buffer, q, -h, -h, 0f, 1f, color);
                quad(buffer, q, h, -h, 1f, 1f, color);
                quad(buffer, q, h, h, 1f, 0f, color);
                quad(buffer, q, -h, h, 0f, 0f, color);
            });
        }
    }
}
