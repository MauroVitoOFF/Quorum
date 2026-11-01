package dev.qrm.police.weapon.mc;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Pistola d'ordinanza. Per ora assegna solo l'id vero all'arma; mira, sparo e ricarica arrivano dalla rete.
 * L'uso vanilla resta quello di un oggetto qualsiasi (nessun effetto).
 */
public class PistolItem extends Item {
    public PistolItem(Item.Properties properties) {
        super(properties);
    }

    /**
     * Lo stato dell'arma cambia a ogni sparo e a ogni passo di ricarica, e ogni cambio sostituisce lo stack: il
     * comportamento di NeoForge (ogni stack diverso = nuova estrazione) abbasserebbe e rialzerebbe arma e braccia a ogni
     * cambio. L'estrazione si rifà solo cambiando slot o oggetto.
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        WeaponComponents.ensurePistolId(stack, UUID::randomUUID);
    }
}
