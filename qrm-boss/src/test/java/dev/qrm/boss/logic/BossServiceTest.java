package dev.qrm.boss.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.AccountId;
import dev.qrm.api.org.Rank;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BossServiceTest {
    private BossEnv env;
    private BossService s;

    @BeforeEach
    void setUp() {
        env = new BossEnv();
        s = env.service;
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private Optional<BossSnapshot> snap(PlayerCharacter viewer, PlayerCharacter... online) {
        return s.snapshot(viewer.id(), BossEnv.ORG, List.of(online));
    }

    // ---- permessi e sezioni -------------------------------------------------------------------------

    @Test
    void patternOnTheRankGrantsEveryBossNode() {
        PlayerCharacter boss = env.member("boss");
        BossSnapshot snap = snap(boss).orElseThrow();
        assertTrue(snap.showMembers());
        assertTrue(snap.showAccount());
        assertTrue(snap.hasBalance());
        assertTrue(snap.canDeposit());
        assertTrue(snap.canWithdraw());
    }

    @Test
    void managerSeesMembersButNotTheAccount() {
        PlayerCharacter manager = env.member("manager");
        BossSnapshot snap = snap(manager).orElseThrow();
        assertTrue(snap.showMembers());
        assertFalse(snap.showAccount());
        assertFalse(snap.hasBalance());
        assertEquals("", snap.balance());
        assertFalse(snap.canDeposit());
        assertFalse(snap.canWithdraw());
    }

    @Test
    void withoutMenuThereIsNoSnapshot() {
        PlayerCharacter worker = env.member("worker");
        assertTrue(snap(worker).isEmpty());
        assertTrue(snap(env.newCharacter()).isEmpty());
    }

    @Test
    void accountSectionAppearsWithAnyBankNode() throws Exception {
        env.orgs.defineRank(BossEnv.ORG, new Rank("cashier", "Cassiere", 15, Set.of("boss.menu", "boss.bank.deposit")));
        PlayerCharacter cashier = env.member("cashier");
        BossSnapshot snap = snap(cashier).orElseThrow();
        assertFalse(snap.showMembers());
        assertTrue(snap.showAccount());
        assertTrue(snap.canDeposit());
        assertFalse(snap.canWithdraw());
        assertFalse(snap.hasBalance());
    }

    @Test
    void balanceIsShownWithTheViewNode() {
        env.orgs.defineRank(BossEnv.ORG, new Rank("auditor", "Revisore", 15, Set.of("boss.menu", "boss.bank.view")));
        env.fund(env.orgAccount(), 12_50);
        PlayerCharacter auditor = env.member("auditor");
        BossSnapshot snap = snap(auditor).orElseThrow();
        assertTrue(snap.hasBalance());
        assertEquals("12.50 $", snap.balance());
        assertEquals("USD", snap.currency());
    }

    @Test
    void archivedOrganizationHasNoSnapshotAndRejectsActions() {
        PlayerCharacter boss = env.member("boss");
        env.personal(boss.id(), 1000);
        env.orgs.archive(BossEnv.ORG);
        assertTrue(snap(boss).isEmpty());
        assertNotEquals(BossResult.OK, s.deposit(boss.id(), BossEnv.ORG, "5", UUID.randomUUID()));
        assertNotEquals(BossResult.OK, s.addMember(boss.id(), BossEnv.ORG, env.newCharacter().id()));
    }

    @Test
    void archivedCharacterLosesAccess() {
        PlayerCharacter boss = env.member("boss");
        env.chars.archive(boss.playerId(), boss.id());
        assertTrue(snap(boss).isEmpty());
        assertEquals(BossResult.NO_PERMISSION, s.removeMember(boss.id(), BossEnv.ORG, env.member("worker").id()));
    }

    @Test
    void menuOrganizationsListsOnlyOrganizationsWithTheNode() {
        env.orgs.create("zeta", "Zeta");
        env.orgs.defineRank("zeta", new Rank("clerk", "Clerk", 1, Set.of()));
        env.orgs.defineRank("zeta", new Rank("owner", "Owner", 9, Set.of("boss.menu")));
        PlayerCharacter c = env.member("boss");
        env.orgs.addMember("zeta", c.id(), "clerk");
        assertEquals(List.of(BossEnv.ORG), s.menuOrganizations(c.id()).stream().map(o -> o.id()).toList());
        env.orgs.setRank("zeta", c.id(), "owner");
        assertEquals(List.of(BossEnv.ORG, "zeta"), s.menuOrganizations(c.id()).stream().map(o -> o.id()).toList());
    }

    // ---- membri -------------------------------------------------------------------------------------

    @Test
    void snapshotListsMembersWithEditableFlagsAndAssignableRanks() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter manager = env.member("manager");
        PlayerCharacter worker = env.member("worker");
        BossSnapshot forManager = snap(manager).orElseThrow();
        assertEquals(3, forManager.memberCount());
        assertEquals(List.of("worker", "manager"), forManager.ranks().stream().map(r -> r.id()).toList());
        assertFalse(editable(forManager, boss), "livello piu' alto");
        assertFalse(editable(forManager, manager), "se stesso");
        assertTrue(editable(forManager, worker));
        BossSnapshot forBoss = snap(boss).orElseThrow();
        assertTrue(editable(forBoss, manager));
        assertFalse(editable(forBoss, boss));
    }

    private static boolean editable(BossSnapshot snap, PlayerCharacter who) {
        return snap.members().stream().filter(m -> m.character().equals(who.id().value())).findFirst().orElseThrow()
                .editable();
    }

    @Test
    void candidatesAreOnlineNonMembers() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter member = env.member("worker");
        PlayerCharacter outsider = env.newCharacterNamed("Zed", "Last");
        BossSnapshot snap = snap(boss, boss, member, outsider).orElseThrow();
        assertEquals(List.of(outsider.id().value()),
                snap.candidates().stream().map(BossSnapshot.Candidate::character).toList());
        assertEquals("Zed Last", snap.candidates().get(0).name());
    }

    @Test
    void addMemberUsesTheLowestAssignableRank() {
        PlayerCharacter manager = env.member("manager");
        PlayerCharacter outsider = env.newCharacter();
        assertEquals(BossResult.OK, s.addMember(manager.id(), BossEnv.ORG, outsider.id()));
        assertEquals("worker", rankOf(outsider));
        assertEquals(BossResult.ALREADY_MEMBER, s.addMember(manager.id(), BossEnv.ORG, outsider.id()));
    }

    @Test
    void membersCannotBeManagedWithoutTheNode() {
        PlayerCharacter worker = env.member("worker");
        PlayerCharacter other = env.newCharacter();
        assertEquals(BossResult.NO_PERMISSION, s.addMember(worker.id(), BossEnv.ORG, other.id()));
        assertTrue(env.orgs.membersOf(BossEnv.ORG).stream().noneMatch(m -> m.character().equals(other.id())));
    }

    @Test
    void cannotActOnSelfOrOnHigherLevels() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter manager = env.member("manager");
        assertEquals(BossResult.SELF, s.removeMember(manager.id(), BossEnv.ORG, manager.id()));
        assertEquals(BossResult.SELF, s.setRank(boss.id(), BossEnv.ORG, boss.id(), "worker"));
        assertEquals(BossResult.LEVEL_TOO_HIGH, s.removeMember(manager.id(), BossEnv.ORG, boss.id()));
        assertEquals(BossResult.LEVEL_TOO_HIGH, s.setRank(manager.id(), BossEnv.ORG, boss.id(), "worker"));
        assertEquals("boss", rankOf(boss));
    }

    @Test
    void cannotAssignARankAboveOwnLevel() {
        PlayerCharacter manager = env.member("manager");
        PlayerCharacter worker = env.member("worker");
        assertEquals(BossResult.LEVEL_TOO_HIGH, s.setRank(manager.id(), BossEnv.ORG, worker.id(), "boss"));
        assertEquals(BossResult.OK, s.setRank(manager.id(), BossEnv.ORG, worker.id(), "manager"));
        assertEquals("manager", rankOf(worker));
        assertEquals(BossResult.UNKNOWN_RANK, s.setRank(manager.id(), BossEnv.ORG, worker.id(), "nope"));
    }

    @Test
    void sameLevelPeersCanBeManaged() {
        PlayerCharacter a = env.member("manager");
        PlayerCharacter b = env.member("manager");
        assertEquals(BossResult.OK, s.setRank(a.id(), BossEnv.ORG, b.id(), "worker"));
        assertEquals(BossResult.OK, s.removeMember(a.id(), BossEnv.ORG, b.id()));
    }

    @Test
    void removeMemberAndNotMember() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter worker = env.member("worker");
        assertEquals(BossResult.OK, s.removeMember(boss.id(), BossEnv.ORG, worker.id()));
        assertEquals(BossResult.NOT_MEMBER, s.removeMember(boss.id(), BossEnv.ORG, worker.id()));
        assertEquals(BossResult.NOT_MEMBER, s.setRank(boss.id(), BossEnv.ORG, worker.id(), "manager"));
    }

    @Test
    void nonMemberCannotManageMembers() {
        PlayerCharacter outsider = env.newCharacter();
        PlayerCharacter worker = env.member("worker");
        assertEquals(BossResult.NO_PERMISSION, s.removeMember(outsider.id(), BossEnv.ORG, worker.id()));
    }

    private String rankOf(PlayerCharacter c) {
        return env.orgs.membersOf(BossEnv.ORG).stream().filter(m -> m.character().equals(c.id())).findFirst()
                .orElseThrow().rankId();
    }

    // ---- conto --------------------------------------------------------------------------------------

    @Test
    void depositMovesMoneyAndIsRecorded() {
        PlayerCharacter boss = env.member("boss");
        AccountId personal = env.personal(boss.id(), 1000_00);
        assertEquals(BossResult.OK, s.deposit(boss.id(), BossEnv.ORG, "500", UUID.randomUUID()));
        assertEquals(500_00, env.balance(env.orgAccount()));
        assertEquals(500_00, env.balance(personal));
        assertEquals(1, env.txs.history(env.orgAccount(), 10).size());
        assertEquals("Deposito aziendale", env.txs.history(env.orgAccount(), 10).get(0).reason());
    }

    @Test
    void percentagesRoundDownOnTheSourceBalance() {
        PlayerCharacter boss = env.member("boss");
        AccountId personal = env.personal(boss.id(), 1001); // 10.01
        assertEquals(BossResult.OK, s.deposit(boss.id(), BossEnv.ORG, "25%", UUID.randomUUID()));
        assertEquals(250, env.balance(env.orgAccount()), "floor(1001 * 25 / 100)");
        assertEquals(751, env.balance(personal));
        assertEquals(BossResult.OK, s.deposit(boss.id(), BossEnv.ORG, "all", UUID.randomUUID()));
        assertEquals(0, env.balance(personal));
        assertEquals(1001, env.balance(env.orgAccount()));
    }

    @Test
    void invalidAndExcessiveAmountsAreRejectedWithoutMovingMoney() {
        PlayerCharacter boss = env.member("boss");
        AccountId personal = env.personal(boss.id(), 100_00);
        for (String bad : new String[] {"", "0", "-5", "abc", "0%", "101%", "1.234"}) {
            assertEquals(BossResult.INVALID_AMOUNT, s.deposit(boss.id(), BossEnv.ORG, bad, UUID.randomUUID()),
                    "importo: '" + bad + "'");
        }
        assertEquals(BossResult.INSUFFICIENT_FUNDS, s.deposit(boss.id(), BossEnv.ORG, "100000", UUID.randomUUID()));
        assertEquals(100_00, env.balance(personal));
        assertEquals(0, env.balance(env.orgAccount()));
    }

    @Test
    void depositNeedsAPersonalAccountAndDoesNotCreateOne() {
        PlayerCharacter boss = env.member("boss");
        assertEquals(BossResult.NO_ACCOUNT, s.deposit(boss.id(), BossEnv.ORG, "5", UUID.randomUUID()));
        assertTrue(env.accounts.accountsOf(dev.qrm.api.economy.OwnerRef.character(boss.id())).isEmpty());
    }

    @Test
    void depositNeedsItsNode() {
        env.orgs.defineRank(BossEnv.ORG, new Rank("viewer", "Viewer", 15, Set.of("boss.menu", "boss.bank.view")));
        PlayerCharacter viewer = env.member("viewer");
        env.personal(viewer.id(), 1000);
        assertEquals(BossResult.NO_PERMISSION, s.deposit(viewer.id(), BossEnv.ORG, "5", UUID.randomUUID()));
        assertEquals(0, env.balance(env.orgAccount()));
    }

    @Test
    void sameRequestIdNeverDuplicatesTheDeposit() {
        PlayerCharacter boss = env.member("boss");
        env.personal(boss.id(), 1000_00);
        UUID id = UUID.randomUUID();
        assertEquals(BossResult.OK, s.deposit(boss.id(), BossEnv.ORG, "100", id));
        assertEquals(BossResult.OK, s.deposit(boss.id(), BossEnv.ORG, "100", id));
        assertEquals(100_00, env.balance(env.orgAccount()));
        assertEquals(BossResult.OK, s.deposit(boss.id(), BossEnv.ORG, "100", UUID.randomUUID()));
        assertEquals(200_00, env.balance(env.orgAccount()));
    }

    @Test
    void withdrawNeedsTheCoreNodeAndMovesMoney() {
        PlayerCharacter boss = env.member("boss");
        AccountId personal = env.personal(boss.id(), 0);
        env.fund(env.orgAccount(), 400_00);
        assertEquals(BossResult.OK, s.withdraw(boss.id(), BossEnv.ORG, "50%", UUID.randomUUID()));
        assertEquals(200_00, env.balance(env.orgAccount()));
        assertEquals(200_00, env.balance(personal));
        assertEquals(BossResult.INSUFFICIENT_FUNDS, s.withdraw(boss.id(), BossEnv.ORG, "500", UUID.randomUUID()));

        PlayerCharacter manager = env.member("manager");
        env.personal(manager.id(), 0);
        assertEquals(BossResult.NO_PERMISSION, s.withdraw(manager.id(), BossEnv.ORG, "1", UUID.randomUUID()));
        assertEquals(200_00, env.balance(env.orgAccount()));
    }

    @Test
    void withdrawRetryWithTheSameIdIsIdempotent() {
        PlayerCharacter boss = env.member("boss");
        env.personal(boss.id(), 0);
        env.fund(env.orgAccount(), 100_00);
        UUID id = UUID.randomUUID();
        assertEquals(BossResult.OK, s.withdraw(boss.id(), BossEnv.ORG, "10", id));
        assertEquals(BossResult.OK, s.withdraw(boss.id(), BossEnv.ORG, "10", id));
        assertEquals(90_00, env.balance(env.orgAccount()));
    }

    @Test
    void frozenOrganizationAccountIsReported() {
        PlayerCharacter boss = env.member("boss");
        env.personal(boss.id(), 100);
        env.accounts.setStatus(env.orgAccount(), dev.qrm.api.economy.AccountStatus.FROZEN);
        assertEquals(BossResult.FROZEN, s.deposit(boss.id(), BossEnv.ORG, "1", UUID.randomUUID()));
    }

    @Test
    void idempotencyKeyFormat() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertEquals("org:acme:deposit:00000000-0000-0000-0000-000000000001", BossService.key("acme", "deposit", id));
    }
}
