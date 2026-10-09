package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C→S: il pannello staff chiede l'elenco dei giocatori (il server ricontrolla il permesso). */
public record PanelRequestPayload() implements CustomPacketPayload {
    public static final Type<PanelRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm", "panel_request"));
    public static final StreamCodec<ByteBuf, PanelRequestPayload> CODEC = StreamCodec.unit(new PanelRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
