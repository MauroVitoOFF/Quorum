package dev.qrm.vehicles.mc;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** I tipi di entità dei veicoli. */
public final class VehicleEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(VehiclesMod.MOD_ID);

    /** Jeep Wrangler: 1,9 di larghezza e 1,6 di altezza (tetto), aggiornata a ogni tick per un movimento fluido. */
    public static final DeferredHolder<EntityType<?>, EntityType<QrmVehicleEntity>> JEEP =
            ENTITIES.registerEntityType("jeep_wrangler", QrmVehicleEntity::new, MobCategory.MISC,
                    b -> b.sized(1.9f, 1.6f).clientTrackingRange(10).updateInterval(1));

    private VehicleEntities() {}
}
