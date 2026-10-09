package dev.qrm.bank.mc;

import com.mojang.serialization.MapCodec;
import dev.qrm.bank.server.AtmActions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Sportello ATM: nessuna block entity né dati. Il click destro apre la schermata (decide il server). */
public final class AtmBlock extends Block {
    public static final MapCodec<AtmBlock> CODEC = simpleCodec(AtmBlock::new);

    public AtmBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) AtmActions.open(serverPlayer, pos);
        return InteractionResult.CONSUME;
    }
}
