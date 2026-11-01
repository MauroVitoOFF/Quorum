package dev.qrm.police.logic;

import java.util.Locale;

/** Dove si trova una chiamata nel suo ciclo, dalla ricezione alla conclusione. Distinto dallo stato dell'agente. */
public enum StatoChiamata {
    PENDING("In attesa"), ASSIGNED("Assegnata"), EN_ROUTE("In viaggio"), ON_SCENE("Sul posto"),
    INTERVENING("In intervento"), BACKUP_REQUESTED("Rinforzi richiesti"), CLOSED("Conclusa"), CANCELED("Annullata");

    private final String label;

    StatoChiamata(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Conclusa o annullata: non si cambia più e non compare fra le chiamate attive. */
    public boolean terminal() {
        return this == CLOSED || this == CANCELED;
    }

    /** Un agente è già al lavoro sulla chiamata (conta fra gli interventi in corso). */
    public boolean inProgress() {
        return this == EN_ROUTE || this == ON_SCENE || this == INTERVENING || this == BACKUP_REQUESTED;
    }

    public static StatoChiamata parse(String s, StatoChiamata fallback) {
        try {
            return valueOf(s.toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
