package dev.qrm.police.weapon.mc;

import dev.qrm.police.mc.PoliceItems;
import dev.qrm.police.weapon.logic.MagazineInventory;
import dev.qrm.police.weapon.logic.MagazineState;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * I caricatori nell'inventario principale (36 slot) del giocatore. Contano solo gli oggetti con il componente
 * {@link WeaponComponents#MAGAZINE}; la pistola impugnata non ne ha, quindi non viene mai toccata.
 */
public final class PlayerMagazines implements MagazineInventory {
    private final ServerPlayer player;

    public PlayerMagazines(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public boolean hasLoaded(UUID excluding) {
        return fullestSlot(excluding) >= 0;
    }

    @Override
    public Optional<MagazineState> takeFullest(UUID excluding) {
        int slot = fullestSlot(excluding);
        if (slot < 0) return Optional.empty();
        Inventory inv = player.getInventory();
        MagazineState mag = inv.getItem(slot).get(WeaponComponents.MAGAZINE.get());
        inv.setItem(slot, ItemStack.EMPTY);
        return Optional.ofNullable(mag);
    }

    @Override
    public boolean store(MagazineState m) {
        Inventory inv = player.getInventory();
        // Slot libero esplicito: Inventory.add potrebbe far cadere l'oggetto in silenzio altrove.
        int free = inv.getFreeSlot();
        if (free < 0) return false;
        inv.setItem(free, stackOf(m));
        return true;
    }

    @Override
    public void drop(MagazineState m) {
        // Entità creata direttamente, senza Player.drop: l'evento di lancio di NeoForge potrebbe essere annullato da un
        // altro mod (caricatore perso) o rimettere l'oggetto nell'inventario (caricatore duplicato con un ripiego).
        Vec3 look = player.getLookAngle();
        ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getEyeY() - 0.3, player.getZ(),
                stackOf(m), look.x * 0.3, 0.1, look.z * 0.3);
        entity.setDefaultPickUpDelay();
        player.level().addFreshEntity(entity);
    }

    /** Slot del caricatore con più colpi (almeno uno, id diverso da {@code excluding}); a parità il primo. -1 se nessuno. */
    private int fullestSlot(UUID excluding) {
        Inventory inv = player.getInventory();
        int best = -1;
        int bestRounds = 0;
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !stack.has(WeaponComponents.MAGAZINE.get())) continue;
            MagazineState mag = WeaponComponents.ensureMagazineId(stack, UUID::randomUUID);
            if (mag.id().equals(excluding)) continue;
            if (mag.rounds() > bestRounds) {
                best = i;
                bestRounds = mag.rounds();
            }
        }
        return best;
    }

    private static ItemStack stackOf(MagazineState m) {
        ItemStack stack = new ItemStack(PoliceItems.MAGAZINE.get());
        stack.set(WeaponComponents.MAGAZINE.get(), m);
        return stack;
    }
}
