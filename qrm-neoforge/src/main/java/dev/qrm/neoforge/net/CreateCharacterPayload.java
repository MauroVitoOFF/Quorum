package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C→S: crea un personaggio. La data è in tre interi: la validazione è del server (INVALID_DATE). */
public record CreateCharacterPayload(String firstName, String lastName, int year, int month, int day)
        implements CustomPacketPayload {
    public static final Type<CreateCharacterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm", "create_character"));
    public static final StreamCodec<ByteBuf, CreateCharacterPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_NAME), CreateCharacterPayload::firstName,
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_NAME), CreateCharacterPayload::lastName,
            ByteBufCodecs.VAR_INT, CreateCharacterPayload::year,
            ByteBufCodecs.VAR_INT, CreateCharacterPayload::month,
            ByteBufCodecs.VAR_INT, CreateCharacterPayload::day,
            CreateCharacterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
