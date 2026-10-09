package dev.qrm.neoforge.server;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.Money;
import dev.qrm.api.character.CreateCharacterResult;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.Account;
import dev.qrm.api.economy.Currency;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.economy.TransferRequest;
import dev.qrm.api.economy.TransferResult;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.DutyReason;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
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
import dev.qrm.neoforge.net.PlayerStatus;
import dev.qrm.neoforge.net.UiConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerStatusBuilderTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);

    private Database db;
    private DefaultPlayerService players;
    private DefaultCharacterService chars;
    private DefaultAccountService accounts;
    private DefaultTransactionService txs;
    private DefaultJobService jobs;
    private InMemoryJobRegistry registry;
    private InMemoryCurrencyRegistry currencies;
    private DefaultOrganizationService orgs;
    private PlayerStatusBuilder builder;
    private PlayerId pid;
    private String primary = "USD";
    private UiConfig ui = UiConfig.DEFAULT;

    @BeforeEach
    void setUp() {
        db = Database.open("jdbc:sqlite::memory:");
        Migrator.migrate(db);
        SimpleEventBus bus = new SimpleEventBus();
        CoreConfig cfg = CoreConfig.defaults();
        players = new DefaultPlayerService(db, CLOCK, cfg);
        chars = new DefaultCharacterService(db, bus, CLOCK, cfg);
        accounts = new DefaultAccountService(db, bus, CLOCK);
        currencies = new InMemoryCurrencyRegistry();
        currencies.register(new Currency("USD", "$", 2));
        currencies.register(new Currency("EUR", "€", 2));
        currencies.register(new Currency("GBP", "£", 2));
        txs = new DefaultTransactionService(db, bus, CLOCK, currencies);
        registry = new InMemoryJobRegistry();
        jobs = new DefaultJobService(db, bus, registry, CLOCK);
        DefaultPermissionService permissions = new DefaultPermissionService(db, bus, registry, jobs);
        orgs = new DefaultOrganizationService(db, bus, accounts, registry, jobs, permissions, CLOCK);
        builder = new PlayerStatusBuilder(chars, accounts, currencies, jobs, registry, orgs, () -> primary,
                () -> ui);
        pid = new PlayerId(UUID.randomUUID());
        players.onJoin(pid);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private PlayerCharacter character() {
        return ((CreateCharacterResult.Created) chars.create(pid, "Anna", "Rossi", LocalDate.of(1990, 1, 1)))
                .character();
    }

    private void fund(PlayerCharacter pc, String currency, long amount) {
        Account acc = accounts.accountsOf(OwnerRef.character(pc.id())).stream().findFirst()
                .orElseGet(() -> accounts.create(OwnerRef.character(pc.id()), false));
        TransferResult r = txs.transfer(TransferRequest.builder()
                .from(accounts.systemAccount().id())
                .to(acc.id())
                .amount(Money.of(currency, amount))
                .reason("test.fund")
                .idempotencyKey("fund-" + UUID.randomUUID())
                .build());
        assertInstanceOf(TransferResult.Success.class, r);
    }

    private static JobDefinition police(String orgId) {
        return new JobDefinition("police", "Polizia",
                List.of(new GradeDefinition("cadet", "Recluta", 1, Set.of(), null)), orgId);
    }

    @Test
    void noActiveCharacterGivesTheEmptyStatus() {
        assertEquals(PlayerStatus.EMPTY, builder.build(pid));
        assertEquals(Optional.empty(), builder.activeCharacter(pid));
    }

    @Test
    void newCharacterHasAZeroBalanceInThePrimaryCurrency() {
        PlayerCharacter pc = character();
        PlayerStatus s = builder.build(pid);
        assertEquals("Anna Rossi", s.characterName());
        assertEquals("USD", s.primaryCurrency());
        assertEquals(List.of(new PlayerStatus.BalanceEntry("USD", 0)), s.balances());
        assertEquals(List.of(new PlayerStatus.CurrencyInfo("USD", "$", 2)), s.currencies());
        assertEquals(Optional.empty(), s.job());
        assertEquals(List.of(), s.orgs());
        assertEquals(Optional.of(pc.id()), builder.activeCharacter(pid));
    }

    @Test
    void primaryCurrencyComesFirstThenTheOthersByCode() {
        PlayerCharacter pc = character();
        fund(pc, "USD", 1250);
        fund(pc, "GBP", 100);
        fund(pc, "EUR", 500);
        assertEquals(List.of(new PlayerStatus.BalanceEntry("USD", 1250), new PlayerStatus.BalanceEntry("EUR", 500),
                new PlayerStatus.BalanceEntry("GBP", 100)), builder.build(pid).balances());
        primary = "GBP";
        assertEquals(List.of(new PlayerStatus.BalanceEntry("GBP", 100), new PlayerStatus.BalanceEntry("EUR", 500),
                new PlayerStatus.BalanceEntry("USD", 1250)), builder.build(pid).balances());
    }

    @Test
    void primaryCurrencyCodeIsCaseInsensitive() {
        character();
        primary = "usd";
        assertEquals("USD", builder.build(pid).primaryCurrency());
    }

    @Test
    void unknownPrimaryFallsBackToTheFirstRegisteredCurrencyByCode() {
        character();
        primary = "XXX";
        assertEquals("EUR", builder.build(pid).primaryCurrency());
    }

    @Test
    void emptyCurrencyRegistryMeansNoPrimaryAndNoBalances() {
        character();
        PlayerStatusBuilder bare = new PlayerStatusBuilder(chars, accounts, new InMemoryCurrencyRegistry(), jobs,
                registry, orgs, () -> "USD", () -> ui);
        PlayerStatus s = bare.build(pid);
        assertEquals("", s.primaryCurrency());
        assertEquals(List.of(), s.balances());
    }

    @Test
    void theConfiguredUiTravelsInTheStatus() {
        character();
        ui = UiConfig.of(List.of("orgs"), List.of("balance"), false);
        PlayerStatus s = builder.build(pid);
        assertEquals(List.of("orgs"), s.ui().hubTabs());
        assertEquals(List.of("balance"), s.ui().hudElements());
        assertFalse(s.ui().selfDuty());
    }

    @Test
    void knownJobShowsLabelsAndDuty() {
        PlayerCharacter pc = character();
        registry.register(police(null));
        jobs.assign(pc.id(), "police", "cadet");
        jobs.setOnDuty(pc.id(), true, DutyReason.MANUAL);
        assertEquals(Optional.of(new PlayerStatus.JobInfo("Polizia", "Recluta", true,
                CLOCK.instant().getEpochSecond(), true)), builder.build(pid).job());
    }

    @Test
    void unknownJobShowsTheRawIds() {
        PlayerCharacter pc = character();
        registry.register(police(null));
        jobs.assign(pc.id(), "police", "cadet");
        registry.remove("police");
        assertEquals(Optional.of(new PlayerStatus.JobInfo("police", "cadet", false,
                CLOCK.instant().getEpochSecond(), false)), builder.build(pid).job());
    }

    @Test
    void explicitMembershipShowsTheRankLabel() {
        PlayerCharacter pc = character();
        orgs.create("pd", "Polizia Distrettuale");
        orgs.defineRank("pd", new Rank("officer", "Agente", 1, Set.of()));
        orgs.addMember("pd", pc.id(), "officer");
        assertEquals(List.of(new PlayerStatus.OrgEntry("Polizia Distrettuale", "Agente", false)),
                builder.build(pid).orgs());
    }

    @Test
    void membershipImpliedByAnOnDutyJobIsMarkedViaJob() {
        PlayerCharacter pc = character();
        orgs.create("pd", "Polizia Distrettuale");
        registry.register(police("pd"));
        jobs.assign(pc.id(), "police", "cadet");
        jobs.setOnDuty(pc.id(), true, DutyReason.MANUAL);
        assertEquals(List.of(new PlayerStatus.OrgEntry("Polizia Distrettuale", "", true)), builder.build(pid).orgs());
    }

    @Test
    void balancesAreCappedWithThePrimaryFirst() {
        PlayerCharacter pc = character();
        for (int i = 1; i <= 20; i++) {
            String code = String.format("C%02d", i);
            currencies.register(new Currency(code, "c", 2));
            fund(pc, code, 100 + i);
        }
        fund(pc, "USD", 5);
        List<PlayerStatus.BalanceEntry> balances = builder.build(pid).balances();
        assertEquals(PlayerStatus.MAX_BALANCES, balances.size());
        assertEquals("USD", balances.get(0).currency());
        assertEquals("C01", balances.get(1).currency());
    }

    @Test
    void organizationsAreCapped() {
        PlayerCharacter pc = character();
        for (int i = 1; i <= 33; i++) {
            String id = String.format("org%02d", i);
            orgs.create(id, "Org " + i);
            orgs.defineRank(id, new Rank("member", "Membro", 1, Set.of()));
            orgs.addMember(id, pc.id(), "member");
        }
        assertEquals(PlayerStatus.MAX_ORGS, builder.build(pid).orgs().size());
    }

    @Test
    void longCurrencySymbolIsTruncated() {
        character();
        currencies.register(new Currency("LNG", "S".repeat(20), 2));
        primary = "LNG";
        List<PlayerStatus.CurrencyInfo> infos = builder.build(pid).currencies();
        assertEquals(1, infos.size());
        assertEquals("S".repeat(PlayerStatus.MAX_SYMBOL), infos.get(0).symbol());
    }
}
