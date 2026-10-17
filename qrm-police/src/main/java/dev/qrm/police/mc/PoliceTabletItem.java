package dev.qrm.police.mc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** L'MDT portatile: il clic destro apre la schermata (decide il server). */
public final class PoliceTabletItem extends Item {
    public PoliceTabletItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) PoliceServer.open(sp);
        return InteractionResult.CONSUME;
    }
}
