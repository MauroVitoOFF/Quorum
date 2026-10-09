package dev.qrm.bank.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C→S: bonifico a "Nome Cognome" oppure "@idOrganizzazione", con causale facoltativa (anche vuota). */
public record TransferPayload(String recipient, String currency, long amount, String note)
        implements CustomPacketPayload {
    public static final Type<TransferPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm_bank", "transfer"));
    public static final StreamCodec<ByteBuf, TransferPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_NAME), TransferPayload::recipient,
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_TEXT), TransferPayload::currency,
            ByteBufCodecs.VAR_LONG, TransferPayload::amount,
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_NOTE), TransferPayload::note,
            TransferPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
