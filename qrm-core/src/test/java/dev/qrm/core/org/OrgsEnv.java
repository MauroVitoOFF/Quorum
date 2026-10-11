package dev.qrm.core.org;

import dev.qrm.api.Money;
import dev.qrm.api.character.CreateCharacterResult;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.Account;
import dev.qrm.api.economy.Currency;
import dev.qrm.api.economy.TransferRequest;
import dev.qrm.api.economy.TransferResult;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.org.MembershipChangedEvent;
import dev.qrm.api.org.OrgError;
import dev.qrm.api.org.OrgResult;
import dev.qrm.api.org.OrganizationArchivedEvent;
import dev.qrm.api.org.OrganizationUnarchivedEvent;
import dev.qrm.api.org.OrganizationCreatedEvent;
import dev.qrm.core.CoreConfig;
import dev.qrm.core.TestClock;
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
import dev.qrm.core.player.DefaultPlayerService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class OrgsEnv implements AutoCloseable {
    private static final List<String> FIRST_NAMES =
            List.of("Anna", "Bruno", "Carla", "Dario", "Elena", "Fabio", "Giulia", "Hugo", "Irene", "Luca");

    public final Database db = Database.open("jdbc:sqlite::memory:");
    public final SimpleEventBus bus = new SimpleEventBus();
    public final TestClock clock = new TestClock(Instant.parse("2026-01-01T00:00:00Z"));
    public final InMemoryCurrencyRegistry currencies = new InMemoryCurrencyRegistry();
    public final InMemoryJobRegistry registry = new InMemoryJobRegistry();
    public final DefaultPlayerService players;
    public final DefaultCharacterService chars;
    public final DefaultAccountService accounts;
    public final DefaultTransactionService txs;
    public final DefaultJobService jobs;
    public final DefaultPermissionService permissions;
    public final DefaultOrganizationService orgs;
    public final DefaultOrganizationBankService bank;
    public final List<Object> events = new ArrayList<>();
    private int counter;

    public OrgsEnv() {
        Migrator.migrate(db);
        currencies.register(new Currency("USD", "$", 2));
        CoreConfig cfg = CoreConfig.defaults();
        players = new DefaultPlayerService(db, clock, cfg);
        chars = new DefaultCharacterService(db, bus, clock, cfg);
        accounts = new DefaultAccountService(db, bus, clock);
        txs = new DefaultTransactionService(db, bus, clock, currencies);
        jobs = new DefaultJobService(db, bus, registry, clock);
        permissions = new DefaultPermissionService(db, bus, registry, jobs);
        orgs = new DefaultOrganizationService(db, bus, accounts, registry, jobs, permissions, clock);
        bank = new DefaultOrganizationBankService(orgs, txs);
        registry.register(policeLinkedToPd());
        bus.register(OrganizationCreatedEvent.class, "t", events::add);
        bus.register(OrganizationArchivedEvent.class, "t", events::add);
        bus.register(OrganizationUnarchivedEvent.class, "t", events::add);
        bus.register(MembershipChangedEvent.class, "t", events::add);
    }

    /** Job "police" collegato all'organizzazione "pd"; il grade "chief" concede il prelievo. */
    public static JobDefinition policeLinkedToPd() {
        return new JobDefinition("police", "Polizia", List.of(
                new GradeDefinition("cadet", "Recluta", 1, Set.of("police.duty"), null),
                new GradeDefinition("chief", "Comandante", 5, Set.of("org.account.withdraw", "police.*"), null)), "pd");
    }

    public PlayerCharacter newCharacter() {
        return newCharacter(new PlayerId(UUID.randomUUID()));
    }

    /** Un personaggio ACTIVE per il giocatore indicato (lo registra se serve). Al massimo 10 per fixture. */
    public PlayerCharacter newCharacter(PlayerId player) {
        players.onJoin(player);
        String first = FIRST_NAMES.get(counter++);
        return ((CreateCharacterResult.Created) chars.create(player, first, "Rossi", LocalDate.of(1990, 1, 1)))
                .character();
    }

    /** Accredita denaro dal conto di sistema. */
    public void fund(Account account, String currency, long amount) {
        TransferResult r = txs.transfer(TransferRequest.builder()
                .from(accounts.systemAccount().id())
                .to(account.id())
                .amount(Money.of(currency, amount))
                .reason("test.fund")
                .idempotencyKey("fund-" + UUID.randomUUID())
                .build());
        if (!(r instanceof TransferResult.Success)) throw new IllegalStateException("fund failed: " + r);
    }

    public static <T> T ok(OrgResult<T> r) {
        if (r instanceof OrgResult.Ok<T> o) return o.value();
        throw new AssertionError("expected Ok but was " + r);
    }

    public static OrgError err(OrgResult<?> r) {
        if (r instanceof OrgResult.Failed<?> f) return f.error();
        throw new AssertionError("expected Failed but was " + r);
    }

    @Override
    public void close() {
        db.close();
    }
}
