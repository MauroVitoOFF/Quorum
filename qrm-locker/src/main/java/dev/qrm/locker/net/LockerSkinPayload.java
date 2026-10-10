package dev.qrm.locker.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S→C: la prossima schermata di baule da 27 slot è un armadietto, da disegnare a tema. */
public record LockerSkinPayload() implements CustomPacketPayload {
    public static final Type<LockerSkinPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm_locker", "skin"));
    public static final StreamCodec<ByteBuf, LockerSkinPayload> CODEC = StreamCodec.unit(new LockerSkinPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
