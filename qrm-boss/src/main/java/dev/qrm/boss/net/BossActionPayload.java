package dev.qrm.boss.net;

import dev.qrm.boss.logic.BossRequest;
import dev.qrm.boss.logic.BossWire;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C&rarr;S: una richiesta del menu boss (il server ricontrolla tutto). */
public record BossActionPayload(BossRequest request) implements CustomPacketPayload {
    public static final Type<BossActionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm_boss", "action"));
    public static final StreamCodec<ByteBuf, BossActionPayload> CODEC = ByteBufCodecs.byteArray(1024)
            .map(bytes -> new BossActionPayload(BossWire.decodeRequest(bytes)), p -> BossWire.encode(p.request()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
