package dev.qrm.neoforge.server;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterArchivedEvent;
import dev.qrm.api.character.CharacterSelectedEvent;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.DutyChangedEvent;
import dev.qrm.api.job.JobChangedEvent;
import dev.qrm.api.org.MembershipChangedEvent;
import dev.qrm.api.org.OrganizationArchivedEvent;
import dev.qrm.api.point.PointChangedEvent;
import dev.qrm.api.point.PointCreatedEvent;
import dev.qrm.api.point.PointRemovedEvent;
import dev.qrm.api.point.PointService;
import dev.qrm.api.point.PointTypeRegistry;
import dev.qrm.neoforge.net.PointsPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tiene aggiornato l'elenco dei punti visibili a ogni giocatore: gli eventi del Core segnano i giocatori interessati,
 * a fine tick si invia al massimo un elenco per giocatore. Il filtro è sul server: il client non riceve mai i punti
 * che non deve vedere.
 */
public final class PointSync {
    private static final Logger LOG = LoggerFactory.getLogger("qrm");
    private static final String OWNER = "qrm-points";
    private static final StatusDirtyTracker TRACKER = new StatusDirtyTracker();

    private PointSync() {}

    /** Da chiamare una sola volta, nel costruttore del mod: gli handler leggono i servizi solo quando scattano. */
    public static void register(EventBus bus) {
        bus.register(CharacterSelectedEvent.class, OWNER, e -> TRACKER.markPlayer(e.character().playerId().value()));
        bus.register(CharacterArchivedEvent.class, OWNER, e -> TRACKER.markPlayer(e.character().playerId().value()));
        bus.register(JobChangedEvent.class, OWNER, e -> TRACKER.markCharacter(e.character().value()));
        bus.register(DutyChangedEvent.class, OWNER, e -> TRACKER.markCharacter(e.character().value()));
        bus.register(MembershipChangedEvent.class, OWNER, e -> TRACKER.markCharacter(e.character().value()));
        bus.register(OrganizationArchivedEvent.class, OWNER, e -> TRACKER.markAll(onlinePlayerIds()));
        bus.register(PointCreatedEvent.class, OWNER, e -> TRACKER.markAll(onlinePlayerIds()));
        bus.register(PointChangedEvent.class, OWNER, e -> TRACKER.markAll(onlinePlayerIds()));
        bus.register(PointRemovedEvent.class, OWNER, e -> TRACKER.markAll(onlinePlayerIds()));
        NeoForge.EVENT_BUS.addListener(PointSync::onServerTick);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        TRACKER.markPlayer(player.getUUID());
    }

    public static void onPlayerLeave(ServerPlayer player) {
        TRACKER.forget(player.getUUID());
    }

    private static List<UUID> onlinePlayerIds() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        List<UUID> ids = new ArrayList<>();
        if (server != null) server.getPlayerList().getPlayers().forEach(p -> ids.add(p.getUUID()));
        return ids;
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        Set<UUID> due = TRACKER.drain();
        if (due.isEmpty()) return;
        for (UUID id : due) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(id);
            if (player != null) send(player);
        }
    }

    private static void send(ServerPlayer player) {
        if (!player.connection.hasChannel(PointsPayload.TYPE)) return;
        try {
            Optional<CharacterId> character = QRM.get(CharacterService.class).activeOf(new PlayerId(player.getUUID()))
                    .map(c -> c.id());
            TRACKER.setActiveCharacter(player.getUUID(), character.map(CharacterId::value).orElse(null));
            PacketDistributor.sendToPlayer(player, new PointsPayload(PointViews.visibleTo(
                    QRM.get(PointService.class), QRM.get(PointTypeRegistry.class), character, PointWorld::active)));
        } catch (RuntimeException e) {
            LOG.error("Could not send the interaction points to {}", player.getUUID(), e);
        }
    }
}
