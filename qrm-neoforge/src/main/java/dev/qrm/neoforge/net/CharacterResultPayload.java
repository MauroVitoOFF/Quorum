package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S→C: esito di un'azione (mai testo, solo codice). */
public record CharacterResultPayload(CharacterAction action, ResultCode code) implements CustomPacketPayload {
    public static final Type<CharacterResultPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm", "character_result"));
    public static final StreamCodec<ByteBuf, CharacterResultPayload> CODEC = StreamCodec.composite(
            PayloadCodecs.ACTION, CharacterResultPayload::action,
            PayloadCodecs.RESULT, CharacterResultPayload::code,
            CharacterResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
