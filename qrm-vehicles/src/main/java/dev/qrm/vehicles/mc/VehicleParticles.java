package dev.qrm.vehicles.mc;

import dev.qrm.vehicles.logic.ParticleRules;
import dev.qrm.vehicles.logic.Surface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Le particelle di un veicolo, solo lato client: polvere dalle ruote posteriori secondo il fondo, fumo delle gomme
 * quando strisciano e uno sbuffo allo scarico a ogni cambio marcia. Tutto si ricava da ciò che il client già
 * conosce (posizione, velocità, marcia): niente dati in più sulla rete.
 */
final class VehicleParticles {
    private static final double TICKS_PER_SECOND = 20.0;
    /** Distanza delle ruote posteriori dal centro, in frazione della larghezza del veicolo. */
    private static final double HALF_TRACK_OF_WIDTH = 0.4;

    private int lastGear = 1;
    private double lastSpeedMs;

    void tick(QrmVehicleEntity v, Surface surface) {
        Level level = v.level();
        RandomSource random = level.getRandom();
        double speedMs = v.speed() * TICKS_PER_SECOND;
        double decel = (lastSpeedMs - speedMs) * TICKS_PER_SECOND;
        lastSpeedMs = speedMs;

        double yaw = Math.toRadians(v.getYRot());
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        // Lato destro del veicolo: perpendicolare al muso (davanti = (-sin, cos) in Minecraft).
        double rx = -fz;
        double rz = fx;
        double lateral = ((v.getX() - v.xo) * rx + (v.getZ() - v.zo) * rz) * TICKS_PER_SECOND;

        double rear = -v.def().wheelBase() * 0.5;
        double side = v.getBbWidth() * HALF_TRACK_OF_WIDTH;
        boolean skid = ParticleRules.skidding(lateral, decel, speedMs);
        double dust = ParticleRules.dust(surface.id(), speedMs);
        BlockState ground = null;
        if (dust > 0 || skid) ground = level.getBlockState(BlockPos.containing(v.getX(), v.getY() - 0.2, v.getZ()));

        for (int i = -1; i <= 1; i += 2) {
            double wx = v.getX() + fx * rear + rx * side * i;
            double wz = v.getZ() + fz * rear + rz * side * i;
            // Le particelle volano all'indietro rispetto al moto, con un po' di dispersione.
            double back = Math.signum(speedMs) * -0.08;
            if (dust > 0 && !ground.isAir() && random.nextDouble() < dust) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), wx, v.getY() + 0.1, wz,
                        fx * back + (random.nextDouble() - 0.5) * 0.08, 0.06 + random.nextDouble() * 0.06,
                        fz * back + (random.nextDouble() - 0.5) * 0.08);
            }
            if (skid) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, wx, v.getY() + 0.1, wz,
                        (random.nextDouble() - 0.5) * 0.02, 0.02, (random.nextDouble() - 0.5) * 0.02);
            }
        }

        int gear = v.gear();
        if (v.engineOn() && ParticleRules.shifted(lastGear, gear)) {
            double ex = v.getX() + fx * (rear - 0.4) + rx * side * 0.7;
            double ez = v.getZ() + fz * (rear - 0.4) + rz * side * 0.7;
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.SMOKE, ex, v.getY() + 0.3, ez,
                        fx * -0.03 + (random.nextDouble() - 0.5) * 0.02, 0.02, fz * -0.03 + (random.nextDouble() - 0.5) * 0.02);
            }
        }
        lastGear = gear;
    }
}
