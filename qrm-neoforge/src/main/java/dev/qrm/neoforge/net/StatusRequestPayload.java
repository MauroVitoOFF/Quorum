package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C→S: chiede lo stato aggiornato (apertura dell'hub). */
public record StatusRequestPayload() implements CustomPacketPayload {
    public static final Type<StatusRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm", "status_request"));
    public static final StreamCodec<ByteBuf, StatusRequestPayload> CODEC = StreamCodec.unit(new StatusRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
