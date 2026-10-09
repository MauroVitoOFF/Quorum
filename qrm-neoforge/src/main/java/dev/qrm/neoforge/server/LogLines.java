package dev.qrm.neoforge.server;

import dev.qrm.api.staff.StaffLogEntry;
import dev.qrm.neoforge.net.PlayerInfoLine;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Trasforma le voci del registro staff nelle righe della pagina informativa del pannello. L'etichetta è letterale (chiave che
 * inizia con {@code =}): data e nome dello staff; il valore è l'azione con bersaglio e dettaglio. Pura.
 */
public final class LogLines {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private LogLines() {}

    public static List<PlayerInfoLine> build(List<StaffLogEntry> entries, ZoneId zone) {
        if (entries.isEmpty()) return List.of(new PlayerInfoLine("qrm.staff.log.empty", ""));
        List<PlayerInfoLine> out = new ArrayList<>();
        for (StaffLogEntry e : entries) {
            String label = PlayerInfoLine.LITERAL + TIME.format(e.at().atZone(zone)) + " " + e.staffName();
            StringBuilder value = new StringBuilder();
            if (e.outcome() == StaffLogEntry.Outcome.DENIED) value.append("✗ ");
            value.append(e.action());
            e.targetName().filter(n -> !n.isBlank()).ifPresent(n -> value.append(" → ").append(n));
            if (!e.detail().isBlank()) value.append(" · ").append(e.detail());
            out.add(new PlayerInfoLine(label, value.toString()));
        }
        return out;
    }
}
