package dev.qrm.wardrobe.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterSelectedEvent;
import dev.qrm.api.job.DutyChangedEvent;
import dev.qrm.api.job.JobChangedEvent;
import dev.qrm.api.player.PlayerJoinEvent;
import dev.qrm.api.point.PointOwner;
import dev.qrm.api.point.PointType;
import dev.qrm.api.point.PointTypeRegistry;
import java.util.Set;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Divise di lavoro: skin cambiate da un armadietto, sui punti di interazione di tipo {@code wardrobe}. */
@Mod(WardrobeMod.MOD_ID)
public final class WardrobeMod {
    public static final String MOD_ID = "qrm_wardrobe";
    public static final String POINT_TYPE = "wardrobe";
    static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public WardrobeMod(IEventBus modBus) {
        QRM.get(PointTypeRegistry.class).register(new PointType(POINT_TYPE, "qrm_wardrobe.point.wardrobe",
                Set.of(PointOwner.Kind.JOB), true, WardrobeServer::onUse));
        NeoForge.EVENT_BUS.addListener(WardrobeCommand::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(WardrobeMod::onServerStarted);
        NeoForge.EVENT_BUS.addListener(WardrobeMod::onServerStopped);
        QRM.events().register(DutyChangedEvent.class, MOD_ID, e -> WardrobeServer.revalidate(e.character()));
        QRM.events().register(JobChangedEvent.class, MOD_ID, e -> WardrobeServer.revalidate(e.character()));
        QRM.events().register(CharacterSelectedEvent.class, MOD_ID, e -> {
            WardrobeServer.revalidate(e.character().id());
            WardrobeServer.onWornMayHaveChanged();
        });
        QRM.events().register(PlayerJoinEvent.class, MOD_ID, e -> WardrobeServer.onPlayerJoined(e.playerId()));
    }

    private static void onServerStarted(ServerStartedEvent event) {
        WardrobeServer.service();
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        WardrobeServer.reset();
    }
}
