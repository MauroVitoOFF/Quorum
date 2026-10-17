package dev.qrm.medical.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterSelectedEvent;
import dev.qrm.api.player.PlayerLeaveEvent;
import dev.qrm.medical.net.MedicalNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Medical: nessuna morte vanilla. A zero vita si cade a terra, poi si perdono i sensi, poi si è ricoverati.
 * Soccorsi: RCP, defibrillatore, kit. Stato legato alla Character.
 */
@Mod(MedicalMod.MOD_ID)
public final class MedicalMod {
    public static final String MOD_ID = "qrm_medical";

    public MedicalMod(IEventBus modBus) {
        MedicalItems.ITEMS.register(modBus);
        modBus.addListener(MedicalNetwork::register);
        modBus.addListener(MedicalMod::onBuildCreativeTab);
        MedicalEvents.register();
        NeoForge.EVENT_BUS.addListener(MedicalCommand::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(MedicalMod::onServerStopped);
        NeoForge.EVENT_BUS.addListener(MedicalMod::onLoggedOut);
        QRM.events().register(CharacterSelectedEvent.class, MOD_ID, e -> MedicalServer.forget(e.character().playerId().value()));
        QRM.events().register(PlayerLeaveEvent.class, MOD_ID, e -> MedicalServer.forget(e.playerId().value()));
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MedicalItems.DEFIBRILLATOR.get());
            event.accept(MedicalItems.MEDKIT.get());
            event.accept(MedicalItems.BANDAGE.get());
        }
    }

    private static void onLoggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) {
        MedicalServer.forget(e.getEntity().getUUID());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        MedicalServer.reset();
    }
}
