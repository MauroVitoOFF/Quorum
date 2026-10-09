package dev.qrm.bank.net;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C&rarr;S: un'azione staff della banca sul giocatore {@code target} (il server ricontrolla permesso e bersaglio).
 * {@code amount} e {@code currency} contano solo per {@code GRANT} e {@code TAKE}.
 */
public record StaffBankActionPayload(StaffBankAction action, Optional<UUID> target, String amount, String currency)
        implements CustomPacketPayload {
    public static final int MAX_AMOUNT = 24;
    public static final int MAX_CURRENCY = 16;
    public static final Type<StaffBankActionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm_bank", "staff_action"));
    public static final StreamCodec<ByteBuf, StaffBankActionPayload> CODEC = StreamCodec.composite(
            PayloadCodecs.STAFF_ACTION, StaffBankActionPayload::action,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), StaffBankActionPayload::target,
            ByteBufCodecs.stringUtf8(MAX_AMOUNT), StaffBankActionPayload::amount,
            ByteBufCodecs.stringUtf8(MAX_CURRENCY), StaffBankActionPayload::currency,
            StaffBankActionPayload::new);

    /** Azione senza importo (tutte tranne dare e togliere denaro). */
    public StaffBankActionPayload(StaffBankAction action, Optional<UUID> target) {
        this(action, target, "", "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
