package dev.qrm.police.weapon.net;

import dev.qrm.police.weapon.logic.PistolAction;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * I pacchetti della pistola di servizio. Il server è autoritativo: il client manda solo intenzioni
 * ({@link Input}), mai munizioni, colpi a segno o danno.
 */
public final class PistolPayloads {
    private PistolPayloads() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("qrm_police", path));
    }

    /**
     * Codec di un enum per ordinale. Un ordinale fuori intervallo (client modificato) non lancia: decodifica
     * {@code null}, che i gestori ignorano, così il thread del server non viene toccato.
     */
    private static <E extends Enum<E>> StreamCodec<ByteBuf, E> enumCodec(Class<E> type) {
        E[] values = type.getEnumConstants();
        return ByteBufCodecs.VAR_INT.map(i -> i >= 0 && i < values.length ? values[i] : null, Enum::ordinal);
    }

    /** C→S: un'intenzione del giocatore. {@code kind()} è {@code null} se l'ordinale ricevuto non è valido. */
    public record Input(Kind kind) implements CustomPacketPayload {
        public static final Type<Input> TYPE = typeOf("pistol_input");
        public static final StreamCodec<ByteBuf, Input> STREAM_CODEC = Kind.CODEC.map(Input::new, Input::kind);

        /** Tipi di intenzione. */
        public enum Kind {
            FIRE, AIM_ON, AIM_OFF, RELOAD, INTERACT_TAP, INTERACT_HOLD;

            public static final StreamCodec<ByteBuf, Kind> CODEC = enumCodec(Kind.class);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** S→C: è iniziata un'azione temporizzata (a chi osserva e a sé). */
    public record ActionStarted(int entityId, PistolAction action) implements CustomPacketPayload {
        public static final Type<ActionStarted> TYPE = typeOf("pistol_action_started");
        public static final StreamCodec<ByteBuf, ActionStarted> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ActionStarted::entityId,
                enumCodec(PistolAction.class), ActionStarted::action, ActionStarted::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** S→C: l'azione è finita o è stata annullata. */
    public record ActionEnded(int entityId, boolean cancelled) implements CustomPacketPayload {
        public static final Type<ActionEnded> TYPE = typeOf("pistol_action_ended");
        public static final StreamCodec<ByteBuf, ActionEnded> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ActionEnded::entityId,
                ByteBufCodecs.BOOL, ActionEnded::cancelled, ActionEnded::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** S→C: un colpo esploso, per effetti di traccia e impatto (a chi osserva). */
    public record Shot(int entityId, Vec3 from, Vec3 to, Optional<BlockPos> block, boolean hitEntity)
            implements CustomPacketPayload {
        public static final Type<Shot> TYPE = typeOf("pistol_shot");
        public static final StreamCodec<ByteBuf, Shot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Shot::entityId,
                Vec3.STREAM_CODEC, Shot::from,
                Vec3.STREAM_CODEC, Shot::to,
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), Shot::block,
                ByteBufCodecs.BOOL, Shot::hitEntity, Shot::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** S→C: lo stato di mira di un giocatore. */
    public record Aim(int entityId, boolean aiming) implements CustomPacketPayload {
        public static final Type<Aim> TYPE = typeOf("pistol_aim");
        public static final StreamCodec<ByteBuf, Aim> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Aim::entityId,
                ByteBufCodecs.BOOL, Aim::aiming, Aim::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** S→C: l'intenzione è stata rifiutata (a sé). */
    public record Rejected(Input.Kind kind) implements CustomPacketPayload {
        public static final Type<Rejected> TYPE = typeOf("pistol_rejected");
        public static final StreamCodec<ByteBuf, Rejected> STREAM_CODEC =
                Input.Kind.CODEC.map(Rejected::new, Rejected::kind);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** S→C: l'identità del Character che impugna l'arma (a sé). */
    public record Identity(String characterId) implements CustomPacketPayload {
        public static final Type<Identity> TYPE = typeOf("pistol_identity");
        public static final StreamCodec<ByteBuf, Identity> STREAM_CODEC =
                ByteBufCodecs.stringUtf8(64).map(Identity::new, Identity::characterId);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * S→C: il giocatore è a terra (o non lo è più), secondo il registro dello stato a terra di QRM. Mandato a sé a ogni
     * cambio e all'ingresso: finché vale il client non prevede spari e non mira.
     */
    public record Incapacitated(boolean value) implements CustomPacketPayload {
        public static final Type<Incapacitated> TYPE = typeOf("pistol_incapacitated");
        public static final StreamCodec<ByteBuf, Incapacitated> STREAM_CODEC =
                ByteBufCodecs.BOOL.map(Incapacitated::new, Incapacitated::value);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
