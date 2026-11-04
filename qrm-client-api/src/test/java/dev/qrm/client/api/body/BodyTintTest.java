package dev.qrm.client.api.body;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BodyTintTest {
    @Test
    void endpointsAndClamping() {
        assertEquals(BodyTint.HEALTHY, BodyTint.color(0f));
        assertEquals(BodyTint.HEALTHY, BodyTint.color(-3f));
        assertEquals(BodyTint.HEALTHY, BodyTint.color(Float.NaN));
        assertEquals(BodyTint.color(1f), BodyTint.color(7f));
        assertNotEquals(BodyTint.HEALTHY, BodyTint.color(1f));
    }

    @Test
    void stopsAreExactAndAlwaysOpaque() {
        assertEquals(0xFFFFD746, BodyTint.color(0.25f));
        assertEquals(0xFFFA8C32, BodyTint.color(0.5f));
        assertEquals(0xFFEB3C37, BodyTint.color(0.75f));
        for (float s = 0f; s <= 1f; s += 0.05f) {
            assertEquals(0xFF, BodyTint.color(s) >>> 24);
        }
    }

    @Test
    void midpointIsBetweenNeighbours() {
        int r = (BodyTint.color(0.125f) >> 16) & 0xFF;
        assertTrue(r > 0x5C && r < 0xFF);
    }

    @Test
    void statusAndPartIds() {
        assertEquals(7, BodyPart.values().length);
        assertEquals("arm_right", BodyPart.ARM_RIGHT.id());
        assertFalse(BodyStatus.HEALTHY.anyInjured());
        assertTrue(((BodyStatus) p -> p == BodyPart.HEAD ? 0.1f : 0f).anyInjured());
    }
}
