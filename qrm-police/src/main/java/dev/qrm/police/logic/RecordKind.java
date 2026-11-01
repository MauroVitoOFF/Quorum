package dev.qrm.police.logic;

/** Che tipo di voce è nello storico di un cittadino. */
public enum RecordKind {
    FINE("Multa"), ARREST("Arresto"), REPORT("Rapporto"), NOTE("Nota"), WARRANT("Mandato");

    private final String label;

    RecordKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Il tipo con questo nome, o {@code fallback} se manca o non si riconosce. */
    public static RecordKind parse(String s, RecordKind fallback) {
        try {
            return valueOf(s);
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
