package dev.qrm.medical.mc;

import dev.qrm.medical.logic.InjuryRecord;
import dev.qrm.medical.logic.InjuryType;
import dev.qrm.medical.logic.MedicalState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Gli eventi di gioco che muovono il sistema: danno, morte, azioni vietate a chi è a terra, interazioni. */
final class MedicalEvents {
    private MedicalEvents() {}

    static void register() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(EventPriority.HIGH, MedicalEvents::onIncomingDamage);
        bus.addListener(EventPriority.LOWEST, MedicalEvents::onDamagePre);
        bus.addListener(EventPriority.LOWEST, MedicalEvents::onDeath);
        bus.addListener(MedicalEvents::onTick);
        // Chi è a terra non agisce: i blocchi vengono prima di ogni altro gestore.
        bus.addListener(EventPriority.HIGHEST, MedicalEvents::onRightClickItem);
        bus.addListener(EventPriority.HIGHEST, MedicalEvents::onRightClickBlock);
        bus.addListener(EventPriority.HIGHEST, MedicalEvents::onLeftClickBlock);
        bus.addListener(EventPriority.HIGHEST, MedicalEvents::onAttack);
        bus.addListener(EventPriority.HIGHEST, MedicalEvents::onBreak);
        bus.addListener(EventPriority.HIGHEST, MedicalEvents::onPlace);
        bus.addListener(EventPriority.HIGHEST, MedicalEvents::onToss);
        bus.addListener(EventPriority.HIGHEST, MedicalEvents::onUseStart);
        bus.addListener(EventPriority.HIGHEST, MedicalEvents::onEntityInteract);
        bus.addListener(MedicalEvents::onItemStacked);
    }

    // ---- danno e morte ----

    /** Un ferito a terra o svenuto non si danneggia (salvo /kill e vuoto); un colpo diretto lo fa svenire. */
    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !MedicalServer.incapacitated(p)) return;
        if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        e.setCanceled(true);
        Entity attacker = e.getSource().getEntity();
        if (attacker instanceof LivingEntity && attacker != p && MedicalServer.stateOf(p) == MedicalState.DOWNED) {
            MedicalServer.record(p, injury(e.getSource(), e.getAmount()));
            MedicalServer.criticalHit(p);
        }
    }

    /** Il colpo che sarebbe fatale non uccide: il giocatore cade a terra. */
    private static void onDamagePre(LivingDamageEvent.Pre e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isCreative() || p.isSpectator()) return;
        if (MedicalServer.stateOf(p) != MedicalState.HEALTHY) return;
        if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        float health = p.getHealth() + p.getAbsorptionAmount();
        if (e.getNewDamage() < health) {
            if (e.getNewDamage() >= 1.0F) MedicalServer.record(p, injury(e.getSource(), e.getNewDamage()));
            return;
        }
        InjuryRecord fatal = injury(e.getSource(), e.getNewDamage());
        e.setNewDamage(0.0F);
        MedicalServer.down(p, fatal);
    }

    /** Rete di sicurezza: una morte che non è passata dal danno (altri mod) cade comunque a terra. */
    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            MedicalServer.onRealDeath(p);
            return;
        }
        e.setCanceled(true);
        if (MedicalServer.stateOf(p) == MedicalState.HEALTHY) {
            MedicalServer.down(p, injury(e.getSource(), p.getMaxHealth()));
        } else {
            p.setHealth(1.0F);
        }
    }

    static InjuryRecord injury(DamageSource s, float amount) {
        return new InjuryRecord(classify(s), System.currentTimeMillis(), amount);
    }

    static InjuryType classify(DamageSource s) {
        if (s.is(DamageTypeTags.IS_FALL)) return InjuryType.FALL;
        if (s.is(DamageTypeTags.IS_DROWNING)) return InjuryType.DROWNING;
        if (s.is(DamageTypeTags.IS_FIRE)) return InjuryType.FIRE;
        if (s.is(DamageTypeTags.IS_EXPLOSION)) return InjuryType.EXPLOSION;
        if (s.is(DamageTypeTags.IS_PROJECTILE)) return InjuryType.BULLET;
        if (s.is(DamageTypes.STARVE)) return InjuryType.STARVATION;
        if (s.getDirectEntity() instanceof LivingEntity attacker) {
            ItemStack held = attacker.getMainHandItem();
            return held.is(ItemTags.SWORDS) || held.is(ItemTags.AXES) ? InjuryType.BLADE : InjuryType.BLUNT;
        }
        if (s.is(DamageTypes.FALLING_ANVIL) || s.is(DamageTypes.FALLING_BLOCK) || s.is(DamageTypes.FLY_INTO_WALL)
                || s.is(DamageTypes.CRAMMING)) {
            return InjuryType.BLUNT;
        }
        return InjuryType.OTHER;
    }

    private static void onTick(ServerTickEvent.Post e) {
        MedicalServer.tick(e.getServer());
    }

    // ---- azioni vietate a chi è a terra ----

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        block(e);
        if (e.isCanceled() || e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer p)) return;
        // Kit e bende su se stessi, a mani libere da altri bersagli.
        ItemStack stack = e.getItemStack();
        if (stack.is(MedicalItems.MEDKIT.get()) && MedicalServer.treat(p, p, stack, p.getMaxHealth(), true)
                || stack.is(MedicalItems.BANDAGE.get()) && MedicalServer.treat(p, p, stack, 8.0F, false)) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        block(e);
    }

    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e) {
        block(e);
    }

    private static void block(PlayerInteractEvent e) {
        if (!e.getLevel().isClientSide() && MedicalServer.incapacitated(e.getEntity())
                && e instanceof ICancellableEvent c) {
            c.setCanceled(true);
        }
    }

    private static void onAttack(AttackEntityEvent e) {
        if (MedicalServer.incapacitated(e.getEntity())) e.setCanceled(true);
    }

    private static void onBreak(BreakBlockEvent e) {
        if (MedicalServer.incapacitated(e.getPlayer())) e.setCanceled(true);
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof net.minecraft.world.entity.player.Player p && MedicalServer.incapacitated(p)) {
            e.setCanceled(true);
        }
    }

    private static void onToss(ItemTossEvent e) {
        if (MedicalServer.incapacitated(e.getPlayer())) {
            e.setCanceled(true);
            e.getPlayer().getInventory().add(e.getEntity().getItem());
        }
    }

    private static void onUseStart(LivingEntityUseItemEvent.Start e) {
        if (e.getEntity() instanceof net.minecraft.world.entity.player.Player p && MedicalServer.incapacitated(p)) {
            e.setCanceled(true);
        }
    }

    private static void onItemStacked(ItemStackedOnOtherEvent e) {
        if (e.getPlayer() != null && MedicalServer.incapacitated(e.getPlayer())) e.setCanceled(true);
    }

    // ---- interazioni sul ferito ----

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(e.getEntity() instanceof ServerPlayer actor) || !(e.getTarget() instanceof ServerPlayer patient)) return;
        if (MedicalServer.incapacitated(actor)) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        ItemStack held = actor.getMainHandItem();
        boolean down = MedicalServer.incapacitated(patient);
        if (held.is(MedicalItems.DEFIBRILLATOR.get())) {
            if (down) MedicalServer.startRevival(actor, patient);
        } else if (held.is(MedicalItems.MEDKIT.get())) {
            MedicalServer.treat(actor, patient, held, patient.getMaxHealth(), true);
        } else if (held.is(MedicalItems.BANDAGE.get())) {
            MedicalServer.treat(actor, patient, held, 8.0F, false);
        } else if (held.isEmpty() && down) {
            if (actor.isShiftKeyDown()) MedicalServer.rcpClick(actor, patient);
            else MedicalServer.inspect(actor, patient);
        } else {
            return;
        }
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }
}
