package dev.qrm.neoforge.server;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.moderation.ModerationService;
import dev.qrm.api.org.OrganizationService;
import dev.qrm.api.staff.StaffNodes;
import dev.qrm.neoforge.net.PanelRequestPayload;
import dev.qrm.neoforge.net.PanelSnapshotPayload;
import java.time.Clock;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Lato server dell'elenco giocatori del pannello staff: permesso ricontrollato a ogni richiesta, con limite di frequenza. */
public final class PanelServer {
    private static final Logger LOG = LoggerFactory.getLogger("qrm");
    private static final RateLimiter LIMITER = new RateLimiter(250, Clock.systemUTC());

    private PanelServer() {}

    public static void onPlayerLeave(ServerPlayer player) {
        LIMITER.forget(player.getUUID());
    }

    public static void onRequest(PanelRequestPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (!StaffAccess.has(player, StaffNodes.PANEL)) return;
        if (!LIMITER.tryAcquire(player.getUUID())) return;
        try {
            StaffAccess.sync(player);
            List<PanelSnapshots.Online> online = player.level().getServer().getPlayerList().getPlayers().stream()
                    .map(p -> new PanelSnapshots.Online(p.getUUID(), p.getGameProfile().name())).toList();
            PacketDistributor.sendToPlayer(player, new PanelSnapshotPayload(PanelSnapshots.build(online,
                    QRM.get(CharacterService.class), QRM.get(JobService.class), QRM.get(JobRegistry.class),
                    QRM.get(OrganizationService.class), QRM.get(ModerationService.class))));
        } catch (RuntimeException e) {
            LOG.error("Panel snapshot failed for {}", player.getUUID(), e);
        }
    }
}
