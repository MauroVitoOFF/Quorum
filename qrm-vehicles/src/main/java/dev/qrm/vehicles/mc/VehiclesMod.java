package dev.qrm.vehicles.mc;

import dev.qrm.vehicles.net.VehicleNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** Veicoli: entità guidabili con carburante, chilometraggio e (nelle fasi successive) manutenzione. */
@Mod(VehiclesMod.MOD_ID)
public final class VehiclesMod {
    public static final String MOD_ID = "qrm_vehicles";

    public VehiclesMod(IEventBus modBus) {
        VehicleEntities.ENTITIES.register(modBus);
        VehicleSounds.SOUNDS.register(modBus);
        modBus.addListener(VehicleNetwork::register);
        NeoForge.EVENT_BUS.addListener(VehicleCommand::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(VehicleConfigFiles::onServerStarting);
    }
}
