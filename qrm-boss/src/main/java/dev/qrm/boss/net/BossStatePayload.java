package dev.qrm.boss.net;

import dev.qrm.boss.logic.BossReply;
import dev.qrm.boss.logic.BossWire;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S&rarr;C: snapshot completo del menu e esito dell'ultima richiesta. */
public record BossStatePayload(BossReply reply) implements CustomPacketPayload {
    public static final Type<BossStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("qrm_boss", "state"));
    public static final StreamCodec<ByteBuf, BossStatePayload> CODEC = ByteBufCodecs.byteArray(1 << 17)
            .map(bytes -> new BossStatePayload(BossWire.decodeReply(bytes)), p -> BossWire.encode(p.reply()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
