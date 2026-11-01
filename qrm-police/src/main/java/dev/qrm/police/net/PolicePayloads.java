package dev.qrm.police.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** I pacchetti del Module. Lo stato è un unico JSON: la logica sta tutta sul server. */
public final class PolicePayloads {
    /** Tetto del JSON dello stato (caratteri). */
    public static final int MAX_JSON = 1_000_000;

    private PolicePayloads() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("qrm_police", path));
    }

    /**
     * S→C: lo stato del database per un agente. {@code open} apre la schermata (uso di tablet o terminale), altrimenti
     * aggiorna quella già aperta.
     */
    public record Snapshot(boolean open, String officer, String json) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = typeOf("snapshot");
        public static final StreamCodec<ByteBuf, Snapshot> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Snapshot::open, ByteBufCodecs.stringUtf8(64), Snapshot::officer,
                ByteBufCodecs.stringUtf8(MAX_JSON), Snapshot::json, Snapshot::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** S→C: una risposta a una richiesta dell'agente (posizioni delle unità, storico), senza rimandare tutto lo stato. */
    public record Reply(String kind, String json) implements CustomPacketPayload {
        public static final Type<Reply> TYPE = typeOf("reply");
        public static final StreamCodec<ByteBuf, Reply> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(12), Reply::kind, ByteBufCodecs.stringUtf8(MAX_JSON), Reply::json, Reply::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C→S: un'azione dell'agente. Il server ricontrolla accesso e argomenti. */
    public record Action(String kind, List<String> args) implements CustomPacketPayload {
        public static final Type<Action> TYPE = typeOf("action");
        public static final StreamCodec<ByteBuf, Action> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(24), Action::kind,
                ByteBufCodecs.stringUtf8(400).apply(ByteBufCodecs.list(12)), Action::args, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
