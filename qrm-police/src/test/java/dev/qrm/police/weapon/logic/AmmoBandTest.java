package dev.qrm.police.weapon.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AmmoBandTest {
    @Test
    void bandsForCapacitySeventeen() {
        assertEquals(AmmoBand.FULL, AmmoBand.of(17, 17));
        assertEquals(AmmoBand.MORE_THAN_HALF, AmmoBand.of(16, 17));
        assertEquals(AmmoBand.MORE_THAN_HALF, AmmoBand.of(9, 17));
        assertEquals(AmmoBand.LESS_THAN_HALF, AmmoBand.of(8, 17));
        assertEquals(AmmoBand.LESS_THAN_HALF, AmmoBand.of(4, 17));
        assertEquals(AmmoBand.ALMOST_EMPTY, AmmoBand.of(3, 17));
        assertEquals(AmmoBand.ALMOST_EMPTY, AmmoBand.of(1, 17));
        assertEquals(AmmoBand.EMPTY, AmmoBand.of(0, 17));
    }

    @Test
    void almostEmptyThresholdHasMinimumOfOne() {
        assertEquals(AmmoBand.ALMOST_EMPTY, AmmoBand.of(1, 2));
        assertEquals(AmmoBand.FULL, AmmoBand.of(2, 2));
        assertEquals(AmmoBand.EMPTY, AmmoBand.of(0, 1));
        assertEquals(AmmoBand.FULL, AmmoBand.of(1, 1));
    }
}
