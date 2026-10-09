package dev.qrm.neoforge.server;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterArchivedEvent;
import dev.qrm.api.character.CharacterSelectedEvent;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.economy.Account;
import dev.qrm.api.economy.AccountService;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.economy.Transaction;
import dev.qrm.api.economy.TransactionPostEvent;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.id.AccountId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.DutyChangedEvent;
import dev.qrm.api.job.JobChangedEvent;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.org.MembershipChangedEvent;
import dev.qrm.api.org.OrganizationArchivedEvent;
import dev.qrm.api.org.OrganizationService;
import dev.qrm.neoforge.QrmConfig;
import dev.qrm.neoforge.net.PlayerStatus;
import dev.qrm.neoforge.net.PlayerStatusPayload;
import dev.qrm.neoforge.net.SetDutyPayload;
import dev.qrm.neoforge.net.StatusRequestPayload;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tiene aggiornato lo stato mostrato da HUD e hub: gli eventi del Core segnano i giocatori interessati, a fine tick
 * si invia al massimo uno snapshot per giocatore.
 */
public final class StatusSync {
    private static final Logger LOG = LoggerFactory.getLogger("qrm");
    private static final String OWNER = "qrm-status";
    private static final StatusDirtyTracker TRACKER = new StatusDirtyTracker();
    private static final RateLimiter DUTY_LIMITER = new RateLimiter(250, Clock.systemUTC());
    private static final RateLimiter LIMITER = new RateLimiter(250, Clock.systemUTC());

    private StatusSync() {}

    /** Da chiamare una sola volta, nel costruttore del mod: gli handler leggono i servizi solo quando scattano. */
    public static void register(EventBus bus) {
        bus.register(CharacterSelectedEvent.class, OWNER, e -> TRACKER.markPlayer(e.character().playerId().value()));
        bus.register(CharacterArchivedEvent.class, OWNER, e -> TRACKER.markPlayer(e.character().playerId().value()));
        bus.register(JobChangedEvent.class, OWNER, e -> TRACKER.markCharacter(e.character().value()));
        bus.register(DutyChangedEvent.class, OWNER, e -> TRACKER.markCharacter(e.character().value()));
        bus.register(MembershipChangedEvent.class, OWNER, e -> TRACKER.markCharacter(e.character().value()));
        bus.register(OrganizationArchivedEvent.class, OWNER, e -> TRACKER.markAll(onlinePlayerIds()));
        bus.register(TransactionPostEvent.class, OWNER, e -> markOwners(e.transaction()));
        NeoForge.EVENT_BUS.addListener(StatusSync::onServerTick);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        TRACKER.markPlayer(player.getUUID());
    }

    public static void onPlayerLeave(ServerPlayer player) {
        TRACKER.forget(player.getUUID());
        LIMITER.forget(player.getUUID());
        DUTY_LIMITER.forget(player.getUUID());
    }

    /** Il giocatore chiede di cambiare servizio dalla GUI: la regola è del server; poi si riallinea la GUI. */
    public static void onSetDuty(SetDutyPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (!DUTY_LIMITER.tryAcquire(player.getUUID())) return;
        try {
            DutyActions.setDuty(QRM.get(CharacterService.class), QRM.get(JobService.class),
                    new PlayerId(player.getUUID()), payload.onDuty(), QrmConfig.uiConfig().selfDuty());
        } catch (RuntimeException e) {
            LOG.error("Could not change the duty of {}", player.getUUID(), e);
        }
        send(player);
    }

    /** Apertura dell'hub: risposta immediata, con il limite di frequenza delle altre richieste della GUI. */
    public static void onRequest(StatusRequestPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (!LIMITER.tryAcquire(player.getUUID())) return;
        send(player);
    }

    private static void markOwners(Transaction t) {
        AccountService accounts = QRM.get(AccountService.class);
        for (AccountId id : new AccountId[] {t.from(), t.to()}) {
            accounts.find(id).map(Account::owner).filter(o -> OwnerRef.CHARACTER.equals(o.type()))
                    .ifPresent(o -> TRACKER.markCharacter(o.id()));
        }
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

    private static PlayerStatusBuilder builder() {
        return new PlayerStatusBuilder(QRM.get(CharacterService.class), QRM.get(AccountService.class),
                QRM.get(CurrencyRegistry.class), QRM.get(JobService.class), QRM.get(JobRegistry.class),
                QRM.get(OrganizationService.class), QrmConfig.DEFAULT_CURRENCY::get, QrmConfig::uiConfig);
    }

    private static void send(ServerPlayer player) {
        if (!player.connection.hasChannel(PlayerStatusPayload.TYPE)) return;
        try {
            PlayerStatusBuilder builder = builder();
            PlayerId id = new PlayerId(player.getUUID());
            PlayerStatus status = builder.build(id);
            TRACKER.setActiveCharacter(player.getUUID(), builder.activeCharacter(id).map(c -> c.value()).orElse(null));
            PacketDistributor.sendToPlayer(player, new PlayerStatusPayload(status));
        } catch (RuntimeException e) {
            LOG.error("Could not send the QRM status to {}", player.getUUID(), e);
        }
    }
}
