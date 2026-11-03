package dev.qrm.immersion.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImmersionRulesTest {
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
