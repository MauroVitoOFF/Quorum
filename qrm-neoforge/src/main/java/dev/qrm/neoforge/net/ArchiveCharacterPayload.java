package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C→S: archivia un proprio personaggio. */
public record ArchiveCharacterPayload(UUID id) implements CustomPacketPayload {
    public static final Type<ArchiveCharacterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm", "archive_character"));
    public static final StreamCodec<ByteBuf, ArchiveCharacterPayload> CODEC =
            StreamCodec.composite(UUIDUtil.STREAM_CODEC, ArchiveCharacterPayload::id, ArchiveCharacterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
