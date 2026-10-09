package dev.qrm.api.staff;

import dev.qrm.api.permission.PermissionNodes;
import java.util.Collection;

/** Nodi del pannello staff e regole per confrontarli con i pattern concessi ({@code staff.*}, {@code *}). */
public final class StaffNodes {
    public static final String PANEL = "staff.panel";
    public static final String JOBS = "staff.jobs";
    public static final String ORGS = "staff.orgs";
    public static final String PERMS = "staff.perms";
    public static final String BANK = "staff.bank";
    /** Vedere il registro delle azioni staff ("Ultime azioni" e "History"). */
    public static final String LOGS = "staff.logs";
    /** Le pagine della sezione Moderation: mute attivi e ultimi avvertimenti. */
    public static final String MODERATION = "staff.moderation";
    /** Prefisso dei nodi delle voci dei moduli: {@code staff.module.<id>}. */
    public static final String MODULE_PREFIX = "staff.module.";
    /** Prefisso dei nodi delle azioni della scheda giocatore: {@code staff.player.<azione>}. */
    public static final String PLAYER_PREFIX = "staff.player.";

    private StaffNodes() {}

    public static boolean matches(String pattern, String node) {
        if (pattern.equals("*")) return true;
        if (pattern.endsWith(".*")) return node.startsWith(pattern.substring(0, pattern.length() - 1));
        return pattern.equals(node);
    }

    public static boolean matchesAny(Collection<String> patterns, String node) {
        for (String p : patterns) if (matches(p, node)) return true;
        return false;
    }

    /** Un grant è un pattern valido che vive sotto {@code staff.} oppure {@code *}. */
    public static boolean isGrantable(String pattern) {
        return PermissionNodes.isPattern(pattern) && (pattern.equals("*") || pattern.startsWith("staff."));
    }
}
