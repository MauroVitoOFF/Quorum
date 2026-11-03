package dev.qrm.immersion.mc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Gli eventi di gioco del modulo: mani alzate, presentazioni, bussare. */
final class ImmersionEvents {
    private ImmersionEvents() {}

    static void register() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(ImmersionEvents::onTick);
        bus.addListener(ImmersionEvents::onStartTracking);
        bus.addListener(ImmersionEvents::onLoggedIn);
        bus.addListener(ImmersionEvents::onEntityLeave);
        // Chi ha le mani alzate non agisce: i blocchi vengono prima di ogni altro gestore.
        bus.addListener(EventPriority.HIGHEST, ImmersionEvents::onRightClickItem);
        bus.addListener(EventPriority.HIGHEST, ImmersionEvents::onRightClickBlockBlocked);
        bus.addListener(EventPriority.HIGHEST, ImmersionEvents::onLeftClickBlockBlocked);
        bus.addListener(EventPriority.HIGHEST, ImmersionEvents::onAttack);
        bus.addListener(EventPriority.HIGHEST, ImmersionEvents::onBreak);
        bus.addListener(EventPriority.HIGHEST, ImmersionEvents::onPlace);
        bus.addListener(EventPriority.HIGHEST, ImmersionEvents::onUseStart);
        bus.addListener(EventPriority.HIGHEST, ImmersionEvents::onIncomingDamage);
        bus.addListener(EventPriority.HIGHEST, ImmersionEvents::onEntityInteractBlocked);
        // La chat locale viene dopo chi la blocca (silenziati): un messaggio gia' annullato non arriva.
        bus.addListener(EventPriority.LOW, ImmersionEvents::onChat);
        // Le azioni del modulo: dopo i blocchi, prima del resto.
        bus.addListener(EventPriority.HIGH, ImmersionEvents::onEntityInteract);
        bus.addListener(EventPriority.HIGH, ImmersionEvents::onLeftClickBlock);
    }

    // ---- ciclo di vita ----

    private static void onTick(ServerTickEvent.Post e) {
        ImmersionServer.tick(e.getServer());
    }

    private static void onStartTracking(PlayerEvent.StartTracking e) {
        if (e.getEntity() instanceof ServerPlayer viewer) ImmersionServer.onStartTracking(viewer, e.getTarget());
    }

    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        // Chi entra deve ricevere le etichette, e gli altri devono ricevere la sua.
        if (e.getEntity() instanceof ServerPlayer p) ImmersionServer.sendSettings(p);
        if (e.getEntity() instanceof ServerPlayer) ImmersionServer.resyncAll();
    }

    private static void onEntityLeave(EntityLeaveLevelEvent e) {
        if (!e.getLevel().isClientSide() && !(e.getEntity() instanceof Player)) ImmersionServer.forgetEntity(e.getEntity());
    }

    private static void onChat(ServerChatEvent e) {
        if (!ImmersionConfig.LOCAL_CHAT.get()) return;
        e.setCanceled(true);
        ImmersionServer.localChat(e.getPlayer(), e.getMessage());
    }

    // ---- azioni vietate a chi ha le mani alzate ----

    private static boolean handsUp(Player p) {
        return !p.level().isClientSide() && ImmersionServer.handsUp(p);
    }

    private static void block(PlayerInteractEvent e) {
        if (!handsUp(e.getEntity()) || !(e instanceof ICancellableEvent c)) return;
        c.setCanceled(true);
        if (e instanceof PlayerInteractEvent.RightClickItem r) r.setCancellationResult(InteractionResult.FAIL);
        else if (e instanceof PlayerInteractEvent.RightClickBlock r) r.setCancellationResult(InteractionResult.FAIL);
        else if (e instanceof PlayerInteractEvent.EntityInteract r) r.setCancellationResult(InteractionResult.FAIL);
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        block(e);
    }

    private static void onRightClickBlockBlocked(PlayerInteractEvent.RightClickBlock e) {
        block(e);
    }

    private static void onLeftClickBlockBlocked(PlayerInteractEvent.LeftClickBlock e) {
        block(e);
    }

    private static void onEntityInteractBlocked(PlayerInteractEvent.EntityInteract e) {
        block(e);
    }

    private static void onAttack(AttackEntityEvent e) {
        if (handsUp(e.getEntity())) e.setCanceled(true);
    }

    private static void onBreak(BreakBlockEvent e) {
        if (handsUp(e.getPlayer())) e.setCanceled(true);
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof Player p && handsUp(p)) e.setCanceled(true);
    }

    private static void onUseStart(LivingEntityUseItemEvent.Start e) {
        if (e.getEntity() instanceof Player p && handsUp(p)) e.setCanceled(true);
    }

    /** Anche un colpo che parte da altre vie (proiettili, mod) non passa: chi si arrende non fa male. */
    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (e.getSource().getEntity() instanceof Player p && p != e.getEntity() && handsUp(p)) e.setCanceled(true);
    }

    // ---- presentarsi ----

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(e.getEntity() instanceof ServerPlayer a) || !a.isShiftKeyDown() || !a.getMainHandItem().isEmpty()) return;
        if (ImmersionServer.handshake(a, e.getTarget())) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS_SERVER);
        }
    }

    // ---- bussare ----

    /**
     * Clic sinistro a mani vuote su una porta. In creativa si bussa solo con Shift, cosi' non si rompe la porta
     * mentre si prova; Shift + clic e' la bussata forte, quella di chi sfonda.
     */
    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e) {
        if (e.getLevel().isClientSide() || e.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getLevel() instanceof ServerLevel level)) return;
        BlockState state = level.getBlockState(e.getPos());
        if (!(state.getBlock() instanceof DoorBlock door) || !p.getMainHandItem().isEmpty()) return;
        boolean shift = p.isShiftKeyDown();
        if (p.isCreative() && !shift) return;
        e.setCanceled(true);
        boolean iron = !state.is(net.minecraft.tags.BlockTags.WOODEN_DOORS);
        ImmersionServer.knock(p, level, e.getPos(), shift, iron);
    }
}
