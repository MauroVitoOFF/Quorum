package dev.qrm.arrival.mc;

import dev.qrm.arrival.logic.ArrivalSession.Phase;
import dev.qrm.arrival.net.ArrivalPayloads;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Manda al giocatore la fase del suo risveglio. Solo a lui: gli altri vedono la posa replicata dal server. */
final class ArrivalSync implements ArrivalServer.Sync {
    @Override
    public void phase(ServerPlayer player, Phase phase, int elapsed) {
        var t = ArrivalServer.files().timing();
        PacketDistributor.sendToPlayer(player,
                new ArrivalPayloads.PhaseUpdate(phase.ordinal(), elapsed, t.lyingTicks(), t.risingTicks()));
    }
}
