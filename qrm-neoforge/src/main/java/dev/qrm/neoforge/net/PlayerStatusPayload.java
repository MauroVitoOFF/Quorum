package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S→C: stato del personaggio attivo per HUD e hub. */
public record PlayerStatusPayload(PlayerStatus status) implements CustomPacketPayload {
    public static final Type<PlayerStatusPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm", "player_status"));
    public static final StreamCodec<ByteBuf, PlayerStatusPayload> CODEC =
            PlayerStatus.CODEC.map(PlayerStatusPayload::new, PlayerStatusPayload::status);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
