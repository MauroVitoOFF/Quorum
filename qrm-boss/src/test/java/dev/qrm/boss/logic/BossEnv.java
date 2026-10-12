package dev.qrm.boss.logic;

import dev.qrm.api.Money;
import dev.qrm.api.character.CreateCharacterResult;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.Account;
import dev.qrm.api.economy.Currency;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.economy.TransferRequest;
import dev.qrm.api.economy.TransferResult;
import dev.qrm.api.id.AccountId;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.org.Organization;
import dev.qrm.api.org.Rank;
import dev.qrm.core.CoreConfig;
import dev.qrm.core.character.DefaultCharacterService;
import dev.qrm.core.db.Database;
import dev.qrm.core.db.Migrator;
import dev.qrm.core.economy.DefaultAccountService;
import dev.qrm.core.economy.DefaultTransactionService;
import dev.qrm.core.economy.InMemoryCurrencyRegistry;
import dev.qrm.core.event.SimpleEventBus;
import dev.qrm.core.job.DefaultJobService;
import dev.qrm.core.job.DefaultPermissionService;
import dev.qrm.core.job.InMemoryJobRegistry;
import dev.qrm.core.org.DefaultOrganizationService;
import dev.qrm.core.player.DefaultPlayerService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Servizi veri di qrm-core su SQLite in memoria, più il {@link BossService}. */
final class BossEnv implements AutoCloseable {
    static final String USD = "USD";
    static final String ORG = "acme";
    private static final List<String> FIRST_NAMES =
            List.of("Anna", "Bruno", "Carla", "Dario", "Elena", "Fabio", "Giulia", "Hugo", "Irene", "Luca");

    final Database db = Database.open("jdbc:sqlite::memory:");
    final SimpleEventBus bus = new SimpleEventBus();
    final InMemoryCurrencyRegistry currencies = new InMemoryCurrencyRegistry();
    final InMemoryJobRegistry registry = new InMemoryJobRegistry();
    final DefaultPlayerService players;
    final DefaultCharacterService chars;
    final DefaultAccountService accounts;
    final DefaultTransactionService txs;
    final DefaultJobService jobs;
    final DefaultPermissionService permissions;
    final DefaultOrganizationService orgs;
    final BossService service;
    private int counter;

    BossEnv() {
        Migrator.migrate(db);
        currencies.register(new Currency(USD, "$", 2));
        Clock clock = Clock.systemUTC();
        CoreConfig cfg = CoreConfig.defaults();
        players = new DefaultPlayerService(db, clock, cfg);
        chars = new DefaultCharacterService(db, bus, clock, cfg);
        accounts = new DefaultAccountService(db, bus, clock);
        txs = new DefaultTransactionService(db, bus, clock, currencies);
        jobs = new DefaultJobService(db, bus, registry, clock);
        permissions = new DefaultPermissionService(db, bus, registry, jobs);
        orgs = new DefaultOrganizationService(db, bus, accounts, registry, jobs, permissions, clock);
        service = new BossService(orgs, new dev.qrm.core.org.DefaultOrganizationBankService(orgs, txs), accounts, txs,
                chars, currencies, jobs, registry);
        orgs.create(ORG, "Acme Inc");
        orgs.defineRank(ORG, new Rank("worker", "Operaio", 10, Set.of()));
        orgs.defineRank(ORG, new Rank("manager", "Manager", 20, Set.of("boss.menu", "boss.members.manage")));
        orgs.defineRank(ORG, new Rank("boss", "Boss", 30, Set.of("boss.*", "org.account.*")));
    }

    PlayerCharacter newCharacter() {
        int n = counter++;
        return newCharacterNamed(FIRST_NAMES.get(n % FIRST_NAMES.size()), "Rossi" + "abcdefghijklmnopqrstuvwxyz".charAt(n % 26));
    }

    PlayerCharacter newCharacterNamed(String first, String last) {
        PlayerId player = new PlayerId(UUID.randomUUID());
        players.onJoin(player);
        return ((CreateCharacterResult.Created) chars.create(player, first, last, LocalDate.of(1990, 1, 1)))
                .character();
    }

    /** Un Character membro dell'azienda con il rank dato. */
    PlayerCharacter member(String rankId) {
        PlayerCharacter c = newCharacter();
        orgs.addMember(ORG, c.id(), rankId);
        return c;
    }

    Organization org() {
        return orgs.find(ORG).orElseThrow();
    }

    AccountId orgAccount() {
        return org().account();
    }

    /** Conto personale con il saldo dato. */
    AccountId personal(CharacterId id, long amount) {
        Account account = accounts.create(OwnerRef.character(id), false);
        if (amount > 0) fund(account.id(), amount);
        return account.id();
    }

    void fund(AccountId to, long amount) {
        TransferResult r = txs.transfer(TransferRequest.builder()
                .from(accounts.systemAccount().id())
                .to(to)
                .amount(Money.of(USD, amount))
                .reason("test.fund")
                .idempotencyKey("fund-" + UUID.randomUUID())
                .build());
        if (!(r instanceof TransferResult.Success)) throw new IllegalStateException("fund failed: " + r);
    }

    long balance(AccountId id) {
        return accounts.balance(id, USD).amount();
    }

    @Override
    public void close() {
        db.close();
    }
}
