package dev.qrm.clock.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.clock.Season;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ClockRulesTest {

    @Test
    void northernSeasonsChangeOnTheSolsticesAndEquinoxes() {
        assertEquals(Season.WINTER, ClockRules.seasonOf(LocalDate.of(2026, 3, 19), false));
        assertEquals(Season.SPRING, ClockRules.seasonOf(LocalDate.of(2026, 3, 20), false));
        assertEquals(Season.SPRING, ClockRules.seasonOf(LocalDate.of(2026, 6, 20), false));
        assertEquals(Season.SUMMER, ClockRules.seasonOf(LocalDate.of(2026, 6, 21), false));
        assertEquals(Season.SUMMER, ClockRules.seasonOf(LocalDate.of(2026, 9, 21), false));
        assertEquals(Season.AUTUMN, ClockRules.seasonOf(LocalDate.of(2026, 9, 22), false));
        assertEquals(Season.AUTUMN, ClockRules.seasonOf(LocalDate.of(2026, 12, 20), false));
        assertEquals(Season.WINTER, ClockRules.seasonOf(LocalDate.of(2026, 12, 21), false));
        assertEquals(Season.WINTER, ClockRules.seasonOf(LocalDate.of(2026, 1, 1), false));
    }

    @Test
    void southernSeasonsAreTheOpposite() {
        assertEquals(Season.SUMMER, ClockRules.seasonOf(LocalDate.of(2026, 12, 25), true));
        assertEquals(Season.WINTER, ClockRules.seasonOf(LocalDate.of(2026, 7, 1), true));
        assertEquals(Season.AUTUMN, ClockRules.seasonOf(LocalDate.of(2026, 4, 1), true));
        assertEquals(Season.SPRING, ClockRules.seasonOf(LocalDate.of(2026, 10, 1), true));
    }
}
