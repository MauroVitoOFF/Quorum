package dev.qrm.bank.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C→S: sceglie il conto con cui operare all'ATM: vuoto = personale, altrimenti l'id di un'organizzazione. */
public record SelectAccountPayload(String orgId) implements CustomPacketPayload {
    public static final Type<SelectAccountPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm_bank", "select_account"));
    public static final StreamCodec<ByteBuf, SelectAccountPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_TEXT), SelectAccountPayload::orgId,
            SelectAccountPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
