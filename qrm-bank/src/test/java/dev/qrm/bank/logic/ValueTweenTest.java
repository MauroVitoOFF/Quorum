package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ValueTweenTest {
    @Test
    void startsAtItsValueAndStaysPutWithoutATarget() {
        ValueTween t = new ValueTween(500);
        assertEquals(500, t.value(0));
        assertEquals(500, t.value(10_000));
        assertFalse(t.running(0));
    }

    @Test
    void movesToTheTargetAndStopsThere() {
        ValueTween t = new ValueTween(1000);
        t.retarget(2000, 100);
        assertEquals(1000, t.value(100));
        long mid = t.value(100 + ValueTween.DURATION_MS / 2);
        assertTrue(mid > 1000 && mid < 2000);
        assertTrue(t.running(100 + 10));
        assertEquals(2000, t.value(100 + ValueTween.DURATION_MS));
        assertFalse(t.running(100 + ValueTween.DURATION_MS));
    }

    @Test
    void retargetingMidwayContinuesFromWhatWasShown() {
        ValueTween t = new ValueTween(0);
        t.retarget(1000, 0);
        long shown = t.value(200);
        t.retarget(0, 200);
        assertEquals(shown, t.value(200));
        assertEquals(0, t.value(200 + ValueTween.DURATION_MS));
    }

    @Test
    void sameTargetDoesNotRestart() {
        ValueTween t = new ValueTween(10);
        t.retarget(20, 0);
        t.retarget(20, 300);
        assertEquals(20, t.value(ValueTween.DURATION_MS));
    }
}
