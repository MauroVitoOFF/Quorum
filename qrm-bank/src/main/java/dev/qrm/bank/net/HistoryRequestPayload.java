package dev.qrm.bank.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C→S: chiede lo stato aggiornato del conto (ATM già aperto). */
public record HistoryRequestPayload() implements CustomPacketPayload {
    public static final Type<HistoryRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm_bank", "history_request"));
    public static final StreamCodec<ByteBuf, HistoryRequestPayload> CODEC =
            StreamCodec.unit(new HistoryRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
