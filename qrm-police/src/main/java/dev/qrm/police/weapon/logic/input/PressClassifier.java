package dev.qrm.police.weapon.logic.input;

/**
 * Distingue la pressione breve dalla tenuta di un tasto, un tick alla volta. Rilasciato prima della soglia: pressione
 * breve, al rilascio. Tenuto fino alla soglia: tenuta, una sola volta, appena la raggiunge (il rilascio poi non manda
 * nulla). Un clic più breve di un tick (visto solo come "cliccato", mai come "giù") conta come pressione breve.
 */
public final class PressClassifier {
    /** Risultato di un tick. */
    public enum Press { NONE, TAP, HOLD }

    private final int threshold;
    private boolean active;
    private boolean held;
    private int ticks;

    /** @param threshold tick di pressione oltre i quali è una tenuta (per la pistola {@code HOLD_THRESHOLD_TICKS}) */
    public PressClassifier(int threshold) {
        this.threshold = threshold;
    }

    /**
     * @param down    il tasto è giù in questo tick
     * @param clicked il tasto è stato premuto almeno una volta dall'ultimo tick
     */
    public Press tick(boolean down, boolean clicked) {
        if (!active) {
            if (!down && !clicked) return Press.NONE;
            active = true;
            held = false;
            ticks = 0;
        }
        if (down) {
            ticks++;
            if (!held && ticks >= threshold) {
                held = true;
                return Press.HOLD;
            }
            return Press.NONE;
        }
        active = false;
        return held ? Press.NONE : Press.TAP;
    }

    /** Interrompe la pressione in corso senza mandare nulla. */
    public void reset() {
        active = false;
        held = false;
        ticks = 0;
    }
}
