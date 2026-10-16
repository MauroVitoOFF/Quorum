package dev.qrm.boss.server;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.data.ModDataService;
import dev.qrm.api.economy.AccountService;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.economy.TransactionService;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.org.OrganizationService;
import dev.qrm.boss.logic.ActionLimiter;
import dev.qrm.boss.logic.BossAction;
import dev.qrm.boss.logic.BossPerks;
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
import net.minecraft.network.chat.Component;
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
        return new BossService(QRM.get(OrganizationService.class), QRM.get(AccountService.class),
                QRM.get(TransactionService.class), QRM.get(CharacterService.class), QRM.get(CurrencyRegistry.class),
                QRM.get(JobService.class), QRM.get(JobRegistry.class),
                new BossPerks(QRM.get(ModDataService.class), Clock.systemUTC()));
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
        if (r.action() == BossAction.SET_NOTICE) {
            BossResult result = service.setNotice(character, r.orgId(), r.text());
            if (result == BossResult.OK) announceNotice(player, service, character, r);
            return result;
        }

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
            case SET_TITLE -> service.setTitle(character, r.orgId(), target, r.text());
            case GIVE_BONUS -> {
                BossResult result = service.giveBonus(character, r.orgId(), target, r.amount(), r.text(), r.requestId());
                if (result == BossResult.OK) notifyBonus(player, service, character, target, r);
                yield result;
            }
            default -> BossResult.ERROR;
        };
    }

    /** Avvisa in chat i membri online che c'è un nuovo avviso (non quando viene tolto). */
    private static void announceNotice(ServerPlayer author, BossService service, CharacterId viewer, BossRequest r) {
        String text = BossPerks.clean(r.text(), BossPerks.MAX_NOTICE);
        if (text.isEmpty()) return;
        String org = QRM.get(OrganizationService.class).find(r.orgId()).map(o -> o.label()).orElse(r.orgId());
        String name = QRM.get(CharacterService.class).find(viewer)
                .map(c -> c.firstName() + " " + c.lastName()).orElse("?");
        List<CharacterId> members = service.membersOf(r.orgId());
        for (ServerPlayer p : author.level().getServer().getPlayerList().getPlayers()) {
            Optional<PlayerCharacter> pc = activeCharacter(p);
            if (pc.isPresent() && !pc.get().id().equals(viewer) && members.contains(pc.get().id())) {
                p.sendSystemMessage(Component.translatable("qrm_boss.msg.notice", org, name, text));
            }
        }
    }

    /** Il dipendente online riceve un messaggio col motivo; se è offline lo trova nello storico della banca. */
    private static void notifyBonus(ServerPlayer author, BossService service, CharacterId viewer, CharacterId target,
            BossRequest r) {
        String amount = service.formatBonus(r.amount()).orElse(r.amount());
        String org = QRM.get(OrganizationService.class).find(r.orgId()).map(o -> o.label()).orElse(r.orgId());
        String reason = BossPerks.clean(r.text(), BossPerks.MAX_REASON);
        for (ServerPlayer p : author.level().getServer().getPlayerList().getPlayers()) {
            Optional<PlayerCharacter> pc = activeCharacter(p);
            if (pc.isPresent() && pc.get().id().equals(target)) {
                p.sendSystemMessage(reason.isEmpty()
                        ? Component.translatable("qrm_boss.msg.bonus", org, amount)
                        : Component.translatable("qrm_boss.msg.bonus_reason", org, amount, reason));
            }
        }
    }
}
