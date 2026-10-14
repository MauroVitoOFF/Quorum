package dev.qrm.bank.net;

import dev.qrm.bank.logic.BankResult;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S→C: stato del conto per la schermata ATM. {@code open} = apri la schermata (altrimenti aggiorna quella aperta);
 * {@code result} = esito dell'ultima azione (mai testo, solo codice); {@code holder}, {@code accountCode} e {@code vip} = nome del
 * personaggio, codice breve del conto e badge: solo da mostrare.
 */
public record AtmStatePayload(boolean open, BankResult result, List<CurrencyInfo> currencies,
        List<BalanceEntry> balances, List<BalanceEntry> held, List<HistoryEntry> history,
        List<String> cashCurrencies, String holder, String accountCode,
        boolean vip) implements CustomPacketPayload {
    public static final int MAX_LIST = 16;
    public static final int MAX_HISTORY = 50;
    public static final int MAX_TEXT = PayloadCodecs.MAX_TEXT;

    public record CurrencyInfo(String code, String symbol, int decimals) {}

    public record BalanceEntry(String currency, long amount) {}

    /**
     * {@code signedAmount} &gt; 0 = accredito, &lt; 0 = addebito. {@code counterparty}: chi manda o riceve (vuoto per
     * prelievi e depositi); {@code note}: causale del bonifico (può essere vuota).
     */
    public record HistoryEntry(long epochSecond, String reason, String currency, long signedAmount,
            String counterparty, String note) {}

    public static final Type<AtmStatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm_bank", "atm_state"));

    private static final StreamCodec<ByteBuf, CurrencyInfo> CURRENCY = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_TEXT), CurrencyInfo::code,
            ByteBufCodecs.stringUtf8(16), CurrencyInfo::symbol,
            ByteBufCodecs.VAR_INT, CurrencyInfo::decimals,
            CurrencyInfo::new);

    private static final StreamCodec<ByteBuf, BalanceEntry> BALANCE = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_TEXT), BalanceEntry::currency,
            ByteBufCodecs.VAR_LONG, BalanceEntry::amount,
            BalanceEntry::new);

    private static final StreamCodec<ByteBuf, HistoryEntry> HISTORY = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, HistoryEntry::epochSecond,
            ByteBufCodecs.stringUtf8(MAX_TEXT), HistoryEntry::reason,
            ByteBufCodecs.stringUtf8(MAX_TEXT), HistoryEntry::currency,
            ByteBufCodecs.VAR_LONG, HistoryEntry::signedAmount,
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_NAME), HistoryEntry::counterparty,
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_NOTE), HistoryEntry::note,
            HistoryEntry::new);

    public static final StreamCodec<ByteBuf, AtmStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AtmStatePayload::open,
            PayloadCodecs.RESULT, AtmStatePayload::result,
            CURRENCY.apply(ByteBufCodecs.list(MAX_LIST)), AtmStatePayload::currencies,
            BALANCE.apply(ByteBufCodecs.list(MAX_LIST)), AtmStatePayload::balances,
            BALANCE.apply(ByteBufCodecs.list(MAX_LIST)), AtmStatePayload::held,
            HISTORY.apply(ByteBufCodecs.list(MAX_HISTORY)), AtmStatePayload::history,
            ByteBufCodecs.stringUtf8(MAX_TEXT).apply(ByteBufCodecs.list(MAX_LIST)), AtmStatePayload::cashCurrencies,
            ByteBufCodecs.stringUtf8(PayloadCodecs.MAX_NAME), AtmStatePayload::holder,
            ByteBufCodecs.stringUtf8(MAX_TEXT), AtmStatePayload::accountCode,
            ByteBufCodecs.BOOL, AtmStatePayload::vip,
            AtmStatePayload::new);

    public AtmStatePayload {
        currencies = List.copyOf(currencies);
        balances = List.copyOf(balances);
        held = List.copyOf(held);
        history = List.copyOf(history);
        cashCurrencies = List.copyOf(cashCurrencies);
    }

    /** Solo un esito (nessun personaggio attivo): la schermata non ha dati da mostrare. */
    public static AtmStatePayload empty(BankResult result) {
        return new AtmStatePayload(false, result, List.of(), List.of(), List.of(), List.of(), List.of(), "", "", false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
