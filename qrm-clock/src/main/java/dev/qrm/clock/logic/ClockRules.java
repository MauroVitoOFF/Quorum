package dev.qrm.clock.logic;

import dev.qrm.api.clock.Season;
import java.time.LocalDate;

/**
 * Le regole pure del tempo di gioco. Il giorno di Minecraft dura {@value #DAY_TICKS} tick e comincia alle 6 del mattino:
 * il tick 6000 e' mezzogiorno, il 18000 e' mezzanotte.
 */
public final class ClockRules {
    public static final int DAY_TICKS = 24000;
    public static final int MIDNIGHT_TICKS = 18000;
    /** Il giorno di Minecraft senza modifiche: 20 minuti reali. */
    private static final int VANILLA_DAY_SECONDS = 1200;

    private ClockRules() {}

    /**
     * Il tick del giorno di Minecraft ricavato dall'ora reale. Un ciclo dura {@code dayMinutes} minuti e comincia sempre
     * a mezzanotte di gioco: dayMinutes deve dividere 1440, cosi' a mezzanotte reale comincia un ciclo e la data cambia
     * proprio a mezzanotte di gioco.
     *
     * @param millisOfRealDay millisecondi trascorsi dalla mezzanotte reale (nel fuso del server)
     */
    public static int tickOfDay(long millisOfRealDay, int dayMinutes) {
        long cycleMillis = dayMinutes * 60_000L;
        long phase = millisOfRealDay % cycleMillis;
        return (int) ((MIDNIGHT_TICKS + phase * DAY_TICKS / cycleMillis) % DAY_TICKS);
    }

    /** L'ora (0-23) di un tick del giorno: il tick 0 e' l'alba, le 6. */
    public static int hourOf(int tickOfDay) {
        return (tickOfDay / 1000 + 6) % 24;
    }

    public static int minuteOf(int tickOfDay) {
        return (tickOfDay % 1000) * 60 / 1000;
    }

    /** Stagione astronomica: cambia agli equinozi e ai solstizi (20 marzo, 21 giugno, 22 settembre, 21 dicembre). */
    public static Season seasonOf(LocalDate date, boolean southernHemisphere) {
        int md = date.getMonthValue() * 100 + date.getDayOfMonth();
        Season north;
        if (md >= 1221 || md < 320) north = Season.WINTER;
        else if (md < 621) north = Season.SPRING;
        else if (md < 922) north = Season.SUMMER;
        else north = Season.AUTUMN;
        if (!southernHemisphere) return north;
        return switch (north) {
            case WINTER -> Season.SUMMER;
            case SPRING -> Season.AUTUMN;
            case SUMMER -> Season.WINTER;
            case AUTUMN -> Season.SPRING;
        };
    }

    /** La velocita' del orologio di Minecraft (1 = normale) perche' un giorno di gioco duri {@code dayMinutes} minuti. */
    public static float rateFor(int dayMinutes) {
        return (float) VANILLA_DAY_SECONDS / (dayMinutes * 60);
    }

    /** Di quanti tick spostarsi da {@code from} a {@code to} passando per la strada piu' corta, in (-12000, 12000]. */
    public static int shortestDelta(int from, int to) {
        int delta = ((to - from) % DAY_TICKS + DAY_TICKS) % DAY_TICKS;
        return delta > DAY_TICKS / 2 ? delta - DAY_TICKS : delta;
    }
}
