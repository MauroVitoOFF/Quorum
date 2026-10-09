package dev.qrm.bank.mc;

import dev.qrm.bank.logic.CashInventory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Il contante nell'inventario principale (hotbar e zaino; non armatura né mano secondaria) di un giocatore. */
public final class PlayerCash implements CashInventory {
    private final ServerPlayer player;

    public PlayerCash(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public long count(String currency) {
        long total = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() instanceof CashItem cash && cash.currency().equals(currency)) {
                total += cash.amount() * stack.getCount();
            }
        }
        return total;
    }

    @Override
    public boolean canFit(String currency, Map<Long, Integer> notes) {
        List<ItemStack> simulated = new ArrayList<>();
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) simulated.add(stack.copy());
        for (Map.Entry<Long, Integer> e : notes.entrySet()) {
            ItemStack toPlace = new ItemStack(BankItems.item(currency, e.getKey()).get(), e.getValue());
            if (!place(simulated, toPlace)) return false;
        }
        return true;
    }

    /** Inserisce prima negli stack uguali non pieni, poi negli slot vuoti. */
    private static boolean place(List<ItemStack> slots, ItemStack toPlace) {
        int remaining = toPlace.getCount();
        int max = toPlace.getMaxStackSize();
        for (ItemStack stack : slots) {
            if (remaining == 0) break;
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, toPlace) && stack.getCount() < max) {
                int moved = Math.min(remaining, max - stack.getCount());
                stack.grow(moved);
                remaining -= moved;
            }
        }
        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            if (!slots.get(i).isEmpty()) continue;
            int moved = Math.min(remaining, max);
            slots.set(i, toPlace.copyWithCount(moved));
            remaining -= moved;
        }
        return remaining == 0;
    }

    @Override
    public void give(String currency, Map<Long, Integer> notes) {
        Inventory inventory = player.getInventory();
        for (Map.Entry<Long, Integer> e : notes.entrySet()) {
            ItemStack stack = new ItemStack(BankItems.item(currency, e.getKey()).get(), e.getValue());
            inventory.add(stack);
            // canFit ha garantito lo spazio; per sicurezza, ciò che non entra cade a terra invece di sparire.
            if (!stack.isEmpty()) player.drop(stack, false);
        }
    }

    @Override
    public boolean remove(String currency, long expectedTotal) {
        if (count(currency) != expectedTotal) return false;
        NonNullList<ItemStack> items = player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getItem() instanceof CashItem cash && cash.currency().equals(currency)) {
                items.set(i, ItemStack.EMPTY);
            }
        }
        player.getInventory().setChanged();
        return true;
    }
}
