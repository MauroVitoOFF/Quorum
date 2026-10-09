package dev.qrm.bank.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C→S: ritira {@code amount} unità minime di {@code currency} dal conto del personaggio attivo. */
public record WithdrawPayload(String currency, long amount) implements CustomPacketPayload {
    public static final Type<WithdrawPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm_bank", "withdraw"));
    public static final StreamCodec<ByteBuf, WithdrawPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_TEXT), WithdrawPayload::currency,
            ByteBufCodecs.VAR_LONG, WithdrawPayload::amount,
            WithdrawPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
