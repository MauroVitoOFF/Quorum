package dev.qrm.immersion.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImmersionRulesTest {
    @Test
    void withinRangeComparesSquaredDistanceWithTheRange() {
        assertTrue(ImmersionRules.withinRange(25.0, 5.0));
        assertFalse(ImmersionRules.withinRange(25.1, 5.0));
        assertTrue(ImmersionRules.withinRange(225.0, 15.0));
        assertFalse(ImmersionRules.withinRange(225.1, 15.0));
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
