package dev.qrm.immersion.mc;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Le entita' del modulo. */
public final class ImmersionEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ImmersionMod.MOD_ID);

    /** Quasi senza dimensioni: niente hitbox ingombrante, non evocabile a mano (se resta sul disco senza nessuno sopra, sparisce al primo tick). */
    public static final Supplier<EntityType<SeatEntity>> SEAT = ENTITIES.register("seat",
            id -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                    .sized(0.01F, 0.01F)
                    .noSummon()
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    private ImmersionEntities() {}
}
