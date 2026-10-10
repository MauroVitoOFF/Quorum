package dev.qrm.neoforge;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.DutyReason;
import dev.qrm.api.job.JobService;
import dev.qrm.api.player.PlayerJoinEvent;
import dev.qrm.api.player.PlayerLeaveEvent;
import dev.qrm.api.player.PlayerService;
import dev.qrm.neoforge.server.GuiServer;
import dev.qrm.neoforge.server.StatusSync;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PlayerEvents {
    private static final Logger LOG = LoggerFactory.getLogger("qrm");

    private PlayerEvents() {}

    static PlayerId idOf(ServerPlayer player) {
        return new PlayerId(player.getUUID());
    }

    static void register(EventBus bus) {
        bus.register(dev.qrm.api.moderation.MuteChangedEvent.class, "qrm",
                dev.qrm.neoforge.server.ModerationEvents::onMuteChanged);
        NeoForge.EVENT_BUS.addListener(dev.qrm.neoforge.server.ModerationEvents::onChat);
        NeoForge.EVENT_BUS.addListener(dev.qrm.neoforge.server.ModerationEvents::onCommand);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) {
                PlayerId id = idOf(sp);
                QRM.get(PlayerService.class).onJoin(id);
                bus.post(new PlayerJoinEvent(id));
                StatusSync.onPlayerJoin(sp);
                dev.qrm.neoforge.server.PointSync.onPlayerJoin(sp);
                try {
                    GuiServer.onPlayerJoin(sp);
                } catch (RuntimeException ex) {
                    LOG.error("Could not send the GUI roster to {}", id.value(), ex);
                }
                dev.qrm.neoforge.server.ModerationEvents.onLogin(sp);
                try {
                    dev.qrm.neoforge.server.StaffAccess.sync(sp);
                } catch (RuntimeException ex) {
                    LOG.error("Could not send the staff access to {}", id.value(), ex);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) dev.qrm.neoforge.server.PlayerActionServer.onPlayerTick(sp);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) {
                PlayerId id = idOf(sp);
                GuiServer.onPlayerLeave(sp);
                StatusSync.onPlayerLeave(sp);
                dev.qrm.neoforge.server.PointSync.onPlayerLeave(sp);
                dev.qrm.neoforge.server.PointEvents.onPlayerLeave(sp);
                dev.qrm.neoforge.server.PanelServer.onPlayerLeave(sp);
                dev.qrm.neoforge.server.PlayerActionServer.onPlayerLeave(sp);
                dev.qrm.neoforge.server.PageServer.onPlayerLeave(sp);
                dev.qrm.neoforge.server.ModerationEvents.onLogout(sp);
                try {
                    QRM.get(CharacterService.class).activeOf(id).ifPresent(pc ->
                            QRM.get(JobService.class).setOnDuty(pc.id(), false, DutyReason.LOGOUT));
                } catch (RuntimeException ex) {
                    LOG.error("Could not put {} off duty on logout", id.value(), ex);
                }
                bus.post(new PlayerLeaveEvent(id));
            }
        });
    }
}
