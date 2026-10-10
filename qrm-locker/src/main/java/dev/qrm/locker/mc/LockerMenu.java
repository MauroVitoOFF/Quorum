package dev.qrm.locker.mc;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

/** Un baule vanilla da 3 righe: il client senza il mod lo apre come un baule qualunque. */
final class LockerMenu extends ChestMenu {
    private final Runnable onClosed;

    LockerMenu(int id, Inventory inventory, LockerContainer container, Runnable onClosed) {
        super(MenuType.GENERIC_9x3, id, inventory, container, 3);
        this.onClosed = onClosed;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        onClosed.run();
    }
}
