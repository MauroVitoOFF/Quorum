package dev.qrm.police.logic;

/** Livello di pericolosità di un cittadino, dal più basso al più alto. */
public enum Danger {
    LOW, MEDIUM, HIGH, ARMED;

    public Danger next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Danger parse(String s) {
        try {
            return valueOf(s);
        } catch (RuntimeException e) {
            return LOW;
        }
    }
}
