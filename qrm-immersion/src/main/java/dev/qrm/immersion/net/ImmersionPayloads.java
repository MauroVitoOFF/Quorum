package dev.qrm.immersion.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** I pacchetti del Module. Il server ricontrolla ogni richiesta del client. */
public final class ImmersionPayloads {
    public static final int REQUEST_TOGGLE_HANDS_UP = 0;
    public static final int REQUEST_POINT_START = 1;
    public static final int REQUEST_POINT_STOP = 2;

    private ImmersionPayloads() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("qrm_immersion", path));
    }

    /** C→S: il giocatore ha premuto o rilasciato un tasto del linguaggio del corpo. */
    public record PoseRequest(int kind) implements CustomPacketPayload {
        public static final Type<PoseRequest> TYPE = typeOf("pose_request");
        public static final StreamCodec<ByteBuf, PoseRequest> CODEC =
                ByteBufCodecs.VAR_INT.map(PoseRequest::new, PoseRequest::kind);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** S→C: la posa di un'entità (giocatore o manichino). {@code pose} è l'ordinale di BodyPose. */
    public record PoseUpdate(int entityId, int pose) implements CustomPacketPayload {
        public static final Type<PoseUpdate> TYPE = typeOf("pose_update");
        public static final StreamCodec<ByteBuf, PoseUpdate> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, PoseUpdate::entityId, ByteBufCodecs.VAR_INT, PoseUpdate::pose, PoseUpdate::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record KnownEntry(UUID id, String name) {
        static final StreamCodec<ByteBuf, KnownEntry> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, KnownEntry::id, ByteBufCodecs.STRING_UTF8, KnownEntry::name, KnownEntry::new);
    }

    /** S→C: tutte le persone che il giocatore conosce, con il nome da mostrare. Sostituisce la lista precedente. */
    public record Known(List<KnownEntry> entries) implements CustomPacketPayload {
        public static final Type<Known> TYPE = typeOf("known");
        public static final StreamCodec<ByteBuf, Known> CODEC =
                KnownEntry.CODEC.apply(ByteBufCodecs.list()).map(Known::new, Known::entries);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
