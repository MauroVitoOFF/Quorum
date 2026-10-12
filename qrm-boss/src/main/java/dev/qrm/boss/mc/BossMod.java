package dev.qrm.boss.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.point.PointOwner;
import dev.qrm.api.point.PointType;
import dev.qrm.api.point.PointTypeRegistry;
import dev.qrm.boss.net.BossNetwork;
import dev.qrm.boss.server.BossCommands;
import dev.qrm.boss.server.BossDeskPoint;
import dev.qrm.boss.server.BossServer;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Menu del boss aziendale: comando {@code /boss} e punto di interazione {@code boss_desk}. */
@Mod(BossMod.MOD_ID)
public final class BossMod {
    public static final String MOD_ID = "qrm_boss";
    public static final String POINT_TYPE = "boss_desk";

    public BossMod(IEventBus modBus) {
        modBus.addListener(BossNetwork::register);
        QRM.get(PointTypeRegistry.class).register(new PointType(POINT_TYPE, "qrm_boss.point.desk",
                Set.of(PointOwner.Kind.ORGANIZATION), false, BossDeskPoint::onUse));
        NeoForge.EVENT_BUS.addListener(BossCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(BossMod::onLoggedOut);
    }

    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BossServer.forget(player.getUUID());
    }
}
