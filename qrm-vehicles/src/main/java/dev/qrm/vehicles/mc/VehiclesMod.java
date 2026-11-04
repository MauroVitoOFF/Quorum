package dev.qrm.vehicles.mc;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Veicoli: entità guidabili con carburante, chilometraggio e (nelle fasi successive) manutenzione. */
@Mod(VehiclesMod.MOD_ID)
public final class VehiclesMod {
    public static final String MOD_ID = "qrm_vehicles";

    public VehiclesMod(IEventBus modBus) {}
}
