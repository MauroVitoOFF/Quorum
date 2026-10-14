package dev.qrm.bank.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C→S: deposita {@code amount} (unità minime) del contante di {@code currency} che il giocatore ha con sé. */
public record DepositPayload(String currency, long amount) implements CustomPacketPayload {
    public static final Type<DepositPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm_bank", "deposit"));
    public static final StreamCodec<ByteBuf, DepositPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_TEXT), DepositPayload::currency,
            ByteBufCodecs.VAR_LONG, DepositPayload::amount,
            DepositPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
