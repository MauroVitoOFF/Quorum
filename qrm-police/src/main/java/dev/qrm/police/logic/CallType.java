package dev.qrm.police.logic;

import java.util.Locale;

/** Tipo di chiamata. Ogni tipo ha una categoria e una priorità predefinita (P1 critica ... P4 informativa). */
public enum CallType {
    // Emergenze
    ROBBERY("Rapina", "Emergenze", 1), SHOOTING("Sparatoria", "Emergenze", 1), ASSAULT("Aggressione", "Emergenze", 2),
    KIDNAPPING("Sequestro di persona", "Emergenze", 1), MEDICAL("Soccorso medico", "Emergenze", 2),
    // Sicurezza
    THEFT("Furto", "Sicurezza", 3), BURGLARY("Effrazione", "Sicurezza", 2), SUSPECT("Persona sospetta", "Sicurezza", 3),
    DISTURBANCE("Disturbo della quiete", "Sicurezza", 3),
    // Supporto
    BACKUP("Richiesta rinforzi", "Supporto", 2), ESCORT("Scorta", "Supporto", 3),
    UNIT_SUPPORT("Supporto a un'unità", "Supporto", 2),
    // Amministrativo
    REPORT("Segnalazione cittadino", "Amministrativo", 4), ROUTINE("Controllo di routine", "Amministrativo", 4),
    OTHER("Altre segnalazioni", "Amministrativo", 4),
    // Panico e chiamate generiche (da altri moduli o dati vecchi)
    PANIC("SOS agente", "Panico", 1), POLICE("Chiamata di polizia", "Emergenze", 3);

    private final String label;
    private final String category;
    private final int defaultPriority;

    CallType(String label, String category, int defaultPriority) {
        this.label = label;
        this.category = category;
        this.defaultPriority = defaultPriority;
    }

    public String label() {
        return label;
    }

    public String category() {
        return category;
    }

    public int defaultPriority() {
        return defaultPriority;
    }

    public static CallType parse(String s) {
        try {
            return valueOf(s.toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return POLICE;
        }
    }
}
