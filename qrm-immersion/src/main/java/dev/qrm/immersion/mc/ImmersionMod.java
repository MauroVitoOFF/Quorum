package dev.qrm.immersion.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterSelectedEvent;
import dev.qrm.api.player.PlayerLeaveEvent;
import dev.qrm.immersion.net.ImmersionNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Immersione: nomi nascosti finche' non ci si presenta, linguaggio del corpo (mani alzate, indicare),
 * bussare alle porte e sedersi su scale e lastre.
 */
@Mod(ImmersionMod.MOD_ID)
public final class ImmersionMod {
    public static final String MOD_ID = "qrm_immersion";

    public ImmersionMod(IEventBus modBus) {
        ImmersionEntities.ENTITIES.register(modBus);
        modBus.addListener(ImmersionNetwork::register);
        ImmersionEvents.register();
        NeoForge.EVENT_BUS.addListener(ImmersionCommand::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(ImmersionMod::onServerStopped);
        NeoForge.EVENT_BUS.addListener(ImmersionMod::onLoggedOut);
        QRM.events().register(CharacterSelectedEvent.class, MOD_ID, e -> {
            ImmersionServer.forget(e.character().playerId().value());
            ImmersionServer.resyncAll();
        });
        QRM.events().register(PlayerLeaveEvent.class, MOD_ID, e -> ImmersionServer.forget(e.playerId().value()));
    }

    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent e) {
        ImmersionServer.forget(e.getEntity().getUUID());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        ImmersionServer.reset();
    }
}
