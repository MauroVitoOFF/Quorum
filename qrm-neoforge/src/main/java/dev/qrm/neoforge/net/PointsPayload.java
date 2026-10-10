package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S→C: l'elenco completo dei punti di interazione che questo giocatore deve vedere (sostituisce il precedente). */
public record PointsPayload(List<PointEntry> points) implements CustomPacketPayload {
    /** Punti massimi per pacchetto: protegge il client da pacchetti enormi. */
    public static final int MAX_POINTS = 512;
    public static final Type<PointsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("qrm", "points"));

    private static final StreamCodec<ByteBuf, PointEntry> ENTRY = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PointEntry::id,
            ByteBufCodecs.stringUtf8(32), PointEntry::typeId,
            ByteBufCodecs.stringUtf8(128), PointEntry::dimension,
            ByteBufCodecs.VAR_INT, PointEntry::x,
            ByteBufCodecs.VAR_INT, PointEntry::y,
            ByteBufCodecs.VAR_INT, PointEntry::z,
            ByteBufCodecs.stringUtf8(128), PointEntry::labelKey,
            ByteBufCodecs.optional(ByteBufCodecs.stringUtf8(64)), PointEntry::name,
            ByteBufCodecs.optional(ByteBufCodecs.stringUtf8(8)), PointEntry::face,
            PointEntry::new);

    public static final StreamCodec<ByteBuf, PointsPayload> CODEC = StreamCodec.composite(
            ENTRY.apply(ByteBufCodecs.list(MAX_POINTS)), PointsPayload::points,
            PointsPayload::new);

    public PointsPayload {
        points = List.copyOf(points);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
