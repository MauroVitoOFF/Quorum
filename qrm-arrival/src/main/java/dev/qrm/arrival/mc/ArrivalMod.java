package dev.qrm.arrival.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterSelectedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Primo arrivo: un Character nuovo si risveglia a terra in un vicolo della città. */
@Mod(ArrivalMod.MOD_ID)
public final class ArrivalMod {
    public static final String MOD_ID = "qrm_arrival";
    static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public ArrivalMod(IEventBus modBus) {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(ArrivalCommand::onRegisterCommands);
        bus.addListener(ArrivalMod::onPlayerTick);
        bus.addListener(ArrivalMod::onLoggedOut);
        bus.addListener(ArrivalMod::onDeath);
        bus.addListener(ArrivalMod::onServerStopped);
        // Chi si sta risvegliando non agisce: i blocchi vengono prima di ogni altro gestore.
        bus.addListener(EventPriority.HIGHEST, ArrivalMod::onInteract);
        bus.addListener(EventPriority.HIGHEST, ArrivalMod::onAttack);
        bus.addListener(EventPriority.HIGHEST, ArrivalMod::onBreak);
        bus.addListener(EventPriority.HIGHEST, ArrivalMod::onPlace);
        bus.addListener(EventPriority.HIGHEST, ArrivalMod::onUseStart);
        QRM.events().register(CharacterSelectedEvent.class, MOD_ID, e -> ArrivalServer.onSelected(e.character()));
    }

    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p) ArrivalServer.onTick(p);
    }

    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent e) {
        ArrivalServer.forget(e.getEntity().getUUID());
    }

    private static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) ArrivalServer.forget(p.getUUID());
    }

    private static void onServerStopped(ServerStoppedEvent e) {
        ArrivalServer.reset();
    }

    private static void onInteract(PlayerInteractEvent e) {
        if (!ArrivalServer.locked(e.getEntity()) || !(e instanceof ICancellableEvent c)) return;
        c.setCanceled(true);
        if (e instanceof PlayerInteractEvent.RightClickItem r) r.setCancellationResult(InteractionResult.FAIL);
        else if (e instanceof PlayerInteractEvent.RightClickBlock r) r.setCancellationResult(InteractionResult.FAIL);
        else if (e instanceof PlayerInteractEvent.EntityInteract r) r.setCancellationResult(InteractionResult.FAIL);
    }

    private static void onAttack(AttackEntityEvent e) {
        if (ArrivalServer.locked(e.getEntity())) e.setCanceled(true);
    }

    private static void onBreak(BreakBlockEvent e) {
        if (ArrivalServer.locked(e.getPlayer())) e.setCanceled(true);
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof net.minecraft.world.entity.player.Player p && ArrivalServer.locked(p)) e.setCanceled(true);
    }

    private static void onUseStart(LivingEntityUseItemEvent.Start e) {
        if (e.getEntity() instanceof net.minecraft.world.entity.player.Player p && ArrivalServer.locked(p)) e.setCanceled(true);
    }
}
