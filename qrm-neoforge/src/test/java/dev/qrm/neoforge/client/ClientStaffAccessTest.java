package dev.qrm.neoforge.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ClientStaffAccessTest {
    @AfterEach
    void reset() {
        ClientStaffAccess.reset();
    }

    @Test
    void nothingBeforeTheServerSpeaks() {
        assertFalse(ClientStaffAccess.allows("staff.panel"));
    }

    @Test
    void operatorAllowsEverything() {
        ClientStaffAccess.set(true, List.of());
        assertTrue(ClientStaffAccess.allows("staff.module.anything"));
    }

    @Test
    void patternsAreMatched() {
        ClientStaffAccess.set(false, List.of("staff.jobs", "staff.module.*"));
        assertTrue(ClientStaffAccess.allows("staff.jobs"));
        assertTrue(ClientStaffAccess.allows("staff.module.bank"));
        assertFalse(ClientStaffAccess.allows("staff.orgs"));
    }

    @Test
    void resetForgetsEverything() {
        ClientStaffAccess.set(true, List.of());
        ClientStaffAccess.reset();
        assertFalse(ClientStaffAccess.allows("staff.panel"));
    }
}
