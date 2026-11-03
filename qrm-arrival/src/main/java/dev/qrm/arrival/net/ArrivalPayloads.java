package dev.qrm.arrival.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Pacchetti del Module. */
public final class ArrivalPayloads {
    private ArrivalPayloads() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("qrm_arrival", path));
    }

    /**
     * S→C: la fase del risveglio del giocatore che lo riceve. {@code phase} è l'ordinale di
     * {@code ArrivalSession.Phase}; le durate servono al client per l'effetto degli occhi.
     */
    public record PhaseUpdate(int phase, int elapsed, int lyingTicks, int risingTicks) implements CustomPacketPayload {
        public static final Type<PhaseUpdate> TYPE = typeOf("phase");
        public static final StreamCodec<ByteBuf, PhaseUpdate> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, PhaseUpdate::phase,
                ByteBufCodecs.VAR_INT, PhaseUpdate::elapsed,
                ByteBufCodecs.VAR_INT, PhaseUpdate::lyingTicks,
                ByteBufCodecs.VAR_INT, PhaseUpdate::risingTicks,
                PhaseUpdate::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
