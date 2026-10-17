package dev.qrm.medical.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MedicalRulesTest {
    private static MedicalProfile downed() {
        MedicalProfile p = new MedicalProfile();
        MedicalRules.down(p);
        return p;
    }

    @Test
    void downStartsTheTimer() {
        MedicalProfile p = downed();
        assertEquals(MedicalState.DOWNED, p.state());
        assertEquals(MedicalRules.BLEED_OUT_TICKS, p.bleedOutTicks());
    }

    @Test
    void healthyDoesNotTick() {
        assertNull(MedicalRules.tick(new MedicalProfile(), false, 1));
    }

    @Test
    void fallsUnconsciousBelowThreshold() {
        MedicalProfile p = downed();
        MedicalState changed = null;
        for (int i = 0; i < MedicalRules.BLEED_OUT_TICKS && changed == null; i++) {
            changed = MedicalRules.tick(p, false, i);
        }
        assertEquals(MedicalState.UNCONSCIOUS, changed);
        assertEquals(MedicalRules.UNCONSCIOUS_BELOW - 1, p.bleedOutTicks());
        assertEquals(MedicalRules.SURRENDER_AFTER_TICKS + 1, p.downedTicks());
    }

    @Test
    void bleedsOutToClinicalDeath() {
        MedicalProfile p = downed();
        MedicalState last = null;
        for (int i = 0; i < MedicalRules.BLEED_OUT_TICKS; i++) {
            MedicalState s = MedicalRules.tick(p, false, i);
            if (s != null) last = s;
        }
        assertEquals(MedicalState.CLINICALLY_DEAD, last);
        assertEquals(0, p.bleedOutTicks());
    }

    @Test
    void rcpSlowsTheTimerByEightyPercent() {
        MedicalProfile p = downed();
        for (long i = 0; i < 100; i++) MedicalRules.tick(p, true, i);
        assertEquals(MedicalRules.BLEED_OUT_TICKS - 20, p.bleedOutTicks());
        assertEquals(0, p.downedTicks(), "con la RCP il tempo a terra non conta");
    }

    @Test
    void criticalHitOnlyFromDowned() {
        MedicalProfile p = downed();
        assertTrue(MedicalRules.criticalHit(p));
        assertEquals(MedicalState.UNCONSCIOUS, p.state());
        assertFalse(MedicalRules.criticalHit(p));
        assertFalse(MedicalRules.criticalHit(new MedicalProfile()));
    }

    @Test
    void surrenderOnlyAfterTwoMinutes() {
        MedicalProfile p = downed();
        assertFalse(MedicalRules.canSurrender(p));
        p.setDownedTicks(MedicalRules.SURRENDER_AFTER_TICKS);
        assertTrue(MedicalRules.canSurrender(p));
        assertFalse(MedicalRules.canSurrender(new MedicalProfile()));
    }

    @Test
    void stabilizeResetsTheTimerButKeepsThePatientDown() {
        MedicalProfile p = downed();
        p.setState(MedicalState.UNCONSCIOUS);
        p.setBleedOutTicks(100);
        MedicalRules.stabilize(p);
        assertEquals(MedicalState.DOWNED, p.state());
        assertEquals(MedicalRules.BLEED_OUT_TICKS, p.bleedOutTicks());
    }

    @Test
    void reviveClearsEverything() {
        MedicalProfile p = downed();
        p.addInjury(new InjuryRecord(InjuryType.BULLET, 1L, 5f));
        MedicalRules.revive(p);
        assertEquals(MedicalState.HEALTHY, p.state());
        assertTrue(p.injuries().isEmpty());
    }

    @Test
    void keepsOnlyTheLatestInjuries() {
        MedicalProfile p = new MedicalProfile();
        for (int i = 0; i < MedicalProfile.MAX_INJURIES + 3; i++) p.addInjury(new InjuryRecord(InjuryType.FALL, i, 1f));
        assertEquals(MedicalProfile.MAX_INJURIES, p.injuries().size());
        assertEquals(3L, p.injuries().get(0).timestamp());
    }

    @Test
    void pulseIsWeakWhenUnconsciousUnlessRcp() {
        MedicalProfile p = downed();
        assertFalse(MedicalRules.pulseWeak(p, false));
        p.setBleedOutTicks(1000);
        assertTrue(MedicalRules.pulseWeak(p, false));
        assertFalse(MedicalRules.pulseWeak(p, true));
    }
}
