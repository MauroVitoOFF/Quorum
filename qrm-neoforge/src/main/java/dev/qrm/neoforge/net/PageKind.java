package dev.qrm.neoforge.net;

import dev.qrm.api.staff.StaffNodes;

/** Le pagine di righe che il pannello staff può chiedere al server (voci delle sezioni *Logs & Transactions* e *Moderation*). */
public enum PageKind {
    /** Le ultime azioni del registro staff. */
    LOG(StaffNodes.LOGS, "qrm.staff.log.title"),
    /** I mute attivi (anche di giocatori offline). */
    MUTES(StaffNodes.MODERATION, "qrm.staff.moderation.mutes"),
    /** Gli ultimi avvertimenti di tutto il server. */
    WARNINGS(StaffNodes.MODERATION, "qrm.staff.moderation.warnings");

    private final String node;
    private final String titleKey;

    PageKind(String node, String titleKey) {
        this.node = node;
        this.titleKey = titleKey;
    }

    /** Il nodo che il server ricontrolla prima di rispondere. */
    public String node() {
        return node;
    }

    /** Chiave di traduzione del titolo della pagina. */
    public String titleKey() {
        return titleKey;
    }

    public static PageKind fromOrdinal(int ordinal) {
        PageKind[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : LOG;
    }
}
