package dev.qrm.core.org;

import static dev.qrm.core.org.OrgsEnv.err;
import static dev.qrm.core.org.OrgsEnv.ok;
import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.economy.Account;
import dev.qrm.api.economy.AccountStatus;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.org.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizationServiceTest {
    private OrgsEnv env;

    @BeforeEach
    void setUp() {
        env = new OrgsEnv();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    @Test
    void createMakesTheOrganizationItsAccountAndAnEvent() {
        Organization org = ok(env.orgs.create("acme", "Acme"));
        assertEquals("acme", org.id());
        assertEquals(OrgStatus.ACTIVE, org.status());
        assertEquals(org, env.orgs.find("acme").orElseThrow());
        assertEquals(OwnerRef.ORGANIZATION, env.accounts.find(org.account()).orElseThrow().owner().type());
        assertEquals(1, env.accounts.accountsOf(Organization.ownerOf("acme")).size());
        assertEquals(List.of(new OrganizationCreatedEvent(org)), env.events);
    }

    @Test
    void duplicateCreateIsRejectedWithoutCreatingAnotherAccount() {
        ok(env.orgs.create("acme", "Acme"));
        assertEquals(new OrgError.Exists("acme"), err(env.orgs.create("acme", "Other")));
        assertEquals(1, env.accounts.accountsOf(Organization.ownerOf("acme")).size());
        assertEquals("Acme", env.orgs.find("acme").orElseThrow().label());
    }

    @Test
    void createReusesAnOrphanAccountLeftByAnInterruptedCreate() {
        Account orphan = env.accounts.create(Organization.ownerOf("acme"), false);
        Organization org = ok(env.orgs.create("acme", "Acme"));
        assertEquals(orphan.id(), org.account());
        assertEquals(1, env.accounts.accountsOf(Organization.ownerOf("acme")).size());
        assertEquals(new OrgError.Exists("acme"), err(env.orgs.create("acme", "Again")));
        assertEquals(1, env.accounts.accountsOf(Organization.ownerOf("acme")).size());
    }

    @Test
    void malformedIdOrLabelThrows() {
        assertThrows(IllegalArgumentException.class, () -> env.orgs.create("Bad Id", "X"));
        assertThrows(IllegalArgumentException.class, () -> env.orgs.create("ok", " "));
    }

    @Test
    void allIsSortedById() {
        ok(env.orgs.create("zeta", "Z"));
        ok(env.orgs.create("alpha", "A"));
        assertEquals(List.of("alpha", "zeta"), env.orgs.all().stream().map(Organization::id).toList());
    }

    @Test
    void archiveFreezesTheAccountIsIdempotentAndEmitsOnce() {
        Organization org = ok(env.orgs.create("acme", "Acme"));
        env.events.clear();
        Organization archived = ok(env.orgs.archive("acme"));
        assertEquals(OrgStatus.ARCHIVED, archived.status());
        assertEquals(AccountStatus.FROZEN, env.accounts.find(org.account()).orElseThrow().status());
        assertEquals(List.of(new OrganizationArchivedEvent(archived)), env.events);
        env.events.clear();
        assertEquals(archived, ok(env.orgs.archive("acme")));
        assertEquals(List.of(), env.events);
        assertEquals(new OrgError.UnknownOrg("nope"), err(env.orgs.archive("nope")));
    }

    @Test
    void renameMovesRanksMembersAndAccountOwnerAndFreesTheOldId() {
        Organization org = ok(env.orgs.create("acme", "Acme"));
        ok(env.orgs.defineRank("acme", new Rank("boss", "Boss", 1, Set.of("org.account.withdraw"))));
        var member = env.newCharacter().id();
        ok(env.orgs.addMember("acme", member, "boss"));

        Organization renamed = ok(env.orgs.rename("acme", "beta", "Beta"));

        assertEquals("beta", renamed.id());
        assertEquals("Beta", renamed.label());
        assertEquals(org.account(), renamed.account());
        assertEquals(OrgStatus.ACTIVE, renamed.status());
        assertTrue(env.orgs.find("acme").isEmpty());
        assertEquals(List.of("boss"), env.orgs.ranksOf("beta").stream().map(Rank::id).toList());
        assertEquals(List.of(member), env.orgs.membersOf("beta").stream().map(Membership::character).toList());
        assertEquals(Organization.ownerOf("beta"), env.accounts.find(org.account()).orElseThrow().owner());
        assertEquals(List.of(), env.accounts.accountsOf(Organization.ownerOf("acme")));

        Organization again = ok(env.orgs.create("acme", "Acme 2"));
        assertNotEquals(org.account(), again.account());
    }

    @Test
    void renameWorksOnAnArchivedOrganizationAndKeepsItArchived() {
        ok(env.orgs.create("police", "Polizia"));
        ok(env.orgs.archive("police"));
        Organization renamed = ok(env.orgs.rename("police", "police_old", null));
        assertEquals(OrgStatus.ARCHIVED, renamed.status());
        assertEquals("Polizia", renamed.label());
        ok(env.orgs.create("police", "Polizia"));
    }

    @Test
    void renameRejectsUnknownSourceAndTakenTarget() {
        ok(env.orgs.create("acme", "Acme"));
        ok(env.orgs.create("beta", "Beta"));
        assertEquals(new OrgError.UnknownOrg("nope"), err(env.orgs.rename("nope", "gamma", null)));
        assertEquals(new OrgError.Exists("beta"), err(env.orgs.rename("acme", "beta", null)));
        assertThrows(IllegalArgumentException.class, () -> env.orgs.rename("acme", "Bad Id", null));
    }

    @Test
    void deleteRemovesAnEmptyArchivedOrganizationAndFreesTheId() {
        Organization org = ok(env.orgs.create("acme", "Acme"));
        ok(env.orgs.defineRank("acme", new Rank("boss", "Boss", 1, Set.of())));
        ok(env.orgs.archive("acme"));

        assertEquals("acme", ok(env.orgs.delete("acme")).id());

        assertTrue(env.orgs.find("acme").isEmpty());
        assertEquals(List.of(), env.orgs.ranksOf("acme"));
        assertEquals(AccountStatus.FROZEN, env.accounts.find(org.account()).orElseThrow().status());
        Organization again = ok(env.orgs.create("acme", "Acme"));
        assertNotEquals(org.account(), again.account());
        assertEquals(AccountStatus.ACTIVE, env.accounts.find(again.account()).orElseThrow().status());
    }

    @Test
    void deleteRefusesActiveUnknownPopulatedOrFundedOrganizations() {
        assertEquals(new OrgError.UnknownOrg("nope"), err(env.orgs.delete("nope")));

        ok(env.orgs.create("acme", "Acme"));
        assertEquals(new OrgError.NotArchived("acme"), err(env.orgs.delete("acme")));

        ok(env.orgs.defineRank("acme", new Rank("boss", "Boss", 1, Set.of())));
        ok(env.orgs.addMember("acme", env.newCharacter().id(), "boss"));
        ok(env.orgs.archive("acme"));
        assertEquals(new OrgError.NotEmpty("acme"), err(env.orgs.delete("acme")));

        Organization funded = ok(env.orgs.create("rich", "Rich"));
        env.fund(env.accounts.find(funded.account()).orElseThrow(), "USD", 10);
        ok(env.orgs.archive("rich"));
        assertEquals(new OrgError.NotEmpty("rich"), err(env.orgs.delete("rich")));
        assertTrue(env.orgs.find("rich").isPresent());
    }

    @Test
    void unarchiveRestoresTheOrganizationAndItsAccountIsIdempotentAndEmitsOnce() {
        Organization org = ok(env.orgs.create("acme", "Acme"));
        ok(env.orgs.archive("acme"));
        env.events.clear();

        Organization restored = ok(env.orgs.unarchive("acme"));

        assertEquals(OrgStatus.ACTIVE, restored.status());
        assertEquals(AccountStatus.ACTIVE, env.accounts.find(org.account()).orElseThrow().status());
        assertEquals(List.of(new OrganizationUnarchivedEvent(restored)), env.events);
        env.events.clear();
        assertEquals(restored, ok(env.orgs.unarchive("acme")));
        assertEquals(List.of(), env.events);
        assertEquals(new OrgError.UnknownOrg("nope"), err(env.orgs.unarchive("nope")));
        ok(env.orgs.defineRank("acme", new Rank("boss", "Boss", 1, Set.of())));
    }

    @Test
    void archiveRepairsARowArchivedWithoutFrozenAccount() throws Exception {
        Organization org = ok(env.orgs.create("acme", "Acme"));
        env.db.tx(c -> {
            try (var ps = c.prepareStatement("UPDATE qrm_organization SET status='ARCHIVED' WHERE id='acme'")) {
                ps.executeUpdate();
            }
            return null;
        });
        assertEquals(AccountStatus.ACTIVE, env.accounts.find(org.account()).orElseThrow().status());
        env.events.clear();
        Organization archived = ok(env.orgs.archive("acme"));
        assertEquals(AccountStatus.FROZEN, env.accounts.find(org.account()).orElseThrow().status());
        assertEquals(List.of(new OrganizationArchivedEvent(archived)), env.events);
        env.events.clear();
        ok(env.orgs.archive("acme"));
        assertEquals(List.of(), env.events);
    }

    @Test
    void archiveRepairsAFrozenAccountOfAStillActiveOrganization() {
        Organization org = ok(env.orgs.create("acme", "Acme"));
        env.accounts.setStatus(org.account(), AccountStatus.FROZEN);
        env.events.clear();
        Organization archived = ok(env.orgs.archive("acme"));
        assertEquals(OrgStatus.ARCHIVED, archived.status());
        assertEquals(AccountStatus.FROZEN, env.accounts.find(org.account()).orElseThrow().status());
        assertEquals(List.of(new OrganizationArchivedEvent(archived)), env.events);
        env.events.clear();
        ok(env.orgs.archive("acme"));
        assertEquals(List.of(), env.events);
    }

    @Test
    void defineRankCreatesReplacesAndListsByLevel() {
        ok(env.orgs.create("acme", "Acme"));
        ok(env.orgs.defineRank("acme", new Rank("boss", "Capo", 5, Set.of("org.account.withdraw"))));
        ok(env.orgs.defineRank("acme", new Rank("clerk", "Impiegato", 1, Set.of("shop.sell"))));
        ok(env.orgs.defineRank("acme", new Rank("boss", "Capo Supremo", 5, Set.of("*"))));
        List<Rank> ranks = env.orgs.ranksOf("acme");
        assertEquals(List.of("clerk", "boss"), ranks.stream().map(Rank::id).toList());
        assertEquals(new Rank("boss", "Capo Supremo", 5, Set.of("*")), ranks.get(1));
        assertEquals(List.of(), env.orgs.ranksOf("nope"));
    }

    @Test
    void defineRankRejectsATakenLevelButAllowsSameIdSameLevel() {
        ok(env.orgs.create("acme", "Acme"));
        ok(env.orgs.defineRank("acme", new Rank("boss", "Capo", 5, Set.of())));
        assertEquals(new OrgError.LevelTaken(5), err(env.orgs.defineRank("acme", new Rank("deputy", "Vice", 5, Set.of()))));
        ok(env.orgs.defineRank("acme", new Rank("boss", "Capo", 5, Set.of("a.b"))));
    }

    @Test
    void rankOperationsOnUnknownOrArchivedOrganizationFail() {
        assertEquals(new OrgError.UnknownOrg("nope"),
                err(env.orgs.defineRank("nope", new Rank("boss", "Capo", 1, Set.of()))));
        assertEquals(new OrgError.UnknownOrg("nope"), err(env.orgs.removeRank("nope", "boss")));
        ok(env.orgs.create("acme", "Acme"));
        ok(env.orgs.defineRank("acme", new Rank("boss", "Capo", 1, Set.of())));
        ok(env.orgs.archive("acme"));
        assertEquals(new OrgError.Archived("acme"),
                err(env.orgs.defineRank("acme", new Rank("clerk", "Imp", 2, Set.of()))));
        assertEquals(new OrgError.Archived("acme"), err(env.orgs.removeRank("acme", "boss")));
    }

    @Test
    void removeRankDeletesIt() {
        ok(env.orgs.create("acme", "Acme"));
        Rank boss = new Rank("boss", "Capo", 5, Set.of());
        ok(env.orgs.defineRank("acme", boss));
        assertEquals(boss, ok(env.orgs.removeRank("acme", "boss")));
        assertEquals(List.of(), env.orgs.ranksOf("acme"));
        assertEquals(new OrgError.UnknownRank("boss"), err(env.orgs.removeRank("acme", "boss")));
    }

    @Test
    void rejectedLevelTakenDoesNotInsertTheConflictingRank() {
        ok(env.orgs.create("acme", "Acme"));
        ok(env.orgs.defineRank("acme", new Rank("boss", "Capo", 5, Set.of())));
        err(env.orgs.defineRank("acme", new Rank("deputy", "Vice", 5, Set.of())));
        assertEquals(List.of("boss"), env.orgs.ranksOf("acme").stream().map(Rank::id).toList());
    }

    @Test
    void changingAnExistingRanksLevelToAFreeOneSucceedsAndReorders() {
        ok(env.orgs.create("acme", "Acme"));
        ok(env.orgs.defineRank("acme", new Rank("clerk", "Impiegato", 1, Set.of())));
        ok(env.orgs.defineRank("acme", new Rank("boss", "Capo", 5, Set.of())));
        ok(env.orgs.defineRank("acme", new Rank("clerk", "Impiegato", 9, Set.of())));
        List<Rank> ranks = env.orgs.ranksOf("acme");
        assertEquals(List.of("boss", "clerk"), ranks.stream().map(Rank::id).toList());
        assertEquals(9, ranks.get(1).level());
    }

    @Test
    void createdAtIsTheClockInstantAsStoredAndReturnedByFind() {
        env.clock.advance(java.time.Duration.ofNanos(123_456_789));
        Organization org = ok(env.orgs.create("acme", "Acme"));
        java.time.Instant expected = env.clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        assertEquals(expected, org.createdAt());
        assertEquals(expected, env.orgs.find("acme").orElseThrow().createdAt());
    }
}
