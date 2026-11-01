package dev.qrm.police.weapon.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;

/**
 * Un punto della scia di un proiettile: fermo nel mondo, si accende per un istante quando il proiettile "passa" e si
 * spegne subito. Più punti allineati lungo la traiettoria, ciascuno con il proprio istante di passaggio, formano un
 * breve tratto luminoso che corre dalla canna al bersaglio.
 *
 * <p>L'istante di passaggio (in tick, frazionario) arriva dal chiamante nel parametro {@code xd}. La luminosità si
 * calcola a ogni fotogramma da età + frazione di tick, così il tratto resta corto anche se corre a molti blocchi per
 * tick.
 */
public final class TracerParticle extends SingleQuadParticle {
    /** Durata dell'accensione di un punto, in tick: con un proiettile a ~10 blocchi per tick il tratto è lungo ~3 blocchi. */
    private static final float WINDOW = 0.3f;
    private static final float SIZE = 0.05f;

    private final float passAt;

    public TracerParticle(ClientLevel level, double x, double y, double z, double passAt, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0, sprites.first());
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.hasPhysics = false;
        this.gravity = 0f;
        this.passAt = (float) passAt;
        this.lifetime = (int) Math.ceil(this.passAt + WINDOW) + 2;
        this.quadSize = SIZE;
        setColor(1.0f, 0.93f, 0.55f);
        this.alpha = 0f;
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
        float t = age + partialTick - passAt;
        float half = WINDOW / 2f;
        float level = 1f - Math.abs(t - half) / half;
        if (level <= 0f) return;
        this.alpha = Math.min(1f, level * 1.4f);
        super.extract(state, camera, partialTick);
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return 0xF000F0;
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }
}
