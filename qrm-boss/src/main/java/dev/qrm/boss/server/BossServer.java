package dev.qrm.boss.server;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.AccountService;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.economy.TransactionService;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.org.OrganizationBankService;
import dev.qrm.api.org.OrganizationService;
import dev.qrm.boss.logic.ActionLimiter;
import dev.qrm.boss.logic.BossAction;
import dev.qrm.boss.logic.BossReply;
import dev.qrm.boss.logic.BossRequest;
import dev.qrm.boss.logic.BossResult;
import dev.qrm.boss.logic.BossService;
import dev.qrm.boss.logic.BossSnapshot;
import dev.qrm.boss.net.BossActionPayload;
import dev.qrm.boss.net.BossOrgListPayload;
import dev.qrm.boss.net.BossStatePayload;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lato server del menu boss: apre il menu e applica le richieste del client. Ogni richiesta ricontrolla Character
 * attivo, azienda e nodi tramite {@link BossService}; il client mostra solo l'ultimo snapshot ricevuto.
 */
public final class BossServer {
    private static final Logger LOG = LoggerFactory.getLogger("qrm_boss");
    private static final long MIN_INTERVAL_MS = 250;
    private static final ActionLimiter LIMITER = new ActionLimiter(Clock.systemUTC());

    private BossServer() {}

    static BossService service() {
        return new BossService(QRM.get(OrganizationService.class), QRM.get(OrganizationBankService.class),
                QRM.get(AccountService.class), QRM.get(TransactionService.class), QRM.get(CharacterService.class),
                QRM.get(CurrencyRegistry.class), QRM.get(JobService.class), QRM.get(JobRegistry.class));
    }

    public static void forget(UUID player) {
        LIMITER.forget(player);
    }

    static Optional<PlayerCharacter> activeCharacter(ServerPlayer player) {
        return QRM.get(CharacterService.class).activeOf(new PlayerId(player.getUUID()));
    }

    /** I Character attivi dei giocatori online: i candidati da aggiungere. */
    private static List<PlayerCharacter> online(ServerPlayer player) {
        List<PlayerCharacter> list = new ArrayList<>();
        for (ServerPlayer p : player.level().getServer().getPlayerList().getPlayers()) {
            activeCharacter(p).ifPresent(list::add);
        }
        return list;
    }

    private static void send(ServerPlayer player, BossResult result, Optional<BossSnapshot> snapshot) {
        PacketDistributor.sendToPlayer(player, new BossStatePayload(new BossReply(result, snapshot)));
    }

    /** Apre il menu dell'azienda per il Character attivo del giocatore. @return false se non può aprirlo */
    public static boolean open(ServerPlayer player, CharacterId character, String orgId) {
        Optional<BossSnapshot> snapshot = service().snapshot(character, orgId, online(player));
        if (snapshot.isEmpty()) return false;
        send(player, BossResult.OK, snapshot);
        return true;
    }

    /** Invia l'elenco di aziende fra cui scegliere. */
    static void openPicker(ServerPlayer player, CharacterId character, List<BossSnapshot.OrgChoice> orgs) {
        PacketDistributor.sendToPlayer(player,
                new BossOrgListPayload(new BossReply.OrgList(character.value(), List.copyOf(orgs))));
    }

    public static void onAction(BossActionPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        BossRequest request = payload.request();
        Optional<PlayerCharacter> active = activeCharacter(player);
        if (active.isEmpty()) {
            send(player, BossResult.NO_CHARACTER, Optional.empty());
            return;
        }
        CharacterId character = active.get().id();
        if (!character.value().equals(request.character())) {
            send(player, BossResult.STALE_CHARACTER, Optional.empty());
            return;
        }
        if (!LIMITER.tryAcquire(player.getUUID(), MIN_INTERVAL_MS)) {
            send(player, BossResult.RATE_LIMITED, Optional.empty());
            return;
        }
        BossResult result;
        try {
            result = run(player, character, request);
        } catch (RuntimeException e) {
            LOG.error("Boss action {} failed for {}", request.action(), player.getUUID(), e);
            result = BossResult.ERROR;
        }
        Optional<BossSnapshot> snapshot;
        try {
            snapshot = service().snapshot(character, request.orgId(), online(player));
        } catch (RuntimeException e) {
            LOG.error("Boss snapshot failed for {}", player.getUUID(), e);
            snapshot = Optional.empty();
        }
        if (snapshot.isEmpty() && !result.closesScreen()) result = BossResult.NO_PERMISSION;
        send(player, result, snapshot);
    }

    private static BossResult run(ServerPlayer player, CharacterId character, BossRequest r) {
        BossService service = service();
        if (r.action() == BossAction.REFRESH) return BossResult.OK;
        if (r.action() == BossAction.DEPOSIT) return service.deposit(character, r.orgId(), r.amount(), r.requestId());
        if (r.action() == BossAction.WITHDRAW) return service.withdraw(character, r.orgId(), r.amount(), r.requestId());

        if (r.target().isEmpty()) return BossResult.NO_TARGET;
        CharacterId target = new CharacterId(r.target().get());
        return switch (r.action()) {
            case ADD_MEMBER -> {
                // Solo i Character attivi dei giocatori online sono candidati.
                boolean isCandidate = online(player).stream().anyMatch(pc -> pc.id().equals(target));
                yield isCandidate ? service.addMember(character, r.orgId(), target) : BossResult.NO_TARGET;
            }
            case SET_RANK -> service.setRank(character, r.orgId(), target, r.rankId());
            case REMOVE_MEMBER -> service.removeMember(character, r.orgId(), target);
            default -> BossResult.ERROR;
        };
    }
}
