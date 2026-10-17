package dev.qrm.medical.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** I pacchetti del Module. Lo stato degli altri giocatori non serve: la posa a terra la sincronizza il gioco. */
public final class MedicalPayloads {
    public static final int ACTION_SURRENDER = 0;
    public static final int ACTION_ALERT = 1;

    private MedicalPayloads() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("qrm_medical", path));
    }

    /** S→C: lo stato del giocatore stesso. {@code state} è l'ordinale di MedicalState. */
    public record Status(int state, int bleedOutTicks, int downedTicks, boolean rcp) implements CustomPacketPayload {
        public static final Type<Status> TYPE = typeOf("status");
        public static final StreamCodec<ByteBuf, Status> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Status::state, ByteBufCodecs.VAR_INT, Status::bleedOutTicks,
                ByteBufCodecs.VAR_INT, Status::downedTicks, ByteBufCodecs.BOOL, Status::rcp, Status::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C→S: resa al coma o allerta ai soccorsi. Il server ricontrolla tutto. */
    public record Action(int kind) implements CustomPacketPayload {
        public static final Type<Action> TYPE = typeOf("action");
        public static final StreamCodec<ByteBuf, Action> CODEC =
                ByteBufCodecs.VAR_INT.map(Action::new, Action::kind);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
