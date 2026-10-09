package dev.qrm.core.org;

import static dev.qrm.core.org.OrgsEnv.ok;
import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.job.DutyReason;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.org.Organization;
import dev.qrm.api.org.Rank;
import dev.qrm.api.permission.PermissionService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizationCanTest {
    private OrgsEnv env;
    private PlayerCharacter anna;

    @BeforeEach
    void setUp() {
        env = new OrgsEnv();
        anna = env.newCharacter();
        ok(env.orgs.create("acme", "Acme"));
        ok(env.orgs.defineRank("acme", new Rank("clerk", "Impiegato", 1, Set.of("shop.sell"))));
        ok(env.orgs.defineRank("acme", new Rank("manager", "Direttore", 3, Set.of("shop.refund", "staff.*"))));
        ok(env.orgs.defineRank("acme", new Rank("boss", "Capo", 5, Set.of("org.account.withdraw"))));
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    @Test
    void explicitMemberGetsOwnAndInheritedLowerRankNodes() {
        ok(env.orgs.addMember("acme", anna.id(), "manager"));
        assertTrue(env.orgs.can(anna.id(), "acme", "shop.refund"));
        assertTrue(env.orgs.can(anna.id(), "acme", "shop.sell"));          // ereditato da clerk
        assertTrue(env.orgs.can(anna.id(), "acme", "staff.hire"));         // jolly
        assertFalse(env.orgs.can(anna.id(), "acme", "org.account.withdraw")); // solo boss
        assertFalse(env.orgs.can(anna.id(), "acme", "staff"));             // x.* non corrisponde a x
    }

    @Test
    void lowerRankDoesNotInheritUpwards() {
        ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        assertTrue(env.orgs.can(anna.id(), "acme", "shop.sell"));
        assertFalse(env.orgs.can(anna.id(), "acme", "shop.refund"));
    }

    @Test
    void nonMemberCannot() {
        assertFalse(env.orgs.can(anna.id(), "acme", "shop.sell"));
    }

    @Test
    void unknownOrArchivedOrganizationIsFalse() {
        ok(env.orgs.addMember("acme", anna.id(), "boss"));
        assertTrue(env.orgs.can(anna.id(), "acme", "org.account.withdraw"));
        assertFalse(env.orgs.can(anna.id(), "nope", "org.account.withdraw"));
        ok(env.orgs.archive("acme"));
        assertFalse(env.orgs.can(anna.id(), "acme", "org.account.withdraw"));
    }

    @Test
    void archivedCharacterCannot() {
        ok(env.orgs.addMember("acme", anna.id(), "boss"));
        env.chars.archive(anna.playerId(), anna.id());
        assertFalse(env.orgs.can(anna.id(), "acme", "org.account.withdraw"));
        assertEquals(List.of(), env.orgs.organizationsOf(anna.id()));
    }

    @Test
    void wildcardOrMalformedNodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> env.orgs.can(anna.id(), "acme", "shop.*"));
        assertThrows(IllegalArgumentException.class, () -> env.orgs.can(anna.id(), "acme", "*"));
        assertThrows(IllegalArgumentException.class, () -> env.orgs.can(anna.id(), "acme", "Bad Node"));
    }

    @Test
    void jobLinkedMemberIsImplicitOnlyWhileOnDuty() {
        ok(env.orgs.create("pd", "Polizia"));
        env.jobs.assign(anna.id(), "police", "chief");
        assertFalse(env.orgs.can(anna.id(), "pd", "org.account.withdraw"));          // fuori servizio
        env.jobs.setOnDuty(anna.id(), true, DutyReason.MANUAL);
        assertTrue(env.orgs.can(anna.id(), "pd", "org.account.withdraw"));
        assertTrue(env.permissions.has(anna.id(), "org.account.withdraw"));
        assertFalse(env.orgs.can(anna.id(), "acme", "org.account.withdraw"));         // altra organizzazione
        env.jobs.setOnDuty(anna.id(), false, DutyReason.MANUAL);
        assertFalse(env.orgs.can(anna.id(), "pd", "org.account.withdraw"));
    }

    @Test
    void jobGradeWithoutTheNodeDoesNotGrantIt() {
        ok(env.orgs.create("pd", "Polizia"));
        env.jobs.assign(anna.id(), "police", "cadet");
        env.jobs.setOnDuty(anna.id(), true, DutyReason.MANUAL);
        assertFalse(env.orgs.can(anna.id(), "pd", "org.account.withdraw"));
    }

    @Test
    void jobLinkedToAMissingOrganizationGrantsNothing() {
        env.jobs.assign(anna.id(), "police", "chief");
        env.jobs.setOnDuty(anna.id(), true, DutyReason.MANUAL);
        assertFalse(env.orgs.can(anna.id(), "pd", "org.account.withdraw"));
        assertEquals(List.of(), env.orgs.organizationsOf(anna.id()));
    }

    @Test
    void globalDenyOverrideBeatsTheImplicitBranch() {
        ok(env.orgs.create("pd", "Polizia"));
        env.jobs.assign(anna.id(), "police", "chief");
        env.jobs.setOnDuty(anna.id(), true, DutyReason.MANUAL);
        ((PermissionService) env.permissions).deny(anna.id(), "org.account.withdraw");
        assertFalse(env.orgs.can(anna.id(), "pd", "org.account.withdraw"));
    }

    @Test
    void explicitAndImplicitBranchesAreUnioned() {
        ok(env.orgs.create("pd", "Polizia"));
        ok(env.orgs.defineRank("pd", new Rank("guest", "Ospite", 1, Set.of("pd.visit"))));
        ok(env.orgs.addMember("pd", anna.id(), "guest"));
        assertTrue(env.orgs.can(anna.id(), "pd", "pd.visit"));
        assertFalse(env.orgs.can(anna.id(), "pd", "org.account.withdraw"));
        env.jobs.assign(anna.id(), "police", "chief");
        env.jobs.setOnDuty(anna.id(), true, DutyReason.MANUAL);
        assertTrue(env.orgs.can(anna.id(), "pd", "pd.visit"));
        assertTrue(env.orgs.can(anna.id(), "pd", "org.account.withdraw"));
    }

    @Test
    void organizationsOfListsExplicitAndImplicitWithoutDuplicates() {
        ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        ok(env.orgs.create("pd", "Polizia"));
        assertEquals(List.of("acme"), ids(env.orgs.organizationsOf(anna.id())));
        env.jobs.assign(anna.id(), "police", "chief");
        env.jobs.setOnDuty(anna.id(), true, DutyReason.MANUAL);
        assertEquals(List.of("acme", "pd"), ids(env.orgs.organizationsOf(anna.id())));
        ok(env.orgs.defineRank("pd", new Rank("guest", "Ospite", 1, Set.of())));
        ok(env.orgs.addMember("pd", anna.id(), "guest"));
        assertEquals(List.of("acme", "pd"), ids(env.orgs.organizationsOf(anna.id())));
        ok(env.orgs.archive("acme"));
        assertEquals(List.of("pd"), ids(env.orgs.organizationsOf(anna.id())));
    }

    @Test
    void unknownJobGradeIsNotImplicit() {
        ok(env.orgs.create("pd", "Polizia"));
        env.jobs.assign(anna.id(), "police", "chief");
        env.jobs.setOnDuty(anna.id(), true, DutyReason.MANUAL);
        env.registry.remove("police");
        env.registry.register(new JobDefinition("police", "Polizia", List.of(
                new GradeDefinition("cadet", "Recluta", 1, Set.of(), null)), "pd"));   // il grade "chief" non esiste più
        assertFalse(env.orgs.can(anna.id(), "pd", "org.account.withdraw"));
        assertEquals(List.of(), env.orgs.organizationsOf(anna.id()));
    }

    @Test
    void memberWhoseRankRowIsMissingFailsClosed() throws Exception {
        ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        assertTrue(env.orgs.can(anna.id(), "acme", "shop.sell"));
        env.db.tx(c -> {
            try (var ps = c.prepareStatement("DELETE FROM qrm_org_rank WHERE org_id='acme' AND rank_id='clerk'")) {
                ps.executeUpdate();
            }
            return null;
        });
        assertFalse(env.orgs.can(anna.id(), "acme", "shop.sell"));
    }

    @Test
    void archivedJobOrganizationIsNeitherGrantedNorListed() {
        ok(env.orgs.create("pd", "Polizia"));
        env.jobs.assign(anna.id(), "police", "chief");
        env.jobs.setOnDuty(anna.id(), true, DutyReason.MANUAL);
        assertTrue(env.orgs.can(anna.id(), "pd", "org.account.withdraw"));
        ok(env.orgs.archive("pd"));
        assertFalse(env.orgs.can(anna.id(), "pd", "org.account.withdraw"));
        assertEquals(List.of(), env.orgs.organizationsOf(anna.id()));
    }

    @Test
    void starRankGrantsWithdrawAndAnyExactNode() {
        ok(env.orgs.defineRank("acme", new Rank("owner", "Titolare", 9, Set.of("*"))));
        ok(env.orgs.addMember("acme", anna.id(), "owner"));
        assertTrue(env.orgs.can(anna.id(), "acme", "org.account.withdraw"));
        assertTrue(env.orgs.can(anna.id(), "acme", "anything.at.all"));
    }

    @Test
    void jobWithoutOrgNeverGrantsOrganizationPermissions() {
        ok(env.orgs.create("pd", "Polizia"));
        env.registry.remove("police");
        env.registry.register(new JobDefinition("police", "Polizia", List.of(
                new GradeDefinition("chief", "Comandante", 5, Set.of("org.account.withdraw"), null))));
        env.jobs.assign(anna.id(), "police", "chief");
        env.jobs.setOnDuty(anna.id(), true, DutyReason.MANUAL);
        assertFalse(env.orgs.can(anna.id(), "pd", "org.account.withdraw"));
        assertEquals(List.of(), env.orgs.organizationsOf(anna.id()));
    }

    @Test
    void implicitJobMemberIsNotListedInMembersOf() {
        ok(env.orgs.create("pd", "Polizia"));
        env.jobs.assign(anna.id(), "police", "chief");
        env.jobs.setOnDuty(anna.id(), true, DutyReason.MANUAL);
        assertTrue(env.orgs.can(anna.id(), "pd", "org.account.withdraw"));
        assertEquals(List.of(), env.orgs.membersOf("pd"));
    }

    @Test
    void unknownCharacterCannot() {
        assertFalse(env.orgs.can(new dev.qrm.api.id.CharacterId(java.util.UUID.randomUUID()), "acme", "shop.sell"));
    }

    @Test
    void organizationsOfIsSortedById() {
        ok(env.orgs.create("zeta", "Z"));
        ok(env.orgs.create("alpha", "A"));
        ok(env.orgs.create("pd", "Polizia"));
        ok(env.orgs.defineRank("zeta", new Rank("r", "R", 1, Set.of())));
        ok(env.orgs.defineRank("alpha", new Rank("r", "R", 1, Set.of())));
        ok(env.orgs.addMember("zeta", anna.id(), "r"));
        ok(env.orgs.addMember("alpha", anna.id(), "r"));
        env.jobs.assign(anna.id(), "police", "chief");
        env.jobs.setOnDuty(anna.id(), true, DutyReason.MANUAL);
        assertEquals(List.of("alpha", "pd", "zeta"), ids(env.orgs.organizationsOf(anna.id())));
    }

    private static List<String> ids(List<Organization> orgs) {
        return orgs.stream().map(Organization::id).toList();
    }
}
