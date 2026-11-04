package dev.qrm.vehicles.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** Parte client del Module: tasti e invio dell'input del guidatore. Caricata solo sul client. */
@Mod(value = "qrm_vehicles", dist = Dist.CLIENT)
public final class VehiclesClient {

    public VehiclesClient(IEventBus modBus) {
        modBus.addListener(VehicleKeys::onRegisterKeys);
        NeoForge.EVENT_BUS.addListener(VehicleInput::onClientTick);
    }
}
