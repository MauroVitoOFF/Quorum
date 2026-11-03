package dev.qrm.api.clock;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Data, ora e stagione di gioco. La data è quella reale del server; l'ora scorre più veloce
 * (vedi {@code dayMinutes} nella configurazione del Module Clock).
 *
 * @param hour   da 0 a 23
 * @param minute da 0 a 59
 */
public record GameTime(LocalDate date, int hour, int minute, Season season) {
    public GameTime {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(season, "season");
        if (hour < 0 || hour > 23) throw new IllegalArgumentException("hour: " + hour);
        if (minute < 0 || minute > 59) throw new IllegalArgumentException("minute: " + minute);
    }

    /** Vero dalle 6 alle 19 (le 20 escluse): la parte della giornata con la luce. */
    public boolean isDaytime() {
        return hour >= 6 && hour < 20;
    }
}
