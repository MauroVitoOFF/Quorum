package dev.qrm.neoforge.client.screen;

/** Scorrimento e hit-test di una lista a righe di altezza fissa. Puro: nessuna dipendenza da Minecraft. */
final class ListScroll {
    private int offset;

    /** Prima riga visibile, riportata nei limiti per {@code total} righe di cui {@code visible} visibili. */
    int offset(int total, int visible) {
        offset = Math.max(0, Math.min(offset, Math.max(0, total - Math.max(0, visible))));
        return offset;
    }

    /** Scorre di {@code delta} righe (positivo = verso il basso). */
    void scroll(int delta, int total, int visible) {
        offset += delta;
        offset(total, visible);
    }

    void reset() {
        offset = 0;
    }

    /**
     * Indice della riga sotto {@code mouseY} (la lista parte a {@code top}), oppure -1 se fuori dalla zona visibile
     * o oltre l'ultima riga.
     */
    int indexAt(double mouseY, int top, int rowH, int total, int visible) {
        if (rowH <= 0 || mouseY < top) return -1;
        int row = (int) ((mouseY - top) / rowH);
        if (row >= visible) return -1;
        int index = offset(total, visible) + row;
        return index < total ? index : -1;
    }
}
