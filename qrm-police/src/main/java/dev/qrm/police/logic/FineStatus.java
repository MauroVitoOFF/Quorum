package dev.qrm.police.logic;

/** Stato di pagamento di una multa. Da non pagata si passa a pagata o annullata, e lì si ferma. */
public enum FineStatus {
    UNPAID("Non pagata"), PAID("Pagata"), CANCELED("Annullata");

    private final String label;

    FineStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static FineStatus parse(String s, FineStatus fallback) {
        try {
            return valueOf(s);
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
