package dev.qrm.wardrobe.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** I quattro pacchetti del Module. Il formato dei dati è in {@code WardrobeWire}, in puro Java. */
public final class WardrobePayloads {
    private WardrobePayloads() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("qrm_wardrobe", path));
    }

    /** S→C: una divisa del catalogo. {@code total} = 0 svuota il catalogo; altrimenti {@code index} 0 lo riparte da capo. */
    public record Catalog(int total, int index, byte[] entry) implements CustomPacketPayload {
        public static final Type<Catalog> TYPE = typeOf("catalog");
        public static final StreamCodec<ByteBuf, Catalog> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Catalog::total, ByteBufCodecs.VAR_INT, Catalog::index,
                ByteBufCodecs.byteArray(40_000), Catalog::entry, Catalog::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** S→C: l'istantanea di chi indossa che cosa. */
    public record Worn(byte[] data) implements CustomPacketPayload {
        public static final Type<Worn> TYPE = typeOf("worn");
        public static final StreamCodec<ByteBuf, Worn> CODEC =
                ByteBufCodecs.byteArray(1 << 17).map(Worn::new, Worn::data);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** S→C: apri la schermata dell'armadietto. */
    public record Open(byte[] data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = typeOf("open");
        public static final StreamCodec<ByteBuf, Open> CODEC =
                ByteBufCodecs.byteArray(1 << 14).map(Open::new, Open::data);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C→S: la divisa scelta (stringa vuota = abito civile). Il server ricontrolla tutto. */
    public record Choose(byte[] data) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = typeOf("choose");
        public static final StreamCodec<ByteBuf, Choose> CODEC =
                ByteBufCodecs.byteArray(256).map(Choose::new, Choose::data);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
