package dev.qrm.vehicles.mc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * I suoni dei veicoli: per ogni modello l'avviamento ({@code <id>.start}) e il motore ({@code <id>.idle}), che si
 * ripete in ciclo e cambia tono con i giri. Gli id coincidono con le chiavi di {@code sounds.json}. Un modello senza
 * suoni registrati resta silenzioso.
 */
public final class VehicleSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, VehiclesMod.MOD_ID);

    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> START = new HashMap<>();
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> ENGINE = new HashMap<>();

    static {
        registerVehicle("dodge_charger_69_black");
    }

    private VehicleSounds() {}

    private static void registerVehicle(String vehicleId) {
        START.put(vehicleId, register(vehicleId + ".start"));
        ENGINE.put(vehicleId, register(vehicleId + ".idle"));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String id) {
        return SOUNDS.register(id,
                () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(VehiclesMod.MOD_ID, id)));
    }

    /** Il suono di avviamento del modello, se ne ha uno. */
    public static Optional<SoundEvent> start(String vehicleId) {
        return Optional.ofNullable(START.get(vehicleId)).map(DeferredHolder::get);
    }

    /** Il suono del motore in ciclo del modello, se ne ha uno. */
    public static Optional<SoundEvent> engine(String vehicleId) {
        return Optional.ofNullable(ENGINE.get(vehicleId)).map(DeferredHolder::get);
    }
}
