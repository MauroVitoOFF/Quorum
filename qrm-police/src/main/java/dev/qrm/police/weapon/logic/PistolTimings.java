package dev.qrm.police.weapon.logic;

/** Costanti di tempo della pistola, in tick di gioco (20 al secondo). */
public final class PistolTimings {
    /** Intervallo minimo tra due colpi: 5 tick (0,25 s). */
    public static final int FIRE_COOLDOWN_TICKS = 5;
    /**
     * Intervallo minimo che accetta il server: un tick meno del client (R29). Il client continua a limitare a
     * {@link #FIRE_COOLDOWN_TICKS}; il tick di tolleranza assorbe la variazione di latenza tra due pacchetti.
     */
    public static final int SERVER_FIRE_COOLDOWN_TICKS = FIRE_COOLDOWN_TICKS - 1;
    /** Soglia tra pressione breve e tenuta: 6 tick (0,3 s). */
    public static final int HOLD_THRESHOLD_TICKS = 6;
    /** Durata della transizione di mira: 4 tick (0,2 s). */
    public static final int AIM_TICKS = 4;

    private PistolTimings() {
    }

    /**
     * Vero se è trascorso l'intervallo minimo dall'ultimo colpo. Chi non ha mai sparato
     * passa un tick molto vecchio (ad esempio {@code -1000}), non {@code Long.MIN_VALUE}.
     */
    public static boolean canFire(long lastShotTick, long nowTick) {
        return nowTick - lastShotTick >= FIRE_COOLDOWN_TICKS;
    }

    /** Come {@link #canFire}, con l'intervallo tollerante del server ({@link #SERVER_FIRE_COOLDOWN_TICKS}). */
    public static boolean serverCanFire(long lastShotTick, long nowTick) {
        return nowTick - lastShotTick >= SERVER_FIRE_COOLDOWN_TICKS;
    }
}
