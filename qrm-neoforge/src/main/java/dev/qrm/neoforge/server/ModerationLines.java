package dev.qrm.neoforge.server;

import dev.qrm.api.moderation.Mute;
import dev.qrm.api.moderation.StaffNote;
import dev.qrm.api.moderation.Warning;
import dev.qrm.neoforge.net.PlayerInfoLine;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Righe delle pagine di moderazione del pannello. L'etichetta è letterale (chiave che inizia con {@code =}), il valore è il
 * testo; l'elenco vuoto diventa una riga segnaposto con una chiave di traduzione. Pure.
 */
public final class ModerationLines {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private ModerationLines() {}

    private static String time(Instant at, ZoneId zone) {
        return TIME.format(at.atZone(zone));
    }

    private static PlayerInfoLine literal(String label, String value) {
        return new PlayerInfoLine(PlayerInfoLine.LITERAL + label, value);
    }

    /** Gli avvertimenti di un giocatore: data e staff, poi il motivo. */
    public static List<PlayerInfoLine> warningsOf(List<Warning> warnings, ZoneId zone) {
        if (warnings.isEmpty()) return List.of(new PlayerInfoLine("qrm.staff.moderation.empty_warnings", ""));
        return warnings.stream().map(w -> literal(time(w.at(), zone) + " " + w.staffName(), w.reason())).toList();
    }

    /** Gli ultimi avvertimenti del server: data e giocatore, poi chi li ha dati e il motivo. */
    public static List<PlayerInfoLine> recentWarnings(List<Warning> warnings, ZoneId zone) {
        if (warnings.isEmpty()) return List.of(new PlayerInfoLine("qrm.staff.moderation.empty_warnings", ""));
        return warnings.stream()
                .map(w -> literal(time(w.at(), zone) + " " + w.playerName(), w.staffName() + ": " + w.reason())).toList();
    }

    /** Le note su un giocatore: data e staff, poi il testo. */
    public static List<PlayerInfoLine> notes(List<StaffNote> notes, ZoneId zone) {
        if (notes.isEmpty()) return List.of(new PlayerInfoLine("qrm.staff.moderation.empty_notes", ""));
        return notes.stream().map(n -> literal(time(n.at(), zone) + " " + n.staffName(), n.text())).toList();
    }

    /** I mute attivi: giocatore, poi scadenza, motivo e staff. */
    public static List<PlayerInfoLine> mutes(List<Mute> mutes, ZoneId zone) {
        if (mutes.isEmpty()) return List.of(new PlayerInfoLine("qrm.staff.moderation.empty_mutes", ""));
        return mutes.stream().map(m -> literal(m.playerName(),
                MuteNotice.until(m, zone) + " · " + m.reason() + " (" + m.staffName() + ")")).toList();
    }
}
