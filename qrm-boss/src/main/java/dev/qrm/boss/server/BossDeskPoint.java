package dev.qrm.boss.server;

import dev.qrm.api.point.PointUse;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Scrivania del boss: il Core ha già verificato l'accesso al punto, il modulo ricontrolla {@code boss.menu}. */
public final class BossDeskPoint {
    private BossDeskPoint() {}

    public static void onUse(PointUse use) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(use.player().value());
        if (player == null) return;
        // Il punto è sempre di un'organizzazione (lo garantisce il tipo registrato).
        if (!BossServer.open(player, use.character(), use.point().owner().id())) {
            player.sendSystemMessage(Component.translatable("qrm_boss.cmd.no_access"));
        }
    }
}
