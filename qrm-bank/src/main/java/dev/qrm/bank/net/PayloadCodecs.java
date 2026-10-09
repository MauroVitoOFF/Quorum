package dev.qrm.bank.net;

import dev.qrm.bank.logic.BankResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

final class PayloadCodecs {
    static final int MAX_TEXT = 64;
    static final int MAX_NAME = 129;
    /** Causale: 64 caratteri ({@code TransferNote.MAX_LENGTH}); il codec conta caratteri UTF-16, quindi con margine. */
    static final int MAX_NOTE = 4 * dev.qrm.bank.logic.TransferNote.MAX_LENGTH;

    /** Si trasmette l'indice dell'enum; un indice fuori intervallo diventa INTERNAL_ERROR. */
    static final StreamCodec<ByteBuf, BankResult> RESULT = ByteBufCodecs.VAR_INT.map(
            i -> i >= 0 && i < BankResult.values().length ? BankResult.values()[i] : BankResult.INTERNAL_ERROR,
            BankResult::ordinal);

    static final StreamCodec<ByteBuf, StaffBankAction> STAFF_ACTION =
            ByteBufCodecs.VAR_INT.map(StaffBankAction::fromOrdinal, StaffBankAction::ordinal);
    static final StreamCodec<ByteBuf, StaffBankResult> STAFF_RESULT =
            ByteBufCodecs.VAR_INT.map(StaffBankResult::fromOrdinal, StaffBankResult::ordinal);

    private PayloadCodecs() {}
}
