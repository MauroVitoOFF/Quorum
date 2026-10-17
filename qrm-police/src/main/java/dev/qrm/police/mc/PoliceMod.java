package dev.qrm.police.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterSelectedEvent;
import dev.qrm.api.emergency.EmergencyAlertEvent;
import dev.qrm.api.player.PlayerLeaveEvent;
import dev.qrm.police.logic.CallType;
import dev.qrm.police.net.PoliceNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Police: MDT portatile e terminale di centrale. Anagrafe e casellario, registro armi, BOLO e dispatch delle chiamate
 * d'emergenza, anche quelle del medical.
 */
@Mod(PoliceMod.MOD_ID)
public final class PoliceMod {
    public static final String MOD_ID = "qrm_police";

    public PoliceMod(IEventBus modBus) {
        PoliceItems.ITEMS.register(modBus);
        PoliceItems.BLOCKS.register(modBus);
        modBus.addListener(PoliceNetwork::register);
        modBus.addListener(PoliceMod::onBuildCreativeTab);
        NeoForge.EVENT_BUS.addListener(PoliceCommand::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(PoliceMod::onTick);
        NeoForge.EVENT_BUS.addListener(PoliceMod::onStopping);
        NeoForge.EVENT_BUS.addListener(PoliceMod::onLoggedOut);
        QRM.events().register(EmergencyAlertEvent.class, MOD_ID, PoliceMod::onEmergency);
        QRM.events().register(CharacterSelectedEvent.class, MOD_ID, e -> {
            // Ogni personaggio è anche un cittadino dell'anagrafe.
            var c = e.character();
            PoliceServer.db().ensureCitizen(c.id().value(), c.firstName(), c.lastName(), c.birthDate().toString());
        });
        QRM.events().register(PlayerLeaveEvent.class, MOD_ID, e -> PoliceServer.forget(e.playerId().value()));
    }

    private static void onEmergency(EmergencyAlertEvent e) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        CallType type = CallType.parse(e.kind().name());
        PoliceServer.raiseCall(server, type, e.description(), e.dimension(), e.x(), e.y(), e.z(), e.reporterName());
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(PoliceItems.TABLET.get());
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(PoliceItems.COMPUTER_ITEM.get());
    }

    private static void onTick(ServerTickEvent.Post event) {
        PoliceServer.tick(event.getServer());
    }

    private static void onStopping(ServerStoppingEvent event) {
        PoliceServer.reset();
    }

    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent e) {
        PoliceServer.forget(e.getEntity().getUUID());
    }
}
