package dev.qrm.police.weapon.logic;

/** Fascia approssimativa di munizioni, l'unica informazione che chi guarda un caricatore ottiene senza contarlo. */
public enum AmmoBand {
    FULL,
    MORE_THAN_HALF,
    LESS_THAN_HALF,
    ALMOST_EMPTY,
    EMPTY;

    /** Fascia per {@code rounds} colpi su {@code capacity}; ALMOST_EMPTY se {@code 1 <= rounds <= max(1, capacity/5)}. */
    public static AmmoBand of(int rounds, int capacity) {
        if (rounds <= 0) {
            return EMPTY;
        }
        if (rounds >= capacity) {
            return FULL;
        }
        if (rounds <= Math.max(1, capacity / 5)) {
            return ALMOST_EMPTY;
        }
        // più della metà: rounds > capacity/2 (in aritmetica reale)
        return rounds * 2 > capacity ? MORE_THAN_HALF : LESS_THAN_HALF;
    }
}
