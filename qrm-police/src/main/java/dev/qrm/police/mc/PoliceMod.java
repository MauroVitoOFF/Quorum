package dev.qrm.police.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterCreatedEvent;
import dev.qrm.api.character.CharacterSelectedEvent;
import dev.qrm.api.emergency.EmergencyAlertEvent;
import dev.qrm.api.player.PlayerLeaveEvent;
import dev.qrm.api.point.PointOwner;
import dev.qrm.api.point.PointType;
import dev.qrm.api.point.PointTypeRegistry;
import dev.qrm.police.logic.CallType;
import dev.qrm.police.net.PoliceNetwork;
import dev.qrm.police.weapon.mc.ArmoryPoint;
import dev.qrm.police.weapon.mc.PistolServer;
import dev.qrm.police.weapon.mc.PistolSounds;
import dev.qrm.police.weapon.mc.WeaponComponents;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
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
 * Police: MDT portatile e terminale di centrale. Anagrafe e casellario, registro armi e dispatch delle chiamate
 * d'emergenza, anche quelle del medical.
 */
@Mod(PoliceMod.MOD_ID)
public final class PoliceMod {
    public static final String MOD_ID = "qrm_police";

    public PoliceMod(IEventBus modBus) {
        WeaponComponents.COMPONENTS.register(modBus);
        PistolSounds.SOUNDS.register(modBus);
        PoliceParticles.PARTICLES.register(modBus);
        PoliceItems.ITEMS.register(modBus);
        PoliceItems.BLOCKS.register(modBus);
        modBus.addListener(PoliceNetwork::register);
        modBus.addListener(PoliceMod::onBuildCreativeTab);
        NeoForge.EVENT_BUS.addListener(PoliceCommand::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(PoliceMod::onTick);
        NeoForge.EVENT_BUS.addListener(PoliceMod::onStopping);
        NeoForge.EVENT_BUS.addListener(PoliceMod::onLoggedIn);
        NeoForge.EVENT_BUS.addListener(PoliceMod::onLoggedOut);
        NeoForge.EVENT_BUS.addListener(PoliceMod::onStartTracking);
        NeoForge.EVENT_BUS.addListener(PoliceMod::onRespawn);
        NeoForge.EVENT_BUS.addListener(PoliceMod::onChangedDimension);
        QRM.get(PointTypeRegistry.class).register(new PointType("armory", "qrm_police.point.armory",
                Set.of(PointOwner.Kind.JOB, PointOwner.Kind.ORGANIZATION), false, ArmoryPoint::open));
        QRM.events().register(EmergencyAlertEvent.class, MOD_ID, PoliceMod::onEmergency);
        QRM.events().register(CharacterSelectedEvent.class, MOD_ID, e -> {
            // Ogni personaggio è anche un cittadino dell'anagrafe.
            var c = e.character();
            PoliceServer.register(c);
            PistolServer.onCharacterSelected(c);
        });
        QRM.events().register(CharacterCreatedEvent.class, MOD_ID, e -> PoliceServer.register(e.character()));
        QRM.events().register(PlayerLeaveEvent.class, MOD_ID, e -> PoliceServer.forget(e.playerId().value()));
    }

    private static void onEmergency(EmergencyAlertEvent e) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        CallType type = CallType.parse(e.kind().name());
        PoliceServer.raiseCall(server, type, e.description(), e.dimension(), e.x(), e.y(), e.z(), e.reporterName(),
                e.reporterId().toString(), "");
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(PoliceItems.TABLET.get());
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(PoliceItems.COMPUTER_ITEM.get());
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(PoliceItems.PISTOL.get());
            event.accept(PoliceItems.MAGAZINE.get());
            event.accept(PoliceItems.BULLET.get());
        }
    }

    private static void onTick(ServerTickEvent.Post event) {
        PoliceServer.tick(event.getServer());
        PistolServer.tick(event.getServer());
    }

    private static void onStopping(ServerStoppingEvent event) {
        PoliceServer.reset();
        PistolServer.reset();
    }

    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer player) PistolServer.onLogin(player);
    }

    private static void onStartTracking(PlayerEvent.StartTracking e) {
        if (e.getEntity() instanceof ServerPlayer tracker) PistolServer.onStartTracking(tracker, e.getTarget());
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer player) PistolServer.onRespawnOrDimensionChange(player);
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer player) PistolServer.onRespawnOrDimensionChange(player);
    }

    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent e) {
        PoliceServer.forget(e.getEntity().getUUID());
        if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) PistolServer.cancelRefill(sp, false);
        PistolServer.forget(e.getEntity().getUUID());
    }
}
