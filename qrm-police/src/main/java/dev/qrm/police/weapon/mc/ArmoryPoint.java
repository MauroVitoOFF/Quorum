package dev.qrm.police.weapon.mc;

import dev.qrm.api.point.PointUse;
import dev.qrm.police.mc.PoliceItems;
import dev.qrm.police.weapon.logic.ArmoryRules;
import dev.qrm.police.weapon.logic.MagazineState;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Punto armeria: riempie i caricatori portati nell'inventario principale e ne aggiunge di nuovi fino a
 * {@link #TARGET}. I caricatori inseriti in una pistola non vengono toccati. Nessun controllo di servizio o ruolo:
 * l'accesso è già deciso dal sistema dei punti.
 */
public final class ArmoryPoint {
    private ArmoryPoint() {}

    /** Numero di caricatori che il giocatore deve avere dopo il rifornimento. */
    public static final int TARGET = 3;

    /** Gestore del punto: eseguito sul thread del server dopo i controlli di accesso. */
    public static void open(PointUse use) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(use.player().value());
        if (player == null) return;
        restock(player);
    }

    static void restock(ServerPlayer player) {
        Inventory inv = player.getInventory();
        List<Integer> slots = new ArrayList<>();
        List<MagazineState> carried = new ArrayList<>();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !stack.has(WeaponComponents.MAGAZINE.get())) continue;
            slots.add(i);
            carried.add(WeaponComponents.ensureMagazineId(stack, UUID::randomUUID));
        }
        List<MagazineState> result = ArmoryRules.restock(carried, TARGET, WeaponComponents.MAGAZINE_CAPACITY,
                PistolServer.characterId(player), UUID::randomUUID);
        for (int k = 0; k < slots.size(); k++) {
            inv.getItem(slots.get(k)).set(WeaponComponents.MAGAZINE.get(), result.get(k));
        }
        for (int k = slots.size(); k < result.size(); k++) {
            ItemStack stack = new ItemStack(PoliceItems.MAGAZINE.get());
            stack.set(WeaponComponents.MAGAZINE.get(), result.get(k));
            int free = inv.getFreeSlot();
            if (free >= 0) inv.setItem(free, stack);
            else dropAtPlayer(player, stack);
        }
        inv.setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.translatable("qrm_police.armory.restocked", result.size()));
    }

    /** Entità creata direttamente, come in {@link PlayerMagazines#drop}: mai tramite Player.drop. */
    private static void dropAtPlayer(ServerPlayer player, ItemStack stack) {
        Vec3 look = player.getLookAngle();
        ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getEyeY() - 0.3, player.getZ(),
                stack, look.x * 0.3, 0.1, look.z * 0.3);
        entity.setDefaultPickUpDelay();
        player.level().addFreshEntity(entity);
    }
}
