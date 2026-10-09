package dev.qrm.neoforge.net;

import dev.qrm.api.economy.Currency;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Stato del personaggio attivo mostrato da HUD e hub. {@code characterName} vuoto = nessun personaggio attivo.
 * {@code primaryCurrency} è vuoto se il server non ha valute registrate.
 */
public record PlayerStatus(String characterName, String primaryCurrency, List<CurrencyInfo> currencies,
        List<BalanceEntry> balances, Optional<JobInfo> job, List<OrgEntry> orgs, UiConfig ui) {
    public static final int MAX_FULL_NAME = 129;
    public static final int MAX_TEXT = 64;
    public static final int MAX_SYMBOL = 16;
    public static final int MAX_CURRENCIES = 16;
    public static final int MAX_BALANCES = 16;
    public static final int MAX_ORGS = 32;

    public record CurrencyInfo(String code, String symbol, int decimals) {}

    public record BalanceEntry(String currency, long amount) {}

    public record JobInfo(String jobLabel, String gradeLabel, boolean onDuty, long sinceEpochSecond, boolean known) {}

    public record OrgEntry(String label, String rankLabel, boolean viaJob) {}

    public static final PlayerStatus EMPTY =
            new PlayerStatus("", "", List.of(), List.of(), Optional.empty(), List.of(), UiConfig.DEFAULT);

    private static final StreamCodec<ByteBuf, CurrencyInfo> CURRENCY = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_TEXT), CurrencyInfo::code,
            ByteBufCodecs.stringUtf8(MAX_SYMBOL), CurrencyInfo::symbol,
            ByteBufCodecs.VAR_INT, CurrencyInfo::decimals,
            CurrencyInfo::new);

    private static final StreamCodec<ByteBuf, BalanceEntry> BALANCE = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_TEXT), BalanceEntry::currency,
            ByteBufCodecs.VAR_LONG, BalanceEntry::amount,
            BalanceEntry::new);

    private static final StreamCodec<ByteBuf, JobInfo> JOB = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_TEXT), JobInfo::jobLabel,
            ByteBufCodecs.stringUtf8(MAX_TEXT), JobInfo::gradeLabel,
            ByteBufCodecs.BOOL, JobInfo::onDuty,
            ByteBufCodecs.VAR_LONG, JobInfo::sinceEpochSecond,
            ByteBufCodecs.BOOL, JobInfo::known,
            JobInfo::new);

    private static final StreamCodec<ByteBuf, OrgEntry> ORG = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_TEXT), OrgEntry::label,
            ByteBufCodecs.stringUtf8(MAX_TEXT), OrgEntry::rankLabel,
            ByteBufCodecs.BOOL, OrgEntry::viaJob,
            OrgEntry::new);

    public static final StreamCodec<ByteBuf, PlayerStatus> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_FULL_NAME), PlayerStatus::characterName,
            ByteBufCodecs.stringUtf8(MAX_TEXT), PlayerStatus::primaryCurrency,
            CURRENCY.apply(ByteBufCodecs.list(MAX_CURRENCIES)), PlayerStatus::currencies,
            BALANCE.apply(ByteBufCodecs.list(MAX_BALANCES)), PlayerStatus::balances,
            ByteBufCodecs.optional(JOB), PlayerStatus::job,
            ORG.apply(ByteBufCodecs.list(MAX_ORGS)), PlayerStatus::orgs,
            UiConfig.CODEC, PlayerStatus::ui,
            PlayerStatus::new);

    public PlayerStatus {
        currencies = List.copyOf(currencies);
        balances = List.copyOf(balances);
        orgs = List.copyOf(orgs);
    }

    public boolean hasCharacter() {
        return !characterName.isEmpty();
    }

    public Optional<BalanceEntry> primaryBalance() {
        if (primaryCurrency.isEmpty()) return Optional.empty();
        return balances.stream().filter(b -> b.currency().equals(primaryCurrency)).findFirst();
    }

    /** Importo con simbolo ("12.50 $"); valuta sconosciuta o definizione non valida: "importo CODICE". */
    public String format(BalanceEntry balance) {
        for (CurrencyInfo c : currencies) {
            if (!c.code().equals(balance.currency())) continue;
            try {
                return new Currency(c.code(), c.symbol(), c.decimals()).format(balance.amount());
            } catch (IllegalArgumentException e) {
                break;
            }
        }
        return balance.amount() + " " + balance.currency();
    }
}
