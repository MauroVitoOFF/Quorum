package dev.qrm.neoforge.server;

import dev.qrm.api.staff.StaffLogEntry;
import dev.qrm.neoforge.net.PlayerAction;
import dev.qrm.neoforge.net.PlayerActionPayload;
import dev.qrm.neoforge.net.PlayerActionResultPayload;
import dev.qrm.neoforge.net.PlayerInfoLine;
import dev.qrm.neoforge.net.PlayerResult;
import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lato server delle azioni della scheda giocatore: a ogni pacchetto ricontrolla il nodo {@code staff.player.<azione>}, applica
 * il limite di frequenza, verifica che il bersaglio sia online e scrive nel log chi ha fatto cosa. L'esecuzione di ogni azione
 * è un {@link Handler} registrato in {@link PlayerActionHandlers}.
 */
public final class PlayerActionServer {
    private static final Logger LOG = LoggerFactory.getLogger("qrm");
    private static final RateLimiter LIMITER = new RateLimiter(250, Clock.systemUTC());
    private static final Map<PlayerAction, Handler> HANDLERS = new EnumMap<>(PlayerAction.class);

    static {
        PlayerActionHandlers.install(HANDLERS);
    }

    /** Esito di un'azione: codice più, per quelle informative, le righe da mostrare. */
    public record Outcome(PlayerResult result, List<PlayerInfoLine> lines) {
        public Outcome {
            lines = List.copyOf(lines);
        }

        public static Outcome of(PlayerResult result) {
            return new Outcome(result, List.of());
        }
    }

    /** Un'azione già autorizzata: {@code staff} e {@code target} sono online, {@code arg} va ancora validato. */
    @FunctionalInterface
    public interface Handler {
        Outcome handle(ServerPlayer staff, ServerPlayer target, String arg);
    }

    private PlayerActionServer() {}

    public static void onPlayerLeave(ServerPlayer player) {
        LIMITER.forget(player.getUUID());
        PlayerActionHandlers.FROZEN.forget(player.getUUID());
    }

    /** Chiamato a ogni tick del giocatore: mantiene fermi i giocatori congelati. */
    public static void onPlayerTick(ServerPlayer player) {
        PlayerActionHandlers.enforceFreeze(player);
    }

    public static void onAction(PlayerActionPayload payload, IPayloadContext context) {
        ServerPlayer staff = (ServerPlayer) context.player();
        PlayerAction action = payload.action();
        if (!StaffAccess.has(staff, action.node())) {
            if (!action.readOnly()) {
                StaffAudit.recordPlayer(staff, "panel." + action.id(),
                        Optional.ofNullable(staff.level().getServer().getPlayerList().getPlayer(payload.target())),
                        payload.target(), StaffAuditText.panelDetail(payload.arg()), StaffLogEntry.Outcome.DENIED);
            }
            reply(staff, action, payload, Outcome.of(PlayerResult.NO_PERMISSION));
            return;
        }
        if (!LIMITER.tryAcquire(staff.getUUID())) {
            reply(staff, action, payload, Outcome.of(PlayerResult.RATE_LIMITED));
            return;
        }
        ServerPlayer target = staff.level().getServer().getPlayerList().getPlayer(payload.target());
        if (target == null) {
            reply(staff, action, payload, Outcome.of(PlayerResult.NO_TARGET));
            return;
        }
        if (!action.readOnly() && !StaffAccess.mayTarget(StaffAccess.isOp(staff), StaffAccess.isOp(target))) {
            StaffAudit.recordPlayer(staff, "panel." + action.id(), Optional.of(target), payload.target(),
                    StaffAuditText.panelDetail(payload.arg()), StaffLogEntry.Outcome.DENIED);
            reply(staff, action, payload, Outcome.of(PlayerResult.NO_PERMISSION));
            return;
        }
        Handler handler = HANDLERS.get(action);
        Outcome outcome;
        if (handler == null) {
            outcome = Outcome.of(PlayerResult.INVALID);
        } else {
            try {
                outcome = handler.handle(staff, target, payload.arg());
            } catch (RuntimeException e) {
                LOG.error("Staff action {} failed for {}", action, staff.getUUID(), e);
                outcome = Outcome.of(PlayerResult.ERROR);
            }
        }
        boolean changed = switch (outcome.result()) {
            case OK, FROZEN, UNFROZEN -> true;
            default -> false;
        };
        if (changed && !action.readOnly()) {
            StaffAudit.recordPlayer(staff, "panel." + action.id(), Optional.of(target), payload.target(),
                    StaffAuditText.panelDetail(payload.arg()), StaffLogEntry.Outcome.OK);
        }
        LOG.info("Staff {} ran {} target={} arg='{}' -> {}", staff.getGameProfile().name(), action,
                target.getGameProfile().name(), payload.arg().replace('\n', '|'), outcome.result());
        reply(staff, action, payload, outcome);
    }

    private static void reply(ServerPlayer staff, PlayerAction action, PlayerActionPayload payload, Outcome outcome) {
        PacketDistributor.sendToPlayer(staff,
                new PlayerActionResultPayload(action, outcome.result(), payload.target(), outcome.lines()));
    }
}
