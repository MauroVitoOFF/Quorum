package dev.qrm.vehicles.mc;

import dev.qrm.vehicles.logic.VehicleDef;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
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
                    b -> b.sized(2.2f, 1.6f).clientTrackingRange(10).updateInterval(1));

    /** Charger '69 nera: 2,6 di larghezza (corpo 40,7 unità) e 1,8 di altezza (tetto a 27,8 unità). */
    public static final DeferredHolder<EntityType<?>, EntityType<QrmVehicleEntity>> CHARGER_BLACK =
            ENTITIES.registerEntityType("dodge_charger_69_black", QrmVehicleEntity::new, MobCategory.MISC,
                    b -> b.sized(2.6f, 1.8f).clientTrackingRange(10).updateInterval(1));

    public static final List<DeferredHolder<EntityType<?>, EntityType<QrmVehicleEntity>>> ALL =
            List.of(JEEP, CHARGER_BLACK);

    private VehicleEntities() {}

    /** Il tipo di entità del veicolo con questo id, se esiste. */
    public static Optional<EntityType<QrmVehicleEntity>> typeOf(String id) {
        return ALL.stream().filter(h -> h.getId().getPath().equals(id)).map(DeferredHolder::get).findFirst();
    }

    /** I parametri del veicolo di questo tipo di entità (la Jeep se il tipo non è uno dei nostri). */
    public static VehicleDef defOf(EntityType<?> type) {
        return VehicleDef.byId(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath()).orElse(VehicleDef.JEEP);
    }
}
