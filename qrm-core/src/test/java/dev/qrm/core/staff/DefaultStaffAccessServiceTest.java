package dev.qrm.core.staff;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.core.db.Database;
import dev.qrm.core.db.Migrator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultStaffAccessServiceTest {
    private Database db;
    private DefaultStaffAccessService svc;
    private final UUID anna = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        db = Database.open("jdbc:sqlite::memory:");
        Migrator.migrate(db);
        svc = new DefaultStaffAccessService(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void noGrantsMeansNoAccess() {
        assertFalse(svc.has(anna, "staff.panel"));
        assertEquals(Set.of(), svc.grantsOf(anna));
    }

    @Test
    void exactGrantAllowsOnlyThatNode() {
        assertTrue(svc.grant(anna, "staff.jobs"));
        assertTrue(svc.has(anna, "staff.jobs"));
        assertFalse(svc.has(anna, "staff.orgs"));
    }

    @Test
    void wildcardGrantAllowsChildren() {
        svc.grant(anna, "staff.*");
        assertTrue(svc.has(anna, "staff.bank"));
        assertTrue(svc.has(anna, "staff.module.police"));
    }

    @Test
    void grantTwiceReportsNoChange() {
        assertTrue(svc.grant(anna, "staff.jobs"));
        assertFalse(svc.grant(anna, "staff.jobs"));
    }

    @Test
    void revokeRemovesAccess() {
        svc.grant(anna, "staff.jobs");
        assertTrue(svc.revoke(anna, "staff.jobs"));
        assertFalse(svc.has(anna, "staff.jobs"));
        assertFalse(svc.revoke(anna, "staff.jobs"));
    }

    @Test
    void accountsAreIndependent() {
        svc.grant(anna, "staff.jobs");
        assertFalse(svc.has(bob, "staff.jobs"));
    }

    @Test
    void grantsAreSortedAndPersistedAcrossInstances() {
        svc.grant(anna, "staff.perms");
        svc.grant(anna, "staff.jobs");
        assertEquals(List.of("staff.jobs", "staff.perms"), List.copyOf(new DefaultStaffAccessService(db).grantsOf(anna)));
    }

    @Test
    void invalidInputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> svc.grant(anna, "police.arrest"));
        assertThrows(IllegalArgumentException.class, () -> svc.revoke(anna, "Not A Node"));
        assertThrows(IllegalArgumentException.class, () -> svc.has(anna, "staff.*"));
    }
}
