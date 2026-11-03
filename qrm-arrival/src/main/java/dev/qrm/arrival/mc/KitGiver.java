package dev.qrm.arrival.mc;

import dev.qrm.arrival.logic.Kit;
import dev.qrm.arrival.logic.KitEntry;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Consegna il kit al giocatore. Ciò che non entra nell'inventario cade a terra: non sparisce mai. */
final class KitGiver {
    private KitGiver() {}

    static void give(ServerPlayer player, Kit kit) {
        for (KitEntry entry : kit.entries()) {
            Identifier id = Identifier.tryParse(entry.itemId());
            Optional<Item> item = id == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(id);
            if (item.isEmpty()) {
                ArrivalMod.LOG.warn("Kit: oggetto sconosciuto '{}', saltato", entry.itemId());
                continue;
            }
            ItemStack stack = new ItemStack(item.get(), entry.count());
            player.getInventory().add(stack);
            if (!stack.isEmpty()) player.drop(stack, false);
        }
    }
}
