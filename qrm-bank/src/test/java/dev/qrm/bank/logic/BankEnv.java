package dev.qrm.bank.logic;

import dev.qrm.api.Money;
import dev.qrm.api.character.CreateCharacterResult;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.Currency;
import dev.qrm.api.economy.TransferRequest;
import dev.qrm.api.economy.TransferResult;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.core.CoreConfig;
import dev.qrm.core.character.DefaultCharacterService;
import dev.qrm.core.db.Database;
import dev.qrm.core.db.Migrator;
import dev.qrm.core.economy.DefaultAccountService;
import dev.qrm.core.economy.DefaultAuditService;
import dev.qrm.core.economy.DefaultTransactionService;
import dev.qrm.core.economy.InMemoryCurrencyRegistry;
import dev.qrm.core.event.SimpleEventBus;
import dev.qrm.core.job.DefaultJobService;
import dev.qrm.core.job.DefaultPermissionService;
import dev.qrm.core.job.InMemoryJobRegistry;
import dev.qrm.core.org.DefaultOrganizationService;
import dev.qrm.core.player.DefaultPlayerService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Servizi veri di qrm-core su SQLite in memoria, più la logica della banca e un inventario finto. */
final class BankEnv implements AutoCloseable {
    static final String USD = "USD";
    private static final List<String> FIRST_NAMES =
            List.of("Anna", "Bruno", "Carla", "Dario", "Elena", "Fabio", "Giulia", "Hugo", "Irene", "Luca");

    final Database db = Database.open("jdbc:sqlite::memory:");
    final SimpleEventBus bus = new SimpleEventBus();
    final BankTestClock clock = new BankTestClock(Instant.parse("2026-01-01T00:00:00Z"));
    final InMemoryCurrencyRegistry currencies = new InMemoryCurrencyRegistry();
    final InMemoryJobRegistry registry = new InMemoryJobRegistry();
    final DefaultPlayerService players;
    final DefaultCharacterService chars;
    final DefaultAccountService accounts;
    final DefaultTransactionService txs;
    final DefaultJobService jobs;
    final DefaultPermissionService permissions;
    final DefaultOrganizationService orgs;
    final DefaultAuditService audit;
    final CashDenominations cash = CashDenominations.of(
            Map.of(USD, new CashDenominations.Spec(2, List.of(100L, 500L, 1000L))));
    final BankOperations ops;
    final FakeCash inventory = new FakeCash();
    final List<Object> events = new ArrayList<>();
    private int counter;

    BankEnv() {
        this(true);
    }

    BankEnv(boolean uniqueNames) {
        Migrator.migrate(db);
        currencies.register(new Currency(USD, "$", 2));
        CoreConfig d = CoreConfig.defaults();
        CoreConfig cfg = new CoreConfig(
                d.defaultCharacterSlots(), d.nameMinLength(), d.nameMaxLength(), d.namePattern(), uniqueNames);
        players = new DefaultPlayerService(db, clock, cfg);
        chars = new DefaultCharacterService(db, bus, clock, cfg);
        accounts = new DefaultAccountService(db, bus, clock);
        txs = new DefaultTransactionService(db, bus, clock, currencies);
        jobs = new DefaultJobService(db, bus, registry, clock);
        permissions = new DefaultPermissionService(db, bus, registry, jobs);
        orgs = new DefaultOrganizationService(db, bus, accounts, registry, jobs, permissions, clock);
        audit = new DefaultAuditService(db);
        ops = new BankOperations(accounts, txs, chars, orgs, bus, clock, cash,
                LimitsProvider.of(new BankLimits(5_000, 8_000), List.of()));
        bus.register(dev.qrm.bank.api.BankWithdrawEvent.class, "t", events::add);
        bus.register(dev.qrm.bank.api.BankDepositEvent.class, "t", events::add);
        bus.register(dev.qrm.bank.api.BankTransferEvent.class, "t", events::add);
    }

    PlayerCharacter newCharacter() {
        return newCharacterNamed(FIRST_NAMES.get(counter++), "Rossi");
    }

    PlayerCharacter newCharacterNamed(String first, String last) {
        PlayerId player = new PlayerId(UUID.randomUUID());
        players.onJoin(player);
        return ((CreateCharacterResult.Created) chars.create(player, first, last, LocalDate.of(1990, 1, 1)))
                .character();
    }

    /** Accredita denaro dal conto di sistema sul conto bancario del personaggio. */
    void fund(CharacterId character, long amount) {
        TransferResult r = txs.transfer(TransferRequest.builder()
                .from(accounts.systemAccount().id())
                .to(ops.accountOf(character))
                .amount(Money.of(USD, amount))
                .reason("test.fund")
                .idempotencyKey("fund-" + UUID.randomUUID())
                .build());
        if (!(r instanceof TransferResult.Success)) throw new IllegalStateException("fund failed: " + r);
    }

    long balance(CharacterId character) {
        return accounts.balance(ops.accountOf(character), USD).amount();
    }

    long vaultBalance() {
        return accounts.balance(ops.vault(), USD).amount();
    }

    @Override
    public void close() {
        db.close();
    }
}
