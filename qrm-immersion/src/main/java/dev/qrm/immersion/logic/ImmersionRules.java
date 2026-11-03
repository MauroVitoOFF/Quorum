package dev.qrm.immersion.logic;

/** Le regole numeriche e pure del modulo: nessuna dipendenza da Minecraft, così sono provabili. */
public final class ImmersionRules {
    /** Tick minimi tra due bussate dello stesso giocatore. */
    public static final int KNOCK_COOLDOWN_TICKS = 10;

    private ImmersionRules() {}

    /** Vero se la distanza (al quadrato) non supera {@code range}, in blocchi. */
    public static boolean withinRange(double distanceSq, double range) {
        return distanceSq <= range * range;
    }
}
