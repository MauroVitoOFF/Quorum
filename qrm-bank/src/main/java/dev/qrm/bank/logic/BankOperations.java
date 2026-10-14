package dev.qrm.bank.logic;

import dev.qrm.api.Money;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.Account;
import dev.qrm.api.economy.AccountService;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.economy.Transaction;
import dev.qrm.api.economy.TransactionService;
import dev.qrm.api.economy.TransferRequest;
import dev.qrm.api.economy.TransferResult;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.id.AccountId;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.org.OrgStatus;
import dev.qrm.api.org.Organization;
import dev.qrm.api.org.OrganizationService;
import dev.qrm.bank.api.BankDepositEvent;
import dev.qrm.bank.api.BankTransferEvent;
import dev.qrm.bank.api.BankWithdrawEvent;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Regole del denaro della banca. Il contante in circolazione coincide col saldo della "cassa" (conto
 * {@code BANK_VAULT}, senza scoperto): il ritiro lo aumenta, il deposito lo diminuisce.
 */
public final class BankOperations {
    private static final System.Logger LOG = System.getLogger("qrm.bank");

    public static final String REASON_WITHDRAW = "bank.withdraw";
    public static final String REASON_DEPOSIT = "bank.deposit";
    public static final String REASON_REVERT = "bank.deposit.revert";
    public static final String REASON_TRANSFER = "bank.transfer";

    static final OwnerRef VAULT_OWNER = new OwnerRef(
            "BANK_VAULT", UUID.nameUUIDFromBytes("qrm-bank:vault".getBytes(StandardCharsets.UTF_8)));
    /** Quante transazioni recenti si leggono, al primo tentativo, per sommare i ritiri delle ultime 24 ore. */
    static final int HISTORY_SCAN = 500;
    static final int HISTORY_SCAN_MAX = 64_000;
    private static final Duration DAY = Duration.ofHours(24);

    private final AccountService accounts;
    private final TransactionService txs;
    private final CharacterService characters;
    private final OrganizationService orgs;
    private final EventBus bus;
    private final Clock clock;
    private final CashDenominations cash;
    private final LimitsProvider limits;
    private AccountId vault;

    public BankOperations(AccountService accounts, TransactionService txs, CharacterService characters,
            OrganizationService orgs, EventBus bus, Clock clock, CashDenominations cash, LimitsProvider limits) {
        this.accounts = accounts;
        this.txs = txs;
        this.characters = characters;
        this.orgs = orgs;
        this.bus = bus;
        this.clock = clock;
        this.cash = cash;
        this.limits = limits;
    }

    /** Conto bancario del personaggio; lo apre se non esiste. */
    public AccountId accountOf(CharacterId character) {
        OwnerRef owner = OwnerRef.character(character);
        List<Account> existing = accounts.accountsOf(owner);
        return (existing.isEmpty() ? accounts.create(owner, false) : existing.get(0)).id();
    }

    /** Conto cassa; lo crea al primo uso. */
    public synchronized AccountId vault() {
        if (vault == null) {
            List<Account> existing = accounts.accountsOf(VAULT_OWNER);
            vault = (existing.isEmpty() ? accounts.create(VAULT_OWNER, false) : existing.get(0)).id();
        }
        return vault;
    }

    public BankResult withdraw(CharacterId character, Money amount, CashInventory inventory) {
        if (amount.amount() <= 0) return BankResult.INVALID_AMOUNT;
        String currency = amount.currency();
        if (!cash.has(currency)) return BankResult.UNKNOWN_CURRENCY;
        Optional<Map<Long, Integer>> notes = CashBreakdown.of(cash.descending(currency), amount.amount());
        if (notes.isEmpty()) return BankResult.NOT_REPRESENTABLE;
        long noteCount = notes.get().values().stream().mapToLong(Integer::longValue).sum();
        if (noteCount > CashBreakdown.MAX_NOTES) return BankResult.INVENTORY_FULL;

        BankLimits limit = limits.forCurrency(currency);
        AccountId account = accountOf(character);
        if (amount.amount() > limit.perOperation()) return BankResult.LIMIT_EXCEEDED;
        if (withdrawnToday(account, currency) + amount.amount() > limit.perDay()) return BankResult.LIMIT_EXCEEDED;
        if (!inventory.canFit(currency, notes.get())) return BankResult.INVENTORY_FULL;

        AccountId vaultId = vault();
        TransferResult result = txs.transfer(request(account, vaultId, amount, REASON_WITHDRAW, key(character, "wd")));
        if (!(result instanceof TransferResult.Success success)) return map(result);
        if (success.transaction().amount().amount() != amount.amount()) {
            // Un altro mod ha cambiato l'importo nel pre-evento: il contante non corrisponderebbe, si annulla.
            revert(account, vaultId, success.transaction().amount(), character);
            return BankResult.CANCELLED;
        }
        inventory.give(currency, notes.get());
        bus.post(new BankWithdrawEvent(character, amount, success.transaction()));
        return BankResult.OK;
    }

    /** Deposita tutto il contante della valuta che il giocatore ha con sé. */
    public BankResult deposit(CharacterId character, String currency, CashInventory inventory) {
        if (!cash.has(currency)) return BankResult.UNKNOWN_CURRENCY;
        long held = inventory.count(currency);
        return held <= 0 ? BankResult.NO_CASH : deposit(character, currency, held, inventory);
    }

    /**
     * Deposita {@code units} del contante della valuta. Se il giocatore ne ha di più, tutto il contante viene ritirato
     * e il resto gli torna in banconote (se non entra nell'inventario cade a terra, come per {@code give}).
     */
    public BankResult deposit(CharacterId character, String currency, long units, CashInventory inventory) {
        if (units <= 0) return BankResult.INVALID_AMOUNT;
        if (!cash.has(currency)) return BankResult.UNKNOWN_CURRENCY;
        long held = inventory.count(currency);
        if (held <= 0 || units > held) return BankResult.NO_CASH;
        Map<Long, Integer> change = Map.of();
        if (held > units) {
            Optional<Map<Long, Integer>> notes = CashBreakdown.of(cash.descending(currency), held - units);
            if (notes.isEmpty()) return BankResult.NOT_REPRESENTABLE;
            if (notes.get().values().stream().mapToLong(Integer::longValue).sum() > CashBreakdown.MAX_NOTES) {
                return BankResult.INVENTORY_FULL;
            }
            change = notes.get();
        }
        AccountId vaultId = vault();
        if (accounts.balance(vaultId, currency).amount() < units) return BankResult.UNBACKED_CASH;

        AccountId account = accountOf(character);
        Money amount = Money.of(currency, units);
        TransferResult result = txs.transfer(request(vaultId, account, amount, REASON_DEPOSIT, key(character, "dp")));
        if (!(result instanceof TransferResult.Success success)) return map(result);
        if (success.transaction().amount().amount() != units) {
            revert(vaultId, account, success.transaction().amount(), character);
            return BankResult.CANCELLED;
        }
        if (!inventory.remove(currency, held)) {
            revert(vaultId, account, amount, character);
            return BankResult.INTERNAL_ERROR;
        }
        if (!change.isEmpty()) inventory.give(currency, change);
        bus.post(new BankDepositEvent(character, amount, success.transaction()));
        return BankResult.OK;
    }

    /**
     * Bonifico dal conto del personaggio.
     *
     * @param recipient "Nome Cognome" di un personaggio non archiviato, oppure "@idOrganizzazione"
     */
    public BankResult transfer(CharacterId from, String recipient, Money amount) {
        return transfer(from, recipient, amount, "");
    }

    /**
     * Bonifico con causale. La causale viene ripulita ({@link TransferNote#clean}) e salvata nei metadati della
     * transazione; a bonifico riuscito si pubblica {@link BankTransferEvent}.
     */
    public BankResult transfer(CharacterId from, String recipient, Money amount, String note) {
        if (amount.amount() <= 0) return BankResult.INVALID_AMOUNT;
        String name = recipient == null ? "" : recipient.strip();
        if (name.isEmpty()) return BankResult.UNKNOWN_RECIPIENT;
        AccountId target;
        CharacterId toCharacter = null;
        String toOrganization = null;
        if (name.startsWith("@")) {
            Optional<Organization> org;
            try {
                org = orgs.find(name.substring(1));
            } catch (IllegalArgumentException e) {
                return BankResult.UNKNOWN_RECIPIENT;
            }
            if (org.isEmpty() || org.get().status() != OrgStatus.ACTIVE) return BankResult.UNKNOWN_RECIPIENT;
            target = org.get().account();
            toOrganization = org.get().id();
        } else {
            List<PlayerCharacter> found = characters.findByName(name);
            if (found.isEmpty()) return BankResult.UNKNOWN_RECIPIENT;
            if (found.size() > 1) return BankResult.AMBIGUOUS_RECIPIENT;
            if (found.get(0).id().equals(from)) return BankResult.SELF_TRANSFER;
            toCharacter = found.get(0).id();
            target = accountOf(toCharacter);
        }
        AccountId source = accountOf(from);
        String cleanNote = TransferNote.clean(note);
        TransferRequest request = TransferRequest.builder().from(source).to(target).amount(amount)
                .reason(REASON_TRANSFER).idempotencyKey(key(from, "tr")).metadataJson(TransferNote.toJson(cleanNote))
                .build();
        TransferResult result = txs.transfer(request);
        if (result instanceof TransferResult.Success success) {
            bus.post(new BankTransferEvent(from, toCharacter, toOrganization, success.transaction().amount(),
                    cleanNote, success.transaction()));
        }
        return map(result);
    }

    /** Movimenti del conto del personaggio, dai più recenti. */
    public List<Transaction> history(CharacterId character, int limit) {
        return txs.history(accountOf(character), limit);
    }

    /**
     * Movimenti del conto come li vede il titolare: con verso (accredito/addebito), controparte (chi manda o riceve)
     * e causale.
     */
    public List<HistoryLine> historyLines(CharacterId character, int limit) {
        AccountId account = accountOf(character);
        Map<AccountId, String> names = new HashMap<>();
        List<HistoryLine> lines = new ArrayList<>();
        for (Transaction t : txs.history(account, limit)) {
            boolean incoming = t.to().equals(account);
            AccountId other = incoming ? t.from() : t.to();
            String counterparty = names.computeIfAbsent(other, this::counterpartyName);
            lines.add(new HistoryLine(t.createdAt().getEpochSecond(), t.reason(), t.amount().currency(),
                    incoming ? t.amount().amount() : -t.amount().amount(), counterparty,
                    TransferNote.fromJson(t.metadataJson())));
        }
        return lines;
    }

    /** Nome del titolare di un conto per lo storico; vuoto per la banca, il sistema e i conti sconosciuti. */
    private String counterpartyName(AccountId id) {
        Optional<Account> account = accounts.find(id);
        if (account.isEmpty()) return "";
        OwnerRef owner = account.get().owner();
        if (OwnerRef.CHARACTER.equals(owner.type())) {
            return characters.find(new CharacterId(owner.id()))
                    .map(pc -> pc.firstName() + " " + pc.lastName()).orElse("");
        }
        if (OwnerRef.ORGANIZATION.equals(owner.type())) {
            return orgs.all().stream().filter(o -> o.account().equals(id)).map(Organization::label).findFirst()
                    .orElse("");
        }
        return "";
    }

    /** Saldi positivi del conto del personaggio. */
    public List<Money> balances(CharacterId character) {
        return accounts.balances(accountOf(character));
    }

    /**
     * Storno di un trasferimento {@code from → to} appena eseguito: muove {@code amount} da {@code to} a
     * {@code from} con un motivo dedicato.
     */
    private void revert(AccountId from, AccountId to, Money amount, CharacterId character) {
        TransferResult result = txs.transfer(request(to, from, amount, REASON_REVERT, key(character, "rv")));
        if (!(result instanceof TransferResult.Success)) {
            LOG.log(System.Logger.Level.ERROR, "Bank revert failed for character " + character.value() + " (" + amount
                    + ", " + result + "): ledger and cash are out of sync, manual correction needed");
        }
    }

    /**
     * Somma dei ritiri delle ultime 24 ore. Legge le transazioni più recenti raddoppiando la finestra (partendo da
     * {@value #HISTORY_SCAN}) finché non arriva a una più vecchia di 24 ore o all'inizio dello storico: altrimenti
     * molti movimenti recenti spingerebbero i ritiri fuori dalla finestra e il limite giornaliero non varrebbe più.
     * Oltre {@value #HISTORY_SCAN_MAX} movimenti in un giorno il conto è considerato al limite.
     */
    long withdrawnToday(AccountId account, String currency) {
        Instant cutoff = clock.instant().minus(DAY);
        int window = HISTORY_SCAN;
        List<Transaction> recent = txs.history(account, window);
        while (recent.size() == window && recent.get(recent.size() - 1).createdAt().isAfter(cutoff)) {
            if (window >= HISTORY_SCAN_MAX) return Long.MAX_VALUE / 2; // margine contro l'overflow nella somma col nuovo importo
            window *= 2;
            recent = txs.history(account, window);
        }
        long sum = 0;
        for (Transaction t : recent) {
            if (REASON_WITHDRAW.equals(t.reason()) && t.from().equals(account)
                    && currency.equals(t.amount().currency()) && t.createdAt().isAfter(cutoff)) {
                sum += t.amount().amount();
            }
        }
        return sum;
    }

    static TransferRequest request(AccountId from, AccountId to, Money amount, String reason, String key) {
        return TransferRequest.builder().from(from).to(to).amount(amount).reason(reason).idempotencyKey(key).build();
    }

    static String key(CharacterId character, String op) {
        return "bank:" + character.value() + ":" + op + ":" + UUID.randomUUID();
    }

    static BankResult map(TransferResult result) {
        return switch (result) {
            case TransferResult.Success success -> BankResult.OK;
            case TransferResult.InsufficientFunds insufficient -> BankResult.INSUFFICIENT_FUNDS;
            case TransferResult.AccountFrozen frozen -> BankResult.FROZEN;
            case TransferResult.Cancelled cancelled -> BankResult.CANCELLED;
            case TransferResult.UnknownCurrency unknown -> BankResult.UNKNOWN_CURRENCY;
            case TransferResult.InvalidRequest invalid -> BankResult.INVALID_AMOUNT;
            case TransferResult.Duplicate duplicate -> BankResult.INTERNAL_ERROR;
            case TransferResult.AccountNotFound notFound -> BankResult.INTERNAL_ERROR;
        };
    }
}
