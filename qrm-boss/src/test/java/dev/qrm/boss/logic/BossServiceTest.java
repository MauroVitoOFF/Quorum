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
        PlayerCharacter worker = env.member("worker");
        BossSnapshot snap = snap(boss).orElseThrow();
        assertTrue(snap.showMembers());
        assertTrue(snap.canManage());
        assertTrue(snap.canNotice());
        assertEquals("USD", snap.currency());
        BossSnapshot.Member w = member(snap, worker);
        assertTrue(w.canTitle());
        assertTrue(w.canBonus());
    }

    @Test
    void managerManagesMembersButCannotPostGiveBonusOrTitle() {
        PlayerCharacter manager = env.member("manager");
        PlayerCharacter worker = env.member("worker");
        BossSnapshot snap = snap(manager).orElseThrow();
        assertTrue(snap.showMembers());
        assertTrue(snap.canManage());
        assertFalse(snap.canNotice());
        BossSnapshot.Member w = member(snap, worker);
        assertTrue(w.editable());
        assertFalse(w.canTitle());
        assertFalse(w.canBonus());
    }

    @Test
    void withoutMenuThereIsNoSnapshot() {
        PlayerCharacter worker = env.member("worker");
        assertTrue(snap(worker).isEmpty());
        assertTrue(snap(env.newCharacter()).isEmpty());
    }

    @Test
    void membersSectionAppearsWithAnyNodeThatActsOnMembers() {
        env.orgs.defineRank(BossEnv.ORG, new Rank("payer", "Cassiere", 15, Set.of("boss.menu", "boss.bonus")));
        PlayerCharacter payer = env.member("payer");
        PlayerCharacter worker = env.member("worker");
        BossSnapshot snap = snap(payer).orElseThrow();
        assertTrue(snap.showMembers());
        assertFalse(snap.canManage());
        assertFalse(snap.canNotice());
        assertTrue(member(snap, worker).canBonus());
        assertFalse(member(snap, worker).editable(), "senza boss.members.manage niente rank");
        assertTrue(snap.candidates().isEmpty());
    }

    private static BossSnapshot.Member member(BossSnapshot snap, PlayerCharacter who) {
        return snap.members().stream().filter(m -> m.character().equals(who.id().value())).findFirst().orElseThrow();
    }

    @Test
    void archivedOrganizationHasNoSnapshotAndRejectsActions() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter worker = env.member("worker");
        env.personal(worker.id(), 0);
        env.fund(env.orgAccount(), 1000);
        env.orgs.archive(BossEnv.ORG);
        assertTrue(snap(boss).isEmpty());
        assertNotEquals(BossResult.OK, s.giveBonus(boss.id(), BossEnv.ORG, worker.id(), "5", "", UUID.randomUUID()));
        assertNotEquals(BossResult.OK, s.setNotice(boss.id(), BossEnv.ORG, "chiusi"));
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

    // ---- presenza e fascicolo -------------------------------------------------------------------------

    @Test
    void presenceFollowsTheOnlineCharacters() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter worker = env.member("worker");
        PlayerCharacter away = env.member("worker");
        BossSnapshot snap = snap(boss, boss, worker).orElseThrow();
        assertEquals(2, snap.online());
        assertEquals(-1, snap.onDuty(), "senza un Job non c'e' il servizio");
        assertEquals(2, member(snap, worker).presence());
        assertTrue(member(snap, worker).active());
        assertEquals(0, member(snap, away).presence());
        assertFalse(member(snap, away).active());
        assertEquals(worker.playerId().value(), member(snap, worker).player());
    }

    @Test
    void bonusAndTitleLandInTheEmployeeFileNewestFirst() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter worker = env.member("worker");
        env.personal(worker.id(), 0);
        env.fund(env.orgAccount(), 1000_00);
        assertEquals(BossResult.OK, s.setTitle(boss.id(), BossEnv.ORG, worker.id(), "Cassiere"));
        assertEquals(BossResult.OK, s.giveBonus(boss.id(), BossEnv.ORG, worker.id(), "100", "Puntuale", UUID.randomUUID()));
        List<BossSnapshot.LogEntry> log = member(snap(boss).orElseThrow(), worker).log();
        assertEquals(2, log.size());
        assertEquals(BossSnapshot.LogEntry.BONUS, log.get(0).kind());
        assertEquals("100.00 $ · Puntuale", log.get(0).text());
        assertEquals(BossSnapshot.LogEntry.TITLE, log.get(1).kind());
        assertEquals("Cassiere", log.get(1).text());
    }

    @Test
    void aRetriedBonusIsLoggedOnce() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter worker = env.member("worker");
        env.personal(worker.id(), 0);
        env.fund(env.orgAccount(), 1000_00);
        UUID id = UUID.randomUUID();
        s.giveBonus(boss.id(), BossEnv.ORG, worker.id(), "100", "", id);
        s.giveBonus(boss.id(), BossEnv.ORG, worker.id(), "100", "", id);
        assertEquals(1, member(snap(boss).orElseThrow(), worker).log().size());
    }

    @Test
    void theFileShowsOnlyTheLatestThreeAndFailedBonusesLeaveNoTrace() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter worker = env.member("worker");
        env.personal(worker.id(), 0);
        env.fund(env.orgAccount(), 1000_00);
        for (int i = 1; i <= 5; i++) s.giveBonus(boss.id(), BossEnv.ORG, worker.id(), Integer.toString(i), "", UUID.randomUUID());
        s.giveBonus(boss.id(), BossEnv.ORG, worker.id(), "abc", "", UUID.randomUUID());
        List<BossSnapshot.LogEntry> log = member(snap(boss).orElseThrow(), worker).log();
        assertEquals(BossSnapshot.MAX_LOG, log.size());
        assertEquals("5.00 $", log.get(0).text());
        assertEquals("3.00 $", log.get(2).text());
    }

    // ---- bacheca ------------------------------------------------------------------------------------

    @Test
    void noticeIsPublishedReadByEveryoneAndCleared() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter manager = env.member("manager");
        assertEquals(BossResult.OK, s.setNotice(boss.id(), BossEnv.ORG, "  Domani   chiusi\u0007 alle 18  "));
        BossSnapshot seen = snap(manager).orElseThrow();
        assertEquals("Domani chiusi alle 18", seen.notice());
        assertTrue(seen.noticeAuthor().startsWith(boss.firstName()));
        assertTrue(seen.noticeAt() > 0);
        assertFalse(seen.canNotice());
        assertEquals(BossResult.OK, s.setNotice(boss.id(), BossEnv.ORG, "   "));
        assertEquals("", snap(manager).orElseThrow().notice());
    }

    @Test
    void noticeNeedsItsNodeAndIsCapped() {
        PlayerCharacter manager = env.member("manager");
        assertEquals(BossResult.NO_PERMISSION, s.setNotice(manager.id(), BossEnv.ORG, "ciao"));
        PlayerCharacter boss = env.member("boss");
        assertEquals(BossResult.OK, s.setNotice(boss.id(), BossEnv.ORG, "x".repeat(500)));
        assertEquals(BossPerks.MAX_NOTICE, snap(boss).orElseThrow().notice().length());
    }

    // ---- titoli -------------------------------------------------------------------------------------

    @Test
    void titleIsAssignedShownAndRemoved() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter worker = env.member("worker");
        assertEquals(BossResult.OK, s.setTitle(boss.id(), BossEnv.ORG, worker.id(), "Capocantiere"));
        assertEquals("Capocantiere", member(snap(boss).orElseThrow(), worker).title());
        assertEquals(BossResult.OK, s.setTitle(boss.id(), BossEnv.ORG, worker.id(), ""));
        assertEquals("", member(snap(boss).orElseThrow(), worker).title());
    }

    @Test
    void titleRulesMatchTheRankLevels() {
        env.orgs.defineRank(BossEnv.ORG, new Rank("lead", "Capo", 25, Set.of("boss.menu", "boss.title")));
        PlayerCharacter lead = env.member("lead");
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter worker = env.member("worker");
        assertEquals(BossResult.LEVEL_TOO_HIGH, s.setTitle(lead.id(), BossEnv.ORG, boss.id(), "Re"));
        assertEquals(BossResult.OK, s.setTitle(lead.id(), BossEnv.ORG, worker.id(), "Apprendista"));
        assertEquals(BossResult.NOT_MEMBER, s.setTitle(boss.id(), BossEnv.ORG, env.newCharacter().id(), "Ospite"));
        PlayerCharacter manager = env.member("manager");
        assertEquals(BossResult.NO_PERMISSION, s.setTitle(manager.id(), BossEnv.ORG, worker.id(), "Altro"));
        assertEquals(BossResult.OK, s.setTitle(boss.id(), BossEnv.ORG, worker.id(), "y".repeat(100)));
        assertEquals(BossPerks.MAX_TITLE, member(snap(boss).orElseThrow(), worker).title().length());
    }

    // ---- bonus --------------------------------------------------------------------------------------

    @Test
    void bonusMovesMoneyFromTheCompanyToTheEmployeeWithTheReason() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter worker = env.member("worker");
        AccountId personal = env.personal(worker.id(), 0);
        env.fund(env.orgAccount(), 1000_00);
        assertEquals(BossResult.OK, s.giveBonus(boss.id(), BossEnv.ORG, worker.id(), "250", "Ottimo turno", UUID.randomUUID()));
        assertEquals(250_00, env.balance(personal));
        assertEquals(750_00, env.balance(env.orgAccount()));
        assertEquals("Bonus aziendale: Ottimo turno", env.txs.history(personal, 10).get(0).reason());
        assertEquals(Optional.of("250.00 $"), s.formatBonus("250"));
    }

    @Test
    void bonusRetryWithTheSameIdPaysOnce() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter worker = env.member("worker");
        AccountId personal = env.personal(worker.id(), 0);
        env.fund(env.orgAccount(), 1000_00);
        UUID id = UUID.randomUUID();
        assertEquals(BossResult.OK, s.giveBonus(boss.id(), BossEnv.ORG, worker.id(), "100", "", id));
        assertEquals(BossResult.OK, s.giveBonus(boss.id(), BossEnv.ORG, worker.id(), "100", "", id));
        assertEquals(100_00, env.balance(personal));
    }

    @Test
    void bonusRejectsBadAmountsAndCapsTheSize() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter worker = env.member("worker");
        AccountId personal = env.personal(worker.id(), 0);
        env.fund(env.orgAccount(), 100_000_00);
        for (String bad : new String[] {"", "0", "-5", "abc", "25%", "all", "1.234", "5001"}) {
            assertEquals(BossResult.INVALID_AMOUNT,
                    s.giveBonus(boss.id(), BossEnv.ORG, worker.id(), bad, "", UUID.randomUUID()), "importo: '" + bad + "'");
        }
        assertEquals(BossResult.OK, s.giveBonus(boss.id(), BossEnv.ORG, worker.id(), "5000", "", UUID.randomUUID()));
        assertEquals(5000_00, env.balance(personal));
        assertEquals(Optional.empty(), s.formatBonus("all"));
    }

    @Test
    void bonusNeedsFundsANodeAMemberAnotherPersonAndAnAccount() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter worker = env.member("worker");
        PlayerCharacter manager = env.member("manager");
        env.fund(env.orgAccount(), 10_00);
        assertEquals(BossResult.NO_ACCOUNT, s.giveBonus(boss.id(), BossEnv.ORG, worker.id(), "5", "", UUID.randomUUID()));
        env.personal(worker.id(), 0);
        assertEquals(BossResult.INSUFFICIENT_FUNDS, s.giveBonus(boss.id(), BossEnv.ORG, worker.id(), "50", "", UUID.randomUUID()));
        assertEquals(BossResult.SELF, s.giveBonus(boss.id(), BossEnv.ORG, boss.id(), "5", "", UUID.randomUUID()));
        assertEquals(BossResult.NOT_MEMBER, s.giveBonus(boss.id(), BossEnv.ORG, env.newCharacter().id(), "5", "", UUID.randomUUID()));
        assertEquals(BossResult.NO_PERMISSION, s.giveBonus(manager.id(), BossEnv.ORG, worker.id(), "5", "", UUID.randomUUID()));
        assertEquals(10_00, env.balance(env.orgAccount()));
    }

    @Test
    void frozenOrganizationAccountIsReported() {
        PlayerCharacter boss = env.member("boss");
        PlayerCharacter worker = env.member("worker");
        env.personal(worker.id(), 0);
        env.fund(env.orgAccount(), 100);
        env.accounts.setStatus(env.orgAccount(), dev.qrm.api.economy.AccountStatus.FROZEN);
        assertEquals(BossResult.FROZEN, s.giveBonus(boss.id(), BossEnv.ORG, worker.id(), "1", "", UUID.randomUUID()));
    }

    @Test
    void idempotencyKeyFormat() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertEquals("org:acme:bonus:00000000-0000-0000-0000-000000000001", BossService.key("acme", "bonus", id));
    }
}
