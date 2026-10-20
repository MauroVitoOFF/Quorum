package dev.qrm.immersion.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImmersionRulesTest {
    @Test
    void strangerCodeIsFourStableDigits() {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String code = ImmersionRules.strangerCode(id);
        assertEquals(4, code.length());
        assertTrue(code.chars().allMatch(Character::isDigit));
        assertEquals(code, ImmersionRules.strangerCode(id));
        for (int i = 0; i < 200; i++) {
            assertEquals(4, ImmersionRules.strangerCode(UUID.randomUUID()).length());
        }
    }

    @Test
    void coneAcceptsStraightAheadAndRejectsSideways() {
        assertTrue(ImmersionRules.inViewCone(0, 0, 1, 0, 0, 3));
        assertTrue(ImmersionRules.inViewCone(0, 0, 1, 0.3, 0, 4));
        assertFalse(ImmersionRules.inViewCone(0, 0, 1, 3, 0, 3));
        assertFalse(ImmersionRules.inViewCone(0, 0, 1, 0, 0, -3));
    }

    @Test
    void rangeIsFiveBlocks() {
        assertTrue(ImmersionRules.withinLabelRange(25.0));
        assertFalse(ImmersionRules.withinLabelRange(25.1));
    }

    @Test
    void onlyHandsUpBlocksActions() {
        assertTrue(BodyPose.HANDS_UP.blocksActions());
        assertFalse(BodyPose.POINTING.blocksActions());
        assertEquals(BodyPose.NONE, BodyPose.byOrdinal(99));
    }

    @Test
    void knownCitizensRoundTrip() {
        KnownCitizens k = new KnownCitizens();
        UUID a = UUID.randomUUID();
        assertTrue(k.learn(a, "Mario Rossi"));
        assertFalse(k.learn(a, "Mario Rossi"));
        KnownCitizens back = KnownCitizens.fromJson(k.toJson());
        assertTrue(back.knows(a));
        assertEquals("Mario Rossi", back.view().get(a));
        assertTrue(KnownCitizens.fromJson("not json").isEmpty());
    }
}
