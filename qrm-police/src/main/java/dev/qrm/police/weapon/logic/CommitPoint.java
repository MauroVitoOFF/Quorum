package dev.qrm.police.weapon.logic;

/** Istante di un'azione in cui lo stato di gioco cambia davvero; corrisponde agli eventi {@code commit} dell'animazione. */
public enum CommitPoint {
    MAG_OUT("mag_out"),
    MAG_IN("mag_in"),
    SLIDE("slide"),
    CHECK("check");

    private final String arg;

    CommitPoint(String arg) {
        this.arg = arg;
    }

    /** Argomento dell'evento {@code commit} nel file di animazione esportato. */
    public String arg() {
        return arg;
    }
}
