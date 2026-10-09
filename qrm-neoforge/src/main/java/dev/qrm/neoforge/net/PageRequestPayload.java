package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C→S: il pannello chiede una pagina di righe (il server ricontrolla il nodo del tipo di pagina). */
public record PageRequestPayload(PageKind kind) implements CustomPacketPayload {
    public static final Type<PageRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("qrm", "page_request"));
    public static final StreamCodec<ByteBuf, PageRequestPayload> CODEC =
            PayloadCodecs.PAGE_KIND.map(PageRequestPayload::new, PageRequestPayload::kind);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
