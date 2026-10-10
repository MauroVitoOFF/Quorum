package dev.qrm.neoforge.server;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.point.PointPosition;
import dev.qrm.api.point.PointService;
import dev.qrm.api.point.PointTypeRegistry;
import java.time.Clock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Click destro sui punti e protezione del blocco marcato (solo lato server). */
public final class PointEvents {
    private static final Logger LOG = LoggerFactory.getLogger("qrm");
    private static final RateLimiter LIMITER = new RateLimiter(250, Clock.systemUTC());

    private PointEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PointEvents::onRightClick);
        NeoForge.EVENT_BUS.addListener(PointEvents::onBreak);
    }

    public static void onPlayerLeave(ServerPlayer player) {
        LIMITER.forget(player.getUUID());
    }

    private static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            PointPosition position = PointWorld.positionOf(event.getLevel(), event.getPos());
            PointService points = QRM.get(PointService.class);
            if (event.getHand() != InteractionHand.MAIN_HAND) {
                // il click va gestito una sola volta: la mano secondaria non fa nulla su un blocco marcato
                if (points.findAt(position).filter(PointWorld::active).isPresent()) cancel(event);
                return;
            }
            if (!LIMITER.tryAcquire(player.getUUID())) {
                if (points.findAt(position).filter(PointWorld::active).isPresent()) cancel(event);
                return;
            }
            PointInteraction.Outcome outcome = PointInteraction.use(points, QRM.get(PointTypeRegistry.class),
                    QRM.get(CharacterService.class), new PlayerId(player.getUUID()), position, PointWorld::active);
            switch (outcome) {
                case NOT_A_POINT -> { }
                case NO_CHARACTER -> {
                    cancel(event);
                    player.sendSystemMessage(Component.literal("Nessun personaggio attivo."));
                }
                case DENIED -> {
                    cancel(event);
                    player.sendSystemMessage(Component.literal("Non puoi usare questo punto."));
                }
                case USED -> cancel(event);
            }
        } catch (RuntimeException e) {
            // un handler che fallisce non deve far passare il click al blocco: il punto resta protetto
            LOG.error("Interaction point failed for {}", player.getUUID(), e);
            cancel(event);
            player.sendSystemMessage(Component.literal("Errore interno: contatta un amministratore."));
        }
    }

    private static void cancel(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void onBreak(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        try {
            PointPosition position = PointWorld.positionOf(player.level(), event.getPos());
            var point = QRM.get(PointService.class).findAt(position);
            if (point.isEmpty()) return;
            event.setCanceled(true);
            event.setNotifyClient(true);
            player.sendSystemMessage(Component.literal("Questo blocco è il punto #" + point.get().id()
                    + ": rimuovilo prima con /qrm admin point remove."));
        } catch (RuntimeException e) {
            LOG.error("Could not check the interaction point at a broken block", e);
        }
    }
}
