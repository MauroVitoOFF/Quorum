package dev.qrm.police.mc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Terminale fisso della centrale: stessa schermata del tablet. Nessuna block entity: i dati stanno nel database. */
public final class PoliceComputerBlock extends Block {
    public static final MapCodec<PoliceComputerBlock> CODEC = simpleCodec(PoliceComputerBlock::new);

    public PoliceComputerBlock(BlockBehaviour.Properties properties) {
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
        if (player instanceof ServerPlayer sp) PoliceServer.open(sp);
        return InteractionResult.CONSUME;
    }
}
