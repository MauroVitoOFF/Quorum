package dev.qrm.neoforge.server;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.neoforge.net.StaffAccessPayload;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StaffAccessTest {
    @Test
    void operatorGetsEverythingWithoutPatterns() {
        assertEquals(new StaffAccessPayload(true, List.of()), StaffAccess.payloadFor(true, Set.of("staff.jobs")));
    }

    @Test
    void nonOperatorGetsItsGrantsSorted() {
        assertEquals(new StaffAccessPayload(false, List.of("staff.jobs", "staff.perms")),
                StaffAccess.payloadFor(false, Set.of("staff.perms", "staff.jobs")));
    }

    @Test
    void noGrantsMeansEmptyPayload() {
        assertEquals(new StaffAccessPayload(false, List.of()), StaffAccess.payloadFor(false, Set.of()));
    }

    @Test
    void payloadIsCappedToTheCodecLimit() {
        Set<String> many = new java.util.TreeSet<>();
        for (int i = 0; i < 200; i++) many.add("staff.module.m" + i);
        assertEquals(StaffAccessPayload.MAX_PATTERNS, StaffAccess.payloadFor(false, many).patterns().size());
    }

    @Test
    void delegatedStaffMayNotActOnAnOperator() {
        assertFalse(StaffAccess.mayTarget(false, true));
        assertTrue(StaffAccess.mayTarget(false, false));
        assertTrue(StaffAccess.mayTarget(true, true));
        assertTrue(StaffAccess.mayTarget(true, false));
    }
}
