package dev.qrm.core;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.Money;
import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.CreateCharacterResult;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.Account;
import dev.qrm.api.economy.AccountService;
import dev.qrm.api.economy.Currency;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.economy.TransactionService;
import dev.qrm.api.economy.TransferRequest;
import dev.qrm.api.economy.TransferResult;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.AssignResult;
import dev.qrm.api.job.DutyReason;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.JobService;
import dev.qrm.api.org.*;
import dev.qrm.api.player.PlayerService;
import dev.qrm.core.economy.InMemoryCurrencyRegistry;
import dev.qrm.core.event.SimpleEventBus;
import dev.qrm.core.job.InMemoryJobRegistry;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QrmCoreOrgsTest {
    private QrmCore core;
    private SimpleEventBus bus;

    @AfterEach
    void tearDown() {
        if (core != null) core.close();
        QRM.clear();
    }

    private void start() {
        bus = new SimpleEventBus();
        InMemoryCurrencyRegistry currencies = new InMemoryCurrencyRegistry();
        currencies.register(new Currency("USD", "$", 2));
        InMemoryJobRegistry jobs = new InMemoryJobRegistry();
        jobs.register(new JobDefinition("police", "Polizia", List.of(
                new GradeDefinition("chief", "Comandante", 5, Set.of("org.account.withdraw"), null)), "pd"));
        QRM.register(EventBus.class, bus);
        QRM.register(CurrencyRegistry.class, currencies);
        core = QrmCore.start(bus, currencies, jobs, "jdbc:sqlite::memory:", null, null, CoreConfig.defaults(),
                Clock.systemUTC());
    }

    private PlayerCharacter character(PlayerId pid, String first) {
        return ((CreateCharacterResult.Created) QRM.get(CharacterService.class)
                .create(pid, first, "Rossi", LocalDate.of(1990, 1, 1))).character();
    }

    private void fund(Account to, long amount) {
        TransferResult r = QRM.get(TransactionService.class).transfer(TransferRequest.builder()
                .from(QRM.get(AccountService.class).systemAccount().id()).to(to.id())
                .amount(Money.of("USD", amount)).reason("test.fund").idempotencyKey("f-" + UUID.randomUUID()).build());
        assertInstanceOf(TransferResult.Success.class, r);
    }

    @Test
    void organizationLifecycleAndWithdrawalEndToEnd() {
        start();
        PlayerId pid = new PlayerId(UUID.randomUUID());
        QRM.get(PlayerService.class).onJoin(pid);
        PlayerCharacter anna = character(pid, "Anna");
        OrganizationService orgs = QRM.get(OrganizationService.class);

        Organization org = ((OrgResult.Ok<Organization>) orgs.create("acme", "Acme")).value();
        orgs.defineRank("acme", new Rank("boss", "Capo", 5, Set.of("org.account.withdraw")));
        assertInstanceOf(OrgResult.Ok.class, orgs.addMember("acme", anna.id(), "boss"));
        fund(QRM.get(AccountService.class).find(org.account()).orElseThrow(), 10_000);

        Account annaAccount = QRM.get(AccountService.class).accountsOf(OwnerRef.character(anna.id())).get(0);
        BankResult r = QRM.get(OrganizationBankService.class)
                .transfer(anna.id(), "acme", annaAccount.id(), Money.of("USD", 4_000), "w-1");
        assertInstanceOf(TransferResult.Success.class, ((BankResult.Transferred) r).result());
        assertEquals(Money.of("USD", 4_000), QRM.get(AccountService.class).balance(annaAccount.id(), "USD"));
        assertEquals(Money.of("USD", 6_000), QRM.get(AccountService.class).balance(org.account(), "USD"));
    }

    @Test
    void jobLinkedOrganizationFollowsDutyAndCharacterSwitch() {
        start();
        PlayerId pid = new PlayerId(UUID.randomUUID());
        QRM.get(PlayerService.class).onJoin(pid);
        PlayerCharacter c1 = character(pid, "Anna");
        PlayerCharacter c2 = character(pid, "Bruno");
        OrganizationService orgs = QRM.get(OrganizationService.class);
        orgs.create("pd", "Polizia");
        JobService js = QRM.get(JobService.class);
        assertInstanceOf(AssignResult.Assigned.class, js.assign(c1.id(), "police", "chief"));

        assertFalse(orgs.can(c1.id(), "pd", "org.account.withdraw"));
        assertTrue(js.setOnDuty(c1.id(), true, DutyReason.MANUAL));
        assertTrue(orgs.can(c1.id(), "pd", "org.account.withdraw"));

        QRM.get(CharacterService.class).select(pid, c2.id());   // l'altro personaggio va fuori servizio
        assertFalse(orgs.can(c1.id(), "pd", "org.account.withdraw"));
        assertEquals(c2.id(), QRM.get(CharacterService.class).activeOf(pid).orElseThrow().id());

        QRM.get(CharacterService.class).select(pid, c1.id());
        assertTrue(js.setOnDuty(c1.id(), true, DutyReason.MANUAL));
        assertTrue(orgs.can(c1.id(), "pd", "org.account.withdraw"));
    }

    @Test
    void closeUnregistersOrganizationServices() {
        start();
        core.close();
        core = null;
        assertThrows(IllegalStateException.class, () -> QRM.get(OrganizationService.class));
        assertThrows(IllegalStateException.class, () -> QRM.get(OrganizationBankService.class));
    }
}
