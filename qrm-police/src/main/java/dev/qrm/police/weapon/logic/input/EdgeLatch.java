package dev.qrm.police.weapon.logic.input;

/** Ricorda un valore booleano e segnala solo i cambi (per esempio mira accesa/spenta). */
public final class EdgeLatch {
    private boolean value;

    /** Imposta il valore; vero se è cambiato. */
    public boolean update(boolean v) {
        if (v == value) return false;
        value = v;
        return true;
    }

    public boolean value() {
        return value;
    }

    /** Torna a falso senza segnalare il cambio. */
    public void reset() {
        value = false;
    }
}
