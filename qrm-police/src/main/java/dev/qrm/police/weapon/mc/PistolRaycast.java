package dev.qrm.police.weapon.mc;

import dev.qrm.police.weapon.logic.Ballistics;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Traiettoria di un colpo di pistola (solo server): blocchi, bersagli e suono d'impatto. */
public final class PistolRaycast {
    private PistolRaycast() {}

    /** Distanza dall'altezza degli occhi del bersaglio entro cui il colpo è alla testa. */
    private static final double HEAD_MARGIN = 0.3;

    /**
     * Esito di un colpo.
     *
     * @param from     origine (occhi del tiratore)
     * @param to       punto d'impatto, o fine della gittata se il colpo non tocca nulla
     * @param block    blocco colpito, se il colpo si è fermato su un blocco
     * @param target   bersaglio vivente colpito, se c'è
     * @param headshot vero se il bersaglio è stato colpito alla testa
     */
    public record Hit(Vec3 from, Vec3 to, Optional<BlockPos> block, Optional<LivingEntity> target, boolean headshot) {}

    /** Traccia il colpo di {@code shooter}, con la dispersione della mira o del fianco. Non applica danni. */
    public static Hit trace(ServerPlayer shooter, boolean aiming, RandomGenerator random) {
        ServerLevel level = shooter.level();
        Vec3 from = shooter.getEyePosition();
        double[] dir = Ballistics.deviate(shooter.getYRot(), shooter.getXRot(), aiming, shooter.isCrouching(), random);
        Vec3 end = from.add(shooter.calculateViewVector((float) dir[1], (float) dir[0]).scale(Ballistics.RANGE));

        BlockHitResult blockHit = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, shooter));
        Vec3 limit = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

        AABB area = new AABB(from, limit).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(shooter, from, limit, area,
                e -> isTarget(shooter, e), from.distanceToSqr(limit));
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
            Vec3 at = entityHit.getLocation();
            boolean head = at.y >= target.getEyeY() - HEAD_MARGIN;
            return new Hit(from, at, Optional.empty(), Optional.of(target), head);
        }
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            return new Hit(from, blockHit.getLocation(), Optional.of(blockHit.getBlockPos()), Optional.empty(), false);
        }
        return new Hit(from, end, Optional.empty(), Optional.empty(), false);
    }

    private static boolean isTarget(ServerPlayer shooter, Entity e) {
        return e != shooter && e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e.isPickable();
    }

    // ---- suono d'impatto ----

    private static final Set<SoundType> METAL = setOf(SoundType.METAL, SoundType.ANVIL,
            SoundType.CHAIN, SoundType.LANTERN, SoundType.NETHERITE_BLOCK, SoundType.COPPER, SoundType.COPPER_BULB,
            SoundType.COPPER_GRATE, SoundType.IRON, SoundType.HEAVY_CORE, SoundType.VAULT, SoundType.TRIAL_SPAWNER,
            SoundType.SPAWNER, SoundType.LODESTONE);
    private static final Set<SoundType> WOOD = setOf(SoundType.WOOD, SoundType.NETHER_WOOD,
            SoundType.BAMBOO_WOOD, SoundType.CHERRY_WOOD, SoundType.LADDER, SoundType.SCAFFOLDING, SoundType.BAMBOO,
            SoundType.CHISELED_BOOKSHELF, SoundType.SHELF, SoundType.HANGING_SIGN, SoundType.NETHER_WOOD_HANGING_SIGN,
            SoundType.BAMBOO_WOOD_HANGING_SIGN, SoundType.CHERRY_WOOD_HANGING_SIGN);
    private static final Set<SoundType> STONE = setOf(SoundType.STONE, SoundType.DEEPSLATE,
            SoundType.DEEPSLATE_BRICKS, SoundType.DEEPSLATE_TILES, SoundType.POLISHED_DEEPSLATE, SoundType.BASALT,
            SoundType.NETHER_BRICKS, SoundType.NETHERRACK, SoundType.NETHER_ORE, SoundType.NETHER_GOLD_ORE,
            SoundType.GILDED_BLACKSTONE, SoundType.TUFF, SoundType.TUFF_BRICKS, SoundType.POLISHED_TUFF,
            SoundType.CALCITE, SoundType.DRIPSTONE_BLOCK, SoundType.POINTED_DRIPSTONE, SoundType.BONE_BLOCK,
            SoundType.ANCIENT_DEBRIS, SoundType.GLASS, SoundType.AMETHYST, SoundType.MUD_BRICKS,
            SoundType.RESIN_BRICKS, SoundType.DECORATED_POT, SoundType.DECORATED_POT_CRACKED);

    /** Insieme tollerante: alcune costanti di {@link SoundType} potrebbero essere lo stesso oggetto. */
    private static Set<SoundType> setOf(SoundType... types) {
        return new HashSet<>(Arrays.asList(types));
    }

    /**
     * Suono d'impatto per il blocco colpito, dal suo {@link SoundType}: metalli, legni e pietre/vetri/mattoni hanno
     * il loro suono; tutto il resto (terra, sabbia, lana, piante, ...) e le creature usano {@code impact.soft}.
     */
    public static Holder<SoundEvent> impactSound(ServerLevel level, Optional<BlockPos> block, ServerPlayer shooter) {
        if (block.isEmpty()) return PistolSounds.IMPACT_SOFT;
        SoundType type = level.getBlockState(block.get()).getSoundType(level, block.get(), shooter);
        if (METAL.contains(type)) return PistolSounds.IMPACT_METAL;
        if (WOOD.contains(type)) return PistolSounds.IMPACT_WOOD;
        if (STONE.contains(type)) return PistolSounds.IMPACT_STONE;
        return PistolSounds.IMPACT_SOFT;
    }
}
