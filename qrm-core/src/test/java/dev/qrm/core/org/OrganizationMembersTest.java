package dev.qrm.core.org;

import static dev.qrm.core.org.OrgsEnv.err;
import static dev.qrm.core.org.OrgsEnv.ok;
import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.character.CharacterStatus;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.org.*;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizationMembersTest {
    private OrgsEnv env;
    private PlayerCharacter anna;

    @BeforeEach
    void setUp() {
        env = new OrgsEnv();
        anna = env.newCharacter();
        ok(env.orgs.create("acme", "Acme"));
        ok(env.orgs.defineRank("acme", new Rank("clerk", "Impiegato", 1, Set.of("shop.sell"))));
        ok(env.orgs.defineRank("acme", new Rank("boss", "Capo", 5, Set.of("shop.*"))));
        env.events.clear();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    @Test
    void addMemberStoresItAndEmitsAnEvent() {
        Membership m = ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        assertEquals(new Membership("acme", anna.id(), "clerk", env.clock.instant()), m);
        assertEquals(List.of(m), env.orgs.membersOf("acme"));
        assertEquals(List.of(new MembershipChangedEvent("acme", anna.id(), null, m)), env.events);
    }

    @Test
    void addMemberErrors() {
        assertEquals(new OrgError.UnknownOrg("nope"), err(env.orgs.addMember("nope", anna.id(), "clerk")));
        assertEquals(new OrgError.UnknownCharacter(),
                err(env.orgs.addMember("acme", new CharacterId(UUID.randomUUID()), "clerk")));
        assertEquals(new OrgError.UnknownRank("ghost"), err(env.orgs.addMember("acme", anna.id(), "ghost")));
        ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        assertEquals(new OrgError.AlreadyMember(), err(env.orgs.addMember("acme", anna.id(), "boss")));
        PlayerCharacter archived = env.newCharacter();
        env.chars.archive(archived.playerId(), archived.id());
        assertEquals(new OrgError.NotUsable(CharacterStatus.ARCHIVED),
                err(env.orgs.addMember("acme", archived.id(), "clerk")));
        ok(env.orgs.archive("acme"));
        PlayerCharacter late = env.newCharacter();
        assertEquals(new OrgError.Archived("acme"), err(env.orgs.addMember("acme", late.id(), "clerk")));
    }

    @Test
    void preEventCanCancelAndSeesFromAndTo() {
        java.util.List<MembershipChangePreEvent> seen = new java.util.ArrayList<>();
        env.bus.register(MembershipChangePreEvent.class, "t", e -> {
            seen.add(e);
            e.cancel("mod_x", "not allowed");
        });
        assertEquals(new OrgError.Cancelled("mod_x", "not allowed"), err(env.orgs.addMember("acme", anna.id(), "clerk")));
        assertEquals(List.of(), env.orgs.membersOf("acme"));
        assertEquals("clerk", seen.get(0).to());
        assertNull(seen.get(0).from());
        assertEquals(List.of(), env.events);
    }

    @Test
    void setRankChangesRankKeepsSinceAndIsNoOpForTheSameRank() {
        Membership first = ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        env.clock.advance(Duration.ofHours(1));
        env.events.clear();
        assertEquals(first, ok(env.orgs.setRank("acme", anna.id(), "clerk")));
        assertEquals(List.of(), env.events);
        Membership promoted = ok(env.orgs.setRank("acme", anna.id(), "boss"));
        assertEquals(new Membership("acme", anna.id(), "boss", first.since()), promoted);
        assertEquals(List.of(new MembershipChangedEvent("acme", anna.id(), first, promoted)), env.events);
    }

    @Test
    void setRankErrors() {
        assertEquals(new OrgError.NotMember(), err(env.orgs.setRank("acme", anna.id(), "boss")));
        ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        assertEquals(new OrgError.UnknownRank("ghost"), err(env.orgs.setRank("acme", anna.id(), "ghost")));
        env.bus.register(MembershipChangePreEvent.class, "t", e -> e.cancel("m", "no"));
        assertEquals(new OrgError.Cancelled("m", "no"), err(env.orgs.setRank("acme", anna.id(), "boss")));
        assertEquals("clerk", env.orgs.membersOf("acme").get(0).rankId());
    }

    @Test
    void removeMemberDeletesAndEmitsWithNullAfter() {
        Membership m = ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        env.events.clear();
        assertEquals(m, ok(env.orgs.removeMember("acme", anna.id())));
        assertEquals(List.of(), env.orgs.membersOf("acme"));
        assertEquals(List.of(new MembershipChangedEvent("acme", anna.id(), m, null)), env.events);
        assertEquals(new OrgError.NotMember(), err(env.orgs.removeMember("acme", anna.id())));
    }

    @Test
    void removeMemberCanBeCancelled() {
        ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        env.bus.register(MembershipChangePreEvent.class, "t", e -> {
            if (e.to() == null) e.cancel("m", "stay");
        });
        assertEquals(new OrgError.Cancelled("m", "stay"), err(env.orgs.removeMember("acme", anna.id())));
        assertEquals(1, env.orgs.membersOf("acme").size());
    }

    @Test
    void removeRankInUseIsRejected() {
        ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        assertEquals(new OrgError.RankInUse("clerk"), err(env.orgs.removeRank("acme", "clerk")));
        ok(env.orgs.removeMember("acme", anna.id()));
        ok(env.orgs.removeRank("acme", "clerk"));
    }

    @Test
    void membersOfExcludesArchivedCharactersAndIsOrderedBySince() {
        PlayerCharacter bruno = env.newCharacter();
        Membership a = ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        env.clock.advance(Duration.ofMinutes(5));
        Membership b = ok(env.orgs.addMember("acme", bruno.id(), "boss"));
        assertEquals(List.of(a, b), env.orgs.membersOf("acme"));
        env.chars.archive(anna.playerId(), anna.id());
        assertEquals(List.of(b), env.orgs.membersOf("acme"));
        assertEquals(List.of(), env.orgs.membersOf("nope"));
    }

    @Test
    void membershipOperationsOnArchivedOrganizationAreRejected() {
        ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        ok(env.orgs.archive("acme"));
        assertEquals(new OrgError.Archived("acme"), err(env.orgs.setRank("acme", anna.id(), "boss")));
        assertEquals(new OrgError.Archived("acme"), err(env.orgs.removeMember("acme", anna.id())));
    }

    @Test
    void rankRemovedDuringPreEventIsCaughtInsideTheTransaction() {
        env.bus.register(MembershipChangePreEvent.class, "t", e -> ok(env.orgs.removeRank("acme", "clerk")));
        assertEquals(new OrgError.UnknownRank("clerk"), err(env.orgs.addMember("acme", anna.id(), "clerk")));
        assertEquals(List.of(), env.orgs.membersOf("acme"));
    }

    @Test
    void rankRemovedDuringSetRankPreEventYieldsUnknownRankAndLeavesMembershipUnchanged() {
        Membership m = ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        env.bus.register(MembershipChangePreEvent.class, "t", e -> ok(env.orgs.removeRank("acme", "boss")));
        assertEquals(new OrgError.UnknownRank("boss"), err(env.orgs.setRank("acme", anna.id(), "boss")));
        assertEquals(List.of(m), env.orgs.membersOf("acme"));
    }

    @Test
    void organizationArchivedDuringPreEventRejectsSetRank() {
        Membership m = ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        env.bus.register(MembershipChangePreEvent.class, "t", e -> env.orgs.archive("acme"));
        assertEquals(new OrgError.Archived("acme"), err(env.orgs.setRank("acme", anna.id(), "boss")));
        assertEquals(List.of(m), env.orgs.membersOf("acme"));
    }

    @Test
    void organizationArchivedDuringPreEventRejectsRemoveMember() {
        Membership m = ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        env.bus.register(MembershipChangePreEvent.class, "t", e -> env.orgs.archive("acme"));
        assertEquals(new OrgError.Archived("acme"), err(env.orgs.removeMember("acme", anna.id())));
        assertEquals(List.of(m), env.orgs.membersOf("acme"));
    }

    @Test
    void membersWithTheSameSinceAreOrderedByCharacterId() {
        PlayerCharacter bruno = env.newCharacter();
        PlayerCharacter carla = env.newCharacter();
        ok(env.orgs.addMember("acme", anna.id(), "clerk"));
        ok(env.orgs.addMember("acme", bruno.id(), "clerk"));
        ok(env.orgs.addMember("acme", carla.id(), "clerk"));
        List<String> expected = java.util.stream.Stream.of(anna, bruno, carla)
                .map(c -> c.id().value().toString()).sorted().toList();
        assertEquals(expected, env.orgs.membersOf("acme").stream()
                .map(m -> m.character().value().toString()).toList());
    }
}
