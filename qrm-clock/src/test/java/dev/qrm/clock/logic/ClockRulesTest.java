package dev.qrm.clock.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.clock.Season;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ClockRulesTest {
    private static final long MIN = 60_000L;

    @Test
    void gameMidnightFallsOnTheStartOfEveryCycle() {
        assertEquals(18000, ClockRules.tickOfDay(0, 60));
        assertEquals(18000, ClockRules.tickOfDay(60 * MIN, 60));
        assertEquals(18000, ClockRules.tickOfDay(23 * 60 * MIN, 60));
    }

    @Test
    void aSixtyMinuteCycleWalksThroughDawnNoonAndDusk() {
        assertEquals(0, ClockRules.tickOfDay(15 * MIN, 60));      // alba, le 6
        assertEquals(6000, ClockRules.tickOfDay(30 * MIN, 60));   // mezzogiorno
        assertEquals(12000, ClockRules.tickOfDay(45 * MIN, 60));  // le 18
        assertEquals(ClockRules.tickOfDay(30 * MIN, 60), ClockRules.tickOfDay(90 * MIN, 60));
    }

    @Test
    void aShorterCycleRunsFaster() {
        assertEquals(6000, ClockRules.tickOfDay(15 * MIN, 30));   // meta' di un ciclo da 30 minuti
        assertEquals(18000, ClockRules.tickOfDay(30 * MIN, 30));
    }

    @Test
    void hourAndMinuteFollowTheMinecraftDayThatStartsAtSix() {
        assertEquals(6, ClockRules.hourOf(0));
        assertEquals(12, ClockRules.hourOf(6000));
        assertEquals(0, ClockRules.hourOf(18000));
        assertEquals(23, ClockRules.hourOf(17999));
        assertEquals(0, ClockRules.minuteOf(0));
        assertEquals(30, ClockRules.minuteOf(500));
        assertEquals(59, ClockRules.minuteOf(999));
    }

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

    @Test
    void rateStretchesTheTwentyMinuteVanillaDay() {
        assertEquals(1.0F, ClockRules.rateFor(20), 1e-6);
        assertEquals(1.0F / 3.0F, ClockRules.rateFor(60), 1e-6);
        assertEquals(1200.0F / (90 * 60), ClockRules.rateFor(90), 1e-6);
    }

    @Test
    void deltaTakesTheShortestWayAroundTheDay() {
        assertEquals(100, ClockRules.shortestDelta(100, 200));
        assertEquals(20, ClockRules.shortestDelta(23990, 10));
        assertEquals(-20, ClockRules.shortestDelta(10, 23990));
        assertEquals(0, ClockRules.shortestDelta(5000, 5000));
        assertEquals(12000, ClockRules.shortestDelta(0, 12000));
    }
}
