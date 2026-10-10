package dev.qrm.neoforge.server;

import dev.qrm.api.QRM;
import dev.qrm.api.point.InactiveReason;
import dev.qrm.api.point.InteractionPoint;
import dev.qrm.api.point.PointPosition;
import dev.qrm.api.point.PointService;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Le cause di inattività che richiedono il mondo (dimensione e blocco), più quelle del Core. */
public final class PointWorld {
    private PointWorld() {}

    public static PointPosition positionOf(Level level, BlockPos pos) {
        return new PointPosition(level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    /** Vuoto se il punto è attivo. Un blocco in un chunk non caricato non si può verificare: si presume presente. */
    public static Optional<InactiveReason> inactiveReason(InteractionPoint point) {
        Optional<InactiveReason> core = QRM.get(PointService.class).inactiveReason(point);
        if (core.isPresent()) return core;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return Optional.empty();
        PointPosition p = point.position();
        Identifier id = Identifier.tryParse(p.dimension());
        ServerLevel level = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (level == null) return Optional.of(InactiveReason.UNKNOWN_DIMENSION);
        BlockPos pos = new BlockPos(p.x(), p.y(), p.z());
        if (!level.isLoaded(pos)) return Optional.empty();
        return level.getBlockState(pos).isAir() ? Optional.of(InactiveReason.MISSING_BLOCK) : Optional.empty();
    }

    public static boolean active(InteractionPoint point) {
        return inactiveReason(point).isEmpty();
    }
}
