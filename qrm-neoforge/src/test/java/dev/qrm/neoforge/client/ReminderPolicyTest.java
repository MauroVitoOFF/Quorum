package dev.qrm.neoforge.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ReminderPolicyTest {
    @Test
    void countsWhenThereIsNoActiveCharacterAndSlotsExist() {
        assertNull(ReminderPolicy.blockedBy(true, true, true, true, false, 0, 3));
        assertNull(ReminderPolicy.blockedBy(true, true, true, true, false, 2, 0));
    }

    @Test
    void isSuspendedWhenSomethingBlocksIt() {
        assertEquals("no player", ReminderPolicy.blockedBy(false, true, true, true, false, 0, 3));
        assertEquals("a screen is open", ReminderPolicy.blockedBy(true, false, true, true, false, 0, 3));
        assertEquals("server has no QRM channel", ReminderPolicy.blockedBy(true, true, false, true, false, 0, 3));
        assertEquals("no roster received", ReminderPolicy.blockedBy(true, true, true, false, false, 0, 3));
        assertEquals("an active character is set", ReminderPolicy.blockedBy(true, true, true, true, true, 1, 3));
        assertEquals("no characters and no slots", ReminderPolicy.blockedBy(true, true, true, true, false, 0, 0));
    }
}
