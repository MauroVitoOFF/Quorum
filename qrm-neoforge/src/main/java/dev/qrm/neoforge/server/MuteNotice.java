package dev.qrm.neoforge.server;

import dev.qrm.api.moderation.Mute;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Testi (in italiano, come gli altri avvisi del server) mandati al giocatore per mute e avvertimenti. Pura. */
public final class MuteNotice {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private MuteNotice() {}

    /** "permanente" oppure "fino al 09/10 14:32". */
    public static String until(Mute mute, ZoneId zone) {
        return mute.expiresAt().map(e -> "fino al " + TIME.format(e.atZone(zone))).orElse("permanente");
    }

    /** Subito dopo il mute. */
    public static String applied(Mute mute, ZoneId zone) {
        return "Sei stato silenziato (" + until(mute, zone) + "). Motivo: " + mute.reason();
    }

    /** Quando prova a scrivere mentre è silenziato. */
    public static String blocked(Mute mute, ZoneId zone) {
        return "Sei silenziato (" + until(mute, zone) + "). Motivo: " + mute.reason();
    }

    public static String lifted() {
        return "Il tuo silenziamento è stato rimosso.";
    }

    /** All'avvertimento: {@code count} è il numero di avvertimenti dopo questo. */
    public static String warned(int count, String reason) {
        return "Avvertimento dallo staff (" + count + "): " + reason;
    }
}
