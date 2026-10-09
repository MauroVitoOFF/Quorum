package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C→S: chiede il roster aggiornato e l'apertura della schermata. */
public record RosterRequestPayload() implements CustomPacketPayload {
    public static final Type<RosterRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm", "roster_request"));
    public static final StreamCodec<ByteBuf, RosterRequestPayload> CODEC = StreamCodec.unit(new RosterRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
