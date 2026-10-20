package dev.qrm.police.weapon.logic;

/** Costanti di tempo della pistola, in tick di gioco (20 al secondo). */
public final class PistolTimings {
    /** Intervallo minimo tra due colpi: 5 tick (0,25 s). */
    public static final int FIRE_COOLDOWN_TICKS = 5;
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
}
