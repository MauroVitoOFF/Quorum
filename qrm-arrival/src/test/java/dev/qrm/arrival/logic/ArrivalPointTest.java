package dev.qrm.arrival.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ArrivalPointTest {
    @Test
    void arrivalPoint_roundTripsThroughJson() {
        ArrivalPoint p = new ArrivalPoint("minecraft:overworld", 10.5, 64.0, -3.25, 180f);
        assertEquals(p, ArrivalPoint.parse(p.toJson()));
    }

    @Test
    void arrivalPoint_parseRejectsMissingFields() {
        assertThrows(IllegalArgumentException.class, () -> ArrivalPoint.parse("{\"dimension\":\"minecraft:overworld\"}"));
        assertThrows(IllegalArgumentException.class, () -> ArrivalPoint.parse("not json"));
    }
}
