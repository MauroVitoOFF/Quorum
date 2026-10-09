package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C→S: seleziona uno dei propri personaggi. */
public record SelectCharacterPayload(UUID id) implements CustomPacketPayload {
    public static final Type<SelectCharacterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm", "select_character"));
    public static final StreamCodec<ByteBuf, SelectCharacterPayload> CODEC =
            StreamCodec.composite(UUIDUtil.STREAM_CODEC, SelectCharacterPayload::id, SelectCharacterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
