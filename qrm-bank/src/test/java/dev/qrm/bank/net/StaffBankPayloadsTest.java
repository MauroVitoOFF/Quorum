package dev.qrm.bank.net;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** I test del modulo girano senza Minecraft: qui solo gli enum del protocollo (i codec si provano all'avvio del server). */
class StaffBankPayloadsTest {
    @Test
    void unknownOrdinalsFallBackSafely() {
        assertEquals(StaffBankAction.REFRESH, StaffBankAction.fromOrdinal(99));
        assertEquals(StaffBankAction.REFRESH, StaffBankAction.fromOrdinal(-1));
        assertEquals(StaffBankResult.ERROR, StaffBankResult.fromOrdinal(-5));
        assertEquals(StaffBankResult.ERROR, StaffBankResult.fromOrdinal(999));
    }

    @Test
    void ordinalsRoundTrip() {
        for (StaffBankAction a : StaffBankAction.values()) assertEquals(a, StaffBankAction.fromOrdinal(a.ordinal()));
        for (StaffBankResult r : StaffBankResult.values()) assertEquals(r, StaffBankResult.fromOrdinal(r.ordinal()));
    }

    @Test
    void onlyPlayerActionsNeedATarget() {
        assertTrue(StaffBankAction.FREEZE.needsTarget());
        assertTrue(StaffBankAction.UNFREEZE.needsTarget());
        assertTrue(StaffBankAction.GIVE_ATM.needsTarget());
        assertFalse(StaffBankAction.AUDIT.needsTarget());
        assertFalse(StaffBankAction.REFRESH.needsTarget());
    }
}
