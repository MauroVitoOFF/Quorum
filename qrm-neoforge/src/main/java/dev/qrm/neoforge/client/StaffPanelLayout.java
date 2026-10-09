package dev.qrm.neoforge.client;

/**
 * Geometria del pannello staff in coordinate GUI, pura (nessun Minecraft): serve al disegno e ai clic. {@code titleH} è la
 * fascia del titolo (clic = indietro); {@code headerH} è l'altezza totale dell'intestazione (titolo + righe informative).
 */
record StaffPanelLayout(int x, int y, int width, int titleH, int headerH, int rowH, int maxRows, int totalRows, int start) {
    private int shown() {
        return Math.max(0, Math.min(maxRows, totalRows - start));
    }

    private int rowsTop() {
        return y + headerH;
    }

    /** Quante righe stanno nell'altezza della GUI sotto {@code top}, lasciando {@code fixedBelow} pixel per il resto. */
    static int fitRows(int guiHeight, int top, int fixedBelow, int rowH, int min, int max) {
        return Math.max(min, Math.min(max, (guiHeight - top - fixedBelow) / rowH));
    }

    boolean contains(int mx, int my) {
        return mx >= x && mx < x + width && my >= y && my < rowsTop() + shown() * rowH;
    }

    boolean isTitle(int mx, int my) {
        return mx >= x && mx < x + width && my >= y && my < y + titleH;
    }

    /** Indice assoluto della riga sotto il cursore, o -1. */
    int rowAt(int mx, int my) {
        if (mx < x || mx >= x + width || my < rowsTop()) return -1;
        int i = (my - rowsTop()) / rowH;
        return i < shown() ? start + i : -1;
    }
}
