package dev.qrm.bank.logic;

import dev.qrm.api.Money;
import dev.qrm.api.economy.AccountService;
import dev.qrm.api.economy.AccountStatus;
import dev.qrm.api.economy.AuditService;
import dev.qrm.api.economy.Currency;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.economy.Transaction;
import dev.qrm.api.economy.TransactionService;
import dev.qrm.api.economy.TransferRequest;
import dev.qrm.api.economy.TransferResult;
import dev.qrm.api.id.AccountId;
import dev.qrm.api.id.CharacterId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.ToLongFunction;

/**
 * Logica amministrativa della banca (stato e storico del conto, denaro dato o tolto, congelamento, audit), senza
 * dipendenze da Minecraft. È condivisa dai comandi {@code /bank ...} e dal menu staff, che aggiungono solo il controllo
 * del permesso. Dare e togliere denaro fa gli stessi movimenti di {@code /money give|take} (conto di sistema,
 * motivi {@code admin.grant} e {@code admin.take}).
 */
public final class StaffBankLogic {
    public static final String REASON_GRANT = "admin.grant";
    public static final String REASON_TAKE = "admin.take";
    /** Movimenti mostrati nello storico del bersaglio. */
    public static final int HISTORY_LIMIT = 16;

    /** Un saldo già formattato con la valuta (es. {@code 12.50 $}). */
    public record Line(String currency, String text) {}

    /** Un movimento del conto: {@code amount} è già formattato e con il segno (+ entrate, - uscite). */
    public record Move(long epochSecond, String amount, String reason) {}

    /** Contante in mano ai giocatori online superiore a quello della cassa, già formattato. */
    public record Excess(String currency, String held, String vault) {}

    public record State(boolean frozen, List<Line> balances, List<Move> history) {
        public State {
            balances = List.copyOf(balances);
            history = List.copyOf(history);
        }
    }

    public record AuditResult(int issues, List<Excess> excess) {
        public AuditResult {
            excess = List.copyOf(excess);
        }

        public boolean clean() {
            return issues == 0 && excess.isEmpty();
        }
    }

    /** Esito di {@link #adjust}. */
    public enum AdjustResult { OK, INSUFFICIENT_FUNDS, FROZEN, UNKNOWN_CURRENCY, INVALID, REFUSED }

    private final BankOperations ops;
    private final AccountService accounts;
    private final TransactionService transactions;
    private final AuditService audit;
    private final CurrencyRegistry currencies;

    public StaffBankLogic(BankOperations ops, AccountService accounts, TransactionService transactions,
            AuditService audit, CurrencyRegistry currencies) {
        this.ops = ops;
        this.accounts = accounts;
        this.transactions = transactions;
        this.audit = audit;
        this.currencies = currencies;
    }

    public String format(Money money) {
        return currencies.find(money.currency()).map(c -> c.format(money.amount()))
                .orElse(money.amount() + " " + money.currency());
    }

    /** Codici delle valute registrate, in ordine alfabetico. */
    public List<String> currencyCodes() {
        return currencies.all().stream().map(Currency::code).sorted().toList();
    }

    /** Stato del conto bancario del personaggio (lo apre se non esiste, come {@code /bank balance}). */
    public State state(CharacterId character) {
        AccountId id = ops.accountOf(character);
        boolean frozen = accounts.find(id).map(a -> a.status() == AccountStatus.FROZEN).orElse(false);
        List<Line> lines = new ArrayList<>();
        for (Money m : accounts.balances(id)) lines.add(new Line(m.currency(), format(m)));
        List<Move> moves = new ArrayList<>();
        for (Transaction t : transactions.history(id, HISTORY_LIMIT)) {
            String sign = t.to().equals(id) ? "+" : "-";
            moves.add(new Move(t.createdAt().getEpochSecond(), sign + format(t.amount()), t.reason()));
        }
        return new State(frozen, lines, moves);
    }

    public void setFrozen(CharacterId character, boolean frozen) {
        accounts.setStatus(ops.accountOf(character), frozen ? AccountStatus.FROZEN : AccountStatus.ACTIVE);
    }

    /**
     * Dà ({@code grant}) o toglie denaro al conto del personaggio, dal/verso il conto di sistema. Come ogni
     * trasferimento, togliere non porta il conto in negativo e un conto congelato non si muove.
     */
    public AdjustResult adjust(CharacterId character, boolean grant, String amountText, String currencyCode) {
        Optional<Currency> currency = currencies.find(currencyCode.strip().toUpperCase(Locale.ROOT));
        if (currency.isEmpty()) return AdjustResult.UNKNOWN_CURRENCY;
        long amount;
        try {
            amount = currency.get().parseAmount(amountText);
        } catch (IllegalArgumentException e) {
            return AdjustResult.INVALID;
        }
        if (amount <= 0) return AdjustResult.INVALID;
        AccountId account = ops.accountOf(character);
        AccountId system = accounts.systemAccount().id();
        TransferResult r = transactions.transfer(TransferRequest.builder()
                .from(grant ? system : account).to(grant ? account : system)
                .amount(Money.of(currency.get().code(), amount)).reason(grant ? REASON_GRANT : REASON_TAKE)
                .idempotencyKey(UUID.randomUUID().toString()).build());
        return switch (r) {
            case TransferResult.Success s -> AdjustResult.OK;
            case TransferResult.Duplicate d -> AdjustResult.OK;
            case TransferResult.InsufficientFunds f -> AdjustResult.INSUFFICIENT_FUNDS;
            case TransferResult.AccountNotFound n -> AdjustResult.REFUSED;
            case TransferResult.AccountFrozen z -> AdjustResult.FROZEN;
            case TransferResult.UnknownCurrency u -> AdjustResult.UNKNOWN_CURRENCY;
            case TransferResult.InvalidRequest i -> AdjustResult.INVALID;
            case TransferResult.Cancelled x -> AdjustResult.REFUSED;
        };
    }

    /**
     * Coerenza del registro e confronto fra il contante dei giocatori online e il saldo della cassa.
     *
     * @param cashCurrencies le valute con contante
     * @param heldCash per ogni valuta, il contante totale in mano ai giocatori online
     */
    public AuditResult audit(Collection<String> cashCurrencies, ToLongFunction<String> heldCash) {
        int issues = audit.verify().size();
        List<Excess> excess = new ArrayList<>();
        for (String currency : cashCurrencies) {
            long held = heldCash.applyAsLong(currency);
            long vault = accounts.balance(ops.vault(), currency).amount();
            if (held > vault) {
                excess.add(new Excess(currency, format(Money.of(currency, held)), format(Money.of(currency, vault))));
            }
        }
        return new AuditResult(issues, excess);
    }
}
