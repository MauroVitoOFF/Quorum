package dev.qrm.police.weapon.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;

/**
 * Sbuffo di fumo dello sparo: un quadrato pixel art grigio chiaro e semitrasparente che sale piano, si ingrandisce e
 * svanisce, fermo nel mondo dove è nato. La texture è una delle tre di {@code particles/gun_smoke.json}.
 */
public final class GunSmokeParticle extends SingleQuadParticle {
    private static final float START_SIZE = 0.06f;
    private static final float END_SIZE = 0.30f;
    private static final float START_ALPHA = 0.75f;

    public GunSmokeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites.get(level.getRandom()));
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.friction = 0.92f;
        this.gravity = 0f;
        this.hasPhysics = false;
        this.lifetime = 28 + random.nextInt(16);
        this.roll = (float) (random.nextInt(4) * (Math.PI / 2));
        this.oRoll = this.roll;
        this.alpha = START_ALPHA;
        this.quadSize = START_SIZE;
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) age / lifetime;
        quadSize = START_SIZE + (END_SIZE - START_SIZE) * (float) Math.sqrt(life);
        float fade = life < 0.25f ? 1f : 1f - (life - 0.25f) / 0.75f;
        setAlpha(START_ALPHA * Math.max(0f, fade));
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }
}
