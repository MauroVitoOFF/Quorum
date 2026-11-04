package dev.qrm.vehicles.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * I pacchetti dei veicoli. Il server è autoritativo: il client manda solo intenzioni (cosa preme il guidatore),
 * mai posizione, velocità o carburante.
 */
public final class VehiclePayloads {
    private VehiclePayloads() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("qrm_vehicles", path));
    }

    /** C→S: gas/retromarcia e sterzo in [-1, 1], freno. Il server li ripulisce comunque. */
    public record Drive(float throttle, float steer, boolean brake) implements CustomPacketPayload {
        public static final Type<Drive> TYPE = typeOf("drive");
        public static final StreamCodec<ByteBuf, Drive> CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Drive::throttle,
                ByteBufCodecs.FLOAT, Drive::steer,
                ByteBufCodecs.BOOL, Drive::brake,
                Drive::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C→S: un'azione del guidatore. {@code kind()} è {@code null} se l'ordinale ricevuto non è valido. */
    public record Action(Kind kind) implements CustomPacketPayload {
        public static final Type<Action> TYPE = typeOf("action");
        public static final StreamCodec<ByteBuf, Action> CODEC = Kind.CODEC.map(Action::new, Action::kind);

        public enum Kind {
            IGNITION, LIGHTS, HORN;

            /** Un ordinale fuori intervallo (client modificato) non lancia: decodifica {@code null}. */
            public static final StreamCodec<ByteBuf, Kind> CODEC = ByteBufCodecs.VAR_INT
                    .map(i -> i >= 0 && i < values().length ? values()[i] : null, Kind::ordinal);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
