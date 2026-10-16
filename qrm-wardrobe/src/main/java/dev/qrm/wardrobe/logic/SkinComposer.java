package dev.qrm.wardrobe.logic;

/**
 * Ricompone una skin: il corpo viene dalla divisa, la testa (x 0-31, y 0-15) e il suo strato esterno (x 32-63, y 0-15)
 * dalla skin originale del giocatore. Pixel ARGB, 64 di larghezza, riga per riga.
 */
public final class SkinComposer {
    public static final int SIZE = 64;
    private static final int HEAD_ROWS = 16;

    private SkinComposer() {}

    /** Una "skin originale" 64x32 con la sola testa (x 0-31, y 0-15) a tinta unita: per il manichino. */
    public static int[] flatHead(int argb) {
        int[] head = new int[SIZE * SIZE / 2];
        for (int y = 0; y < HEAD_ROWS; y++) {
            for (int x = 0; x < SIZE / 2; x++) head[y * SIZE + x] = argb;
        }
        return head;
    }

    public static int[] compose(int[] outfit, int[] original) {
        if (outfit.length != SIZE * SIZE) throw new IllegalArgumentException("la divisa deve essere 64x64");
        if (original.length != SIZE * SIZE && original.length != SIZE * SIZE / 2) {
            throw new IllegalArgumentException("la skin originale deve essere 64x64 o 64x32");
        }
        int[] out = outfit.clone();
        System.arraycopy(original, 0, out, 0, SIZE * HEAD_ROWS);
        return out;
    }
}
