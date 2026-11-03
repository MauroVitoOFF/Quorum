package dev.qrm.clock.mc;

import dev.qrm.api.clock.ClockService;
import dev.qrm.api.internal.ServiceRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Il tempo di gioco: il giorno dura più di 20 minuti, la data e la stagione seguono il calendario reale.
 * Espone {@link ClockService} agli altri Module.
 */
@Mod(ClockMod.MOD_ID)
public final class ClockMod {
    public static final String MOD_ID = "qrm_clock";

    public ClockMod(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ClockConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(ClockCommand::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(ClockMod::onServerStarting);
        NeoForge.EVENT_BUS.addListener(ClockMod::onServerStopped);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> ClockServer.tick(e.getServer()));
    }

    private static void onServerStarting(ServerStartingEvent e) {
        ServiceRegistry.unregister(ClockService.class);
        ServiceRegistry.register(ClockService.class, ClockServer::now);
    }

    private static void onServerStopped(ServerStoppedEvent e) {
        ServiceRegistry.unregister(ClockService.class);
    }
}
