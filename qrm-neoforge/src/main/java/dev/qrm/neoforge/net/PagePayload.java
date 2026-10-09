package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S→C: una pagina di righe per il pannello (titolo come chiave di traduzione; etichette letterali già formattate dal server). */
public record PagePayload(String titleKey, List<PlayerInfoLine> lines) implements CustomPacketPayload {
    public static final Type<PagePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("qrm", "page"));
    public static final StreamCodec<ByteBuf, PagePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), PagePayload::titleKey,
            PayloadCodecs.INFO_LINES, PagePayload::lines,
            PagePayload::new);

    public PagePayload {
        lines = List.copyOf(lines);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
