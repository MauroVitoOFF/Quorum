package dev.qrm.api.staff;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class StaffNodesTest {
    @Test
    void exactPatternMatchesOnlyItself() {
        assertTrue(StaffNodes.matches("staff.jobs", "staff.jobs"));
        assertFalse(StaffNodes.matches("staff.jobs", "staff.orgs"));
    }

    @Test
    void wildcardMatchesChildrenButNotLookalikes() {
        assertTrue(StaffNodes.matches("staff.*", "staff.jobs"));
        assertTrue(StaffNodes.matches("staff.module.*", "staff.module.bank"));
        assertFalse(StaffNodes.matches("staff.*", "staffing.jobs"));
        assertFalse(StaffNodes.matches("staff.module.*", "staff.jobs"));
    }

    @Test
    void starMatchesEverything() {
        assertTrue(StaffNodes.matches("*", "staff.bank"));
    }

    @Test
    void matchesAnyChecksEveryPattern() {
        assertTrue(StaffNodes.matchesAny(Set.of("staff.orgs", "staff.jobs"), "staff.jobs"));
        assertFalse(StaffNodes.matchesAny(Set.of("staff.orgs"), "staff.jobs"));
        assertFalse(StaffNodes.matchesAny(Set.of(), "staff.jobs"));
    }

    @Test
    void onlyStaffPatternsAreGrantable() {
        assertTrue(StaffNodes.isGrantable("staff.jobs"));
        assertTrue(StaffNodes.isGrantable("staff.*"));
        assertTrue(StaffNodes.isGrantable("staff.module.*"));
        assertTrue(StaffNodes.isGrantable("*"));
        assertFalse(StaffNodes.isGrantable("police.arrest"));
        assertFalse(StaffNodes.isGrantable("Staff.jobs"));
        assertFalse(StaffNodes.isGrantable(""));
        assertFalse(StaffNodes.isGrantable(null));
    }
}
