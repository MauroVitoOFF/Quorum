package dev.qrm.police.weapon.client;

import dev.qrm.police.weapon.logic.anim.AnimEvent;
import dev.qrm.police.weapon.logic.anim.RemoteSoundScheduler;
import dev.qrm.police.weapon.mc.PistolSounds;
import dev.qrm.police.weapon.net.PistolPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Effetti visivi e suoni di manipolazione degli altri, solo client e a basso costo.
 *
 * <p>Niente suoni di sparo o di impatto: li trasmette il server. Ogni accesso a livello ed entità controlla che
 * esistano ancora.
 */
public final class PistolEffects {
    private static final RemoteSoundScheduler REMOTE_SOUNDS = new RemoteSoundScheduler();
    private static final int IMPACT_BLOCK_PARTICLES = 6;
    private static final int IMPACT_ENTITY_PARTICLES = 4;

    private PistolEffects() {}

    /** Registra l'ascoltatore degli eventi della clip del giocatore locale. */
    public static void init() {
        PistolClientEvents.addListener(PistolEffects::onLocalEvent);
    }

    private static void onLocalEvent(AnimEvent e) {
        if (!"muzzle_flash".equals(e.type())) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 muzzle = player.getEyePosition().add(look.scale(0.7)).add(right.scale(0.15)).add(0, -0.12, 0);
        muzzleBurst(level, muzzle, look);
    }

    /** Colpo esploso: vampa e bossolo per gli altri, particelle di impatto per tutti. */
    public static void onShot(PistolPayloads.Shot shot) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        LocalPlayer local = mc.player;
        boolean own = local != null && local.getId() == shot.entityId();
        if (!own) {
            Vec3 dir = shot.to().subtract(shot.from());
            if (dir.lengthSqr() > 1.0E-6) {
                dir = dir.normalize();
                Vec3 muzzle = shot.from().add(dir.scale(0.5));
                muzzleBurst(level, muzzle, dir);
                Vec3 right = dir.cross(new Vec3(0, 1, 0));
                if (right.lengthSqr() > 1.0E-6) {
                    Vec3 casing = shot.from().add(right.normalize().scale(0.3)).add(0, -0.1, 0);
                    level.addParticle(ParticleTypes.CRIT, casing.x, casing.y, casing.z, 0.05, 0.05, 0.0);
                }
            }
        }
        impact(level, shot);
    }

    private static void muzzleBurst(ClientLevel level, Vec3 at, Vec3 dir) {
        level.addParticle(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, dir.x * 0.05, dir.y * 0.05, dir.z * 0.05);
        level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, dir.x * 0.02, dir.y * 0.02 + 0.01, dir.z * 0.02);
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, dir.x * 0.1, dir.y * 0.1, dir.z * 0.1);
    }

    private static void impact(ClientLevel level, PistolPayloads.Shot shot) {
        Vec3 to = shot.to();
        if (shot.block().isPresent()) {
            BlockPos pos = shot.block().get();
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, state);
                for (int i = 0; i < IMPACT_BLOCK_PARTICLES; i++) {
                    level.addParticle(option, to.x, to.y, to.z,
                            (level.getRandom().nextDouble() - 0.5) * 0.2, level.getRandom().nextDouble() * 0.15,
                            (level.getRandom().nextDouble() - 0.5) * 0.2);
                }
            }
        }
        if (shot.hitEntity()) {
            for (int i = 0; i < IMPACT_ENTITY_PARTICLES; i++) {
                level.addParticle(ParticleTypes.CRIT, to.x, to.y, to.z,
                        (level.getRandom().nextDouble() - 0.5) * 0.3, level.getRandom().nextDouble() * 0.2,
                        (level.getRandom().nextDouble() - 0.5) * 0.3);
            }
        }
    }

    /** Un altro giocatore inizia un'azione: pianifica i suoni della sua clip alla sua posizione. */
    public static void onRemoteActionStarted(int entityId, String clipName) {
        REMOTE_SOUNDS.start(entityId, PistolAssets.clips().get(clipName));
    }

    public static void onRemoteActionEnded(int entityId) {
        REMOTE_SOUNDS.cancel(entityId);
    }

    /** Uscita dal mondo o cambio di livello. */
    public static void clear() {
        REMOTE_SOUNDS.clear();
    }

    /** Da registrare sul bus di NeoForge, lato client. */
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            REMOTE_SOUNDS.clear();
            return;
        }
        for (RemoteSoundScheduler.Due due : REMOTE_SOUNDS.tick()) {
            Entity entity = level.getEntity(due.key());
            if (entity == null || entity.isRemoved()) {
                REMOTE_SOUNDS.cancel(due.key());
                continue;
            }
            PistolSounds.byId(due.soundId()).ifPresent(sound -> level.playLocalSound(entity.getX(), entity.getY(),
                    entity.getZ(), sound.value(), SoundSource.PLAYERS, 1f, 1f, false));
        }
    }
}
