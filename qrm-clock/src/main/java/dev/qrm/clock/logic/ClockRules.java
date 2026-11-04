package dev.qrm.clock.logic;

import dev.qrm.api.clock.Season;
import java.time.LocalDate;

/** Le regole pure del calendario di gioco. */
public final class ClockRules {
    private ClockRules() {}

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
}
