package dev.qrm.vehicles.client;

import dev.qrm.vehicles.mc.QrmVehicleEntity;
import dev.qrm.vehicles.mc.VehicleSounds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

/** Tiene un solo suono del motore per ogni veicolo acceso, vicino o lontano che sia (il volume lo gestisce il gioco). */
public final class EngineSounds {
    private static final Map<Integer, EngineSoundInstance> ACTIVE = new HashMap<>();

    private EngineSounds() {}

    /** Da chiamare a ogni tick del veicolo sul client. */
    public static void tick(QrmVehicleEntity vehicle) {
        if (!vehicle.engineOn()) return;
        EngineSoundInstance current = ACTIVE.get(vehicle.getId());
        if (current != null && !current.isStopped()) return;
        // I suoni finiti (motore spento, veicolo sparito, mondo lasciato) tengono il veicolo e il suo mondo: si tolgono.
        ACTIVE.values().removeIf(EngineSoundInstance::isStopped);
        VehicleSounds.engine(vehicle.def().id()).ifPresent(event -> {
            EngineSoundInstance sound = new EngineSoundInstance(vehicle, event);
            ACTIVE.put(vehicle.getId(), sound);
            Minecraft.getInstance().getSoundManager().play(sound);
        });
    }
}
