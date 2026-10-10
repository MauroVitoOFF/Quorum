package dev.qrm.locker.mc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.qrm.locker.logic.LockerSlots;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

/** Conversione fra oggetti e JSON, con il codec di Minecraft (componenti compresi). */
final class LockerItems {
    private final RegistryOps<JsonElement> ops;

    LockerItems(HolderLookup.Provider registries) {
        this.ops = registries.createSerializationContext(JsonOps.INSTANCE);
    }

    Optional<JsonElement> encode(ItemStack stack) {
        return ItemStack.CODEC.encodeStart(ops, stack).result();
    }

    Optional<ItemStack> decode(JsonElement json) {
        try {
            return ItemStack.CODEC.parse(ops, json).result().filter(s -> !s.isEmpty());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /** Riempie il container; ciò che non si legge (più) resta fra gli illeggibili. */
    void load(LockerSlots saved, LockerContainer container) {
        for (Map.Entry<Integer, JsonElement> e : saved.items().entrySet()) {
            Optional<ItemStack> stack = decode(e.getValue());
            if (stack.isPresent()) container.setItem(e.getKey(), stack.get());
            else container.keepUnreadable(e.getKey(), e.getValue());
        }
        for (JsonObject entry : saved.unreadable()) {
            int slot = entry.get("slot").getAsInt();
            JsonElement item = entry.get("item");
            Optional<ItemStack> stack = decode(item);
            // Il mod è tornato: l'oggetto riappare, se il suo slot è libero.
            if (stack.isPresent() && slot >= 0 && slot < LockerSlots.SIZE && container.getItem(slot).isEmpty()) {
                container.setItem(slot, stack.get());
            } else {
                container.keepUnreadable(slot, item);
            }
        }
    }

    LockerSlots snapshot(LockerContainer container) {
        LockerSlots slots = LockerSlots.empty();
        for (int i = 0; i < LockerSlots.SIZE; i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            Optional<JsonElement> json = encode(stack);
            if (json.isEmpty()) throw new IllegalStateException("item not serializable in slot " + i);
            slots.put(i, json.get());
        }
        container.unreadable().forEach(e -> slots.addUnreadable(e.get("slot").getAsInt(), e.get("item")));
        return slots;
    }
}
