package dev.qrm.bank.net;

import dev.qrm.bank.logic.StaffBankLogic;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S&rarr;C: stato del conto bancario del bersaglio (saldi e ultimi movimenti), valute disponibili e esito dell'ultima
 * azione (mai testo, solo codici e importi gi&agrave; formattati). {@code auditIssues} vale -1 se la risposta non contiene
 * un audit.
 */
public record StaffBankStatePayload(StaffBankAction action, StaffBankResult result, boolean hasCharacter,
        boolean frozen, List<StaffBankLogic.Line> balances, List<StaffBankLogic.Move> history, List<String> currencies,
        String primaryCurrency, int auditIssues, List<StaffBankLogic.Excess> excess) implements CustomPacketPayload {
    public static final int MAX_LIST = 32;
    public static final Type<StaffBankStatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm_bank", "staff_state"));

    private static final StreamCodec<ByteBuf, StaffBankLogic.Line> LINE = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_TEXT), StaffBankLogic.Line::currency,
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_TEXT), StaffBankLogic.Line::text,
            StaffBankLogic.Line::new);
    private static final StreamCodec<ByteBuf, StaffBankLogic.Move> MOVE = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, StaffBankLogic.Move::epochSecond,
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_TEXT), StaffBankLogic.Move::amount,
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_TEXT), StaffBankLogic.Move::reason,
            StaffBankLogic.Move::new);
    private static final StreamCodec<ByteBuf, StaffBankLogic.Excess> EXCESS = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_TEXT), StaffBankLogic.Excess::currency,
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_TEXT), StaffBankLogic.Excess::held,
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_TEXT), StaffBankLogic.Excess::vault,
            StaffBankLogic.Excess::new);

    public static final StreamCodec<ByteBuf, StaffBankStatePayload> CODEC = StreamCodec.composite(
            PayloadCodecs.STAFF_ACTION, StaffBankStatePayload::action,
            PayloadCodecs.STAFF_RESULT, StaffBankStatePayload::result,
            ByteBufCodecs.BOOL, StaffBankStatePayload::hasCharacter,
            ByteBufCodecs.BOOL, StaffBankStatePayload::frozen,
            LINE.apply(ByteBufCodecs.list(MAX_LIST)), StaffBankStatePayload::balances,
            MOVE.apply(ByteBufCodecs.list(MAX_LIST)), StaffBankStatePayload::history,
            ByteBufCodecs.stringUtf8(StaffBankActionPayload.MAX_CURRENCY).apply(ByteBufCodecs.list(MAX_LIST)),
            StaffBankStatePayload::currencies,
            ByteBufCodecs.stringUtf8(StaffBankActionPayload.MAX_CURRENCY), StaffBankStatePayload::primaryCurrency,
            ByteBufCodecs.VAR_INT, StaffBankStatePayload::auditIssues,
            EXCESS.apply(ByteBufCodecs.list(MAX_LIST)), StaffBankStatePayload::excess,
            StaffBankStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
