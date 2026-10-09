package dev.qrm.api.org;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.id.AccountId;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrgTypesTest {
    @Test
    void organizationValidatesIdAndLabel() {
        AccountId acc = AccountId.random();
        Organization ok = new Organization("acme", "Acme", OrgStatus.ACTIVE, acc, Instant.EPOCH);
        assertEquals("acme", ok.id());
        assertThrows(IllegalArgumentException.class,
                () -> new Organization("Acme", "Acme", OrgStatus.ACTIVE, acc, Instant.EPOCH));
        assertThrows(IllegalArgumentException.class,
                () -> new Organization("acme", " ", OrgStatus.ACTIVE, acc, Instant.EPOCH));
        assertThrows(IllegalArgumentException.class,
                () -> new Organization("acme", "x".repeat(65), OrgStatus.ACTIVE, acc, Instant.EPOCH));
    }

    @Test
    void ownerRefIsStableAndOfOrganizationType() {
        OwnerRef a = Organization.ownerOf("acme");
        assertEquals(OwnerRef.ORGANIZATION, a.type());
        assertEquals(a, Organization.ownerOf("acme"));
        assertNotEquals(a, Organization.ownerOf("other"));
    }

    @Test
    void rankValidatesFields() {
        Rank r = new Rank("boss", "Capo", 5, Set.of("org.account.withdraw", "shop.*"));
        assertEquals(5, r.level());
        assertThrows(IllegalArgumentException.class, () -> new Rank("Boss", "Capo", 5, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Rank("boss", "", 5, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Rank("boss", "Capo", -1, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Rank("boss", "Capo", 1, Set.of("a.*.b")));
    }

    @Test
    void rankPermissionsAreCopied() {
        java.util.HashSet<String> mutable = new java.util.HashSet<>(Set.of("a.b"));
        Rank r = new Rank("boss", "Capo", 1, mutable);
        mutable.add("c.d");
        assertEquals(Set.of("a.b"), r.permissions());
        assertThrows(UnsupportedOperationException.class, () -> r.permissions().add("x"));
    }

    @Test
    void resultHelpers() {
        OrgResult<String> ok = OrgResult.ok("v");
        OrgResult<String> bad = OrgResult.fail(new OrgError.NotMember());
        assertEquals("v", ((OrgResult.Ok<String>) ok).value());
        assertEquals(new OrgError.NotMember(), ((OrgResult.Failed<String>) bad).error());
    }

    @Test
    void preEventCancellationKeepsFirstReason() {
        MembershipChangePreEvent e = new MembershipChangePreEvent("acme", dev.qrm.api.id.CharacterId.random(), null, "boss");
        assertFalse(e.isCancelled());
        e.cancel("mod_a", "no");
        e.cancel("mod_b", "later");
        assertEquals("mod_a", e.cancelledBy());
        assertEquals("boss", e.to());
        assertNull(e.from());
    }
}
