package dev.qrm.neoforge.server;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.player.PlayerService;
import dev.qrm.neoforge.net.ArchiveCharacterPayload;
import dev.qrm.neoforge.net.CharacterAction;
import dev.qrm.neoforge.net.CharacterResultPayload;
import dev.qrm.neoforge.net.CharacterRosterPayload;
import dev.qrm.neoforge.net.CreateCharacterPayload;
import dev.qrm.neoforge.net.ResultCode;
import dev.qrm.neoforge.net.RosterRequestPayload;
import dev.qrm.neoforge.net.SelectCharacterPayload;
import java.time.Clock;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Lato server della GUI: invio del roster all'ingresso e handler delle richieste del client. */
public final class GuiServer {
    private static final Logger LOG = LoggerFactory.getLogger("qrm");
    private static final RateLimiter LIMITER = new RateLimiter(250, Clock.systemUTC());

    private GuiServer() {}

    private static CharacterActions actions() {
        return new CharacterActions(QRM.get(CharacterService.class), QRM.get(PlayerService.class),
                QRM.get(JobService.class), QRM.get(JobRegistry.class), Clock.systemUTC());
    }

    private static PlayerId idOf(ServerPlayer player) {
        return new PlayerId(player.getUUID());
    }

    private static CharacterRosterPayload roster(CharacterActions.Snapshot s, boolean openScreen) {
        return new CharacterRosterPayload(s.cards(), s.slots(), s.activeId(), openScreen);
    }

    /** All'ingresso: se il client ha il canale QRM, invia il roster e apre la schermata se manca un personaggio attivo. */
    public static void onPlayerJoin(ServerPlayer player) {
        if (!player.connection.hasChannel(CharacterRosterPayload.TYPE)) return;
        CharacterActions.Snapshot s = actions().snapshot(idOf(player));
        PacketDistributor.sendToPlayer(player, roster(s, s.activeId().isEmpty()));
    }

    public static void onPlayerLeave(ServerPlayer player) {
        LIMITER.forget(player.getUUID());
    }

    public static void onRosterRequest(RosterRequestPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        handle(player, CharacterAction.ROSTER, true, a -> new CharacterActions.Outcome(ResultCode.OK, a.snapshot(idOf(player))));
    }

    public static void onSelect(SelectCharacterPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        handle(player, CharacterAction.SELECT, false, a -> a.select(idOf(player), payload.id()));
    }

    public static void onCreate(CreateCharacterPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        handle(player, CharacterAction.CREATE, false, a -> a.create(idOf(player), payload.firstName(),
                payload.lastName(), payload.year(), payload.month(), payload.day()));
    }

    public static void onArchive(ArchiveCharacterPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        handle(player, CharacterAction.ARCHIVE, false, a -> a.archive(idOf(player), payload.id()));
    }

    private static void handle(ServerPlayer player, CharacterAction action, boolean openScreen,
            Function<CharacterActions, CharacterActions.Outcome> operation) {
        if (!LIMITER.tryAcquire(player.getUUID())) {
            PacketDistributor.sendToPlayer(player, new CharacterResultPayload(action, ResultCode.RATE_LIMITED));
            return;
        }
        try {
            CharacterActions.Outcome outcome = operation.apply(actions());
            PacketDistributor.sendToPlayer(player, new CharacterResultPayload(action, outcome.code()),
                    roster(outcome.snapshot(), openScreen));
        } catch (RuntimeException e) {
            LOG.error("QRM GUI action {} failed for {}", action, player.getUUID(), e);
            PacketDistributor.sendToPlayer(player, new CharacterResultPayload(action, ResultCode.ERROR));
        }
    }
}
