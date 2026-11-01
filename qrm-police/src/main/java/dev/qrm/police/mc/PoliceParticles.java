package dev.qrm.police.mc;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Particelle del modulo: il fumo dello sparo ({@code gun_smoke}) e la scia del proiettile ({@code tracer}). */
public final class PoliceParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, PoliceMod.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GUN_SMOKE =
            PARTICLES.register("gun_smoke", () -> new SimpleParticleType(false));

    /** Scia luminosa del proiettile ({@code particles/tracer.json}). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TRACER =
            PARTICLES.register("tracer", () -> new SimpleParticleType(false));

    private PoliceParticles() {}
}
