package dev.qrm.neoforge.server;

import dev.qrm.neoforge.net.PlayerInfoLine;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Righe delle pagine informative della scheda giocatore ("View character", "View permissions"): coppie (chiave di traduzione
 * dell'etichetta, valore). Pure, senza Minecraft; il client traduce le etichette.
 */
public final class InfoLines {
    /** Righe massime di una pagina (il pacchetto ne ammette 80). */
    public static final int MAX_LINES = 60;
    private static final String DASH = "—";

    private InfoLines() {}

    /** Il lavoro del personaggio, già risolto in etichette. */
    public record JobView(String jobLabel, String gradeLabel, boolean onDuty) {}

    /** Un'organizzazione del personaggio; {@code rankLabel} è vuoto per l'appartenenza implicita dal lavoro. */
    public record OrgView(String orgLabel, Optional<String> rankLabel) {}

    public static List<PlayerInfoLine> character(String firstName, String lastName, LocalDate birthDate, String status,
            Optional<JobView> job, List<OrgView> orgs) {
        List<PlayerInfoLine> out = new ArrayList<>();
        out.add(new PlayerInfoLine("qrm.staff.info.name", (firstName + " " + lastName).strip()));
        out.add(new PlayerInfoLine("qrm.staff.info.birth", birthDate.toString()));
        out.add(new PlayerInfoLine("qrm.staff.info.status", status.toLowerCase(Locale.ROOT)));
        if (job.isPresent()) {
            JobView j = job.get();
            out.add(new PlayerInfoLine("qrm.staff.info.job", j.jobLabel() + " · " + j.gradeLabel()));
            out.add(new PlayerInfoLine(j.onDuty() ? "qrm.staff.info.on_duty" : "qrm.staff.info.off_duty", ""));
        } else {
            out.add(new PlayerInfoLine("qrm.staff.info.job", DASH));
        }
        if (orgs.isEmpty()) {
            out.add(new PlayerInfoLine("qrm.staff.info.org", DASH));
        }
        for (OrgView o : orgs) {
            out.add(new PlayerInfoLine("qrm.staff.info.org",
                    o.rankLabel().map(r -> o.orgLabel() + " · " + r).orElse(o.orgLabel())));
        }
        return cap(out);
    }

    /**
     * @param effective pattern effettivi del personaggio
     * @param grants override a favore (GRANT) e {@code denies} override contrari (DENY), come nodi o pattern
     */
    public static List<PlayerInfoLine> permissions(Set<String> effective, Map<String, Boolean> overrides) {
        List<PlayerInfoLine> out = new ArrayList<>();
        for (String p : new TreeSet<>(effective)) out.add(new PlayerInfoLine("qrm.staff.info.permission", p));
        for (Map.Entry<String, Boolean> e : new TreeMap<>(overrides).entrySet()) {
            out.add(new PlayerInfoLine(e.getValue() ? "qrm.staff.info.override_grant" : "qrm.staff.info.override_deny",
                    e.getKey()));
        }
        if (out.isEmpty()) out.add(new PlayerInfoLine("qrm.staff.info.no_permissions", DASH));
        return cap(out);
    }

    private static List<PlayerInfoLine> cap(List<PlayerInfoLine> lines) {
        return lines.size() <= MAX_LINES ? lines : List.copyOf(lines.subList(0, MAX_LINES));
    }
}
