package dev.qrm.police.logic;

import java.util.Locale;

/** Lo stato operativo che un agente dichiara dal proprio tablet. */
public enum StatoAgente {
    PATROL("Disponibile"), BUSY("Occupato"), OFF("Fuori servizio"),
    // Questi tre non si scelgono: seguono la chiamata che l'agente ha in carico.
    EN_ROUTE("In viaggio"), ON_SCENE("Sul posto"), INTERVENING("In intervento");

    private final String label;

    StatoAgente(String label) {
        this.label = label;
    }

    /** Gli stati che l'agente può scegliere da solo; gli altri li ricava il server dalla sua chiamata. */
    public boolean selectable() {
        return this == PATROL || this == BUSY || this == OFF;
    }

    public String label() {
        return label;
    }

    public static StatoAgente parse(String s, StatoAgente fallback) {
        try {
            return valueOf(s.toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
