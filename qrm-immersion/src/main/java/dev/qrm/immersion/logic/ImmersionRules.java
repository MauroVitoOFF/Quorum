package dev.qrm.immersion.logic;

/** Le regole numeriche e pure del modulo: nessuna dipendenza da Minecraft, così sono provabili. */
public final class ImmersionRules {
    /** Oltre questa distanza (in blocchi) un'etichetta non si vede mai. */
    public static final double LABEL_RANGE = 5.0;
    /** Distanza massima per stringere la mano. */
    public static final double HANDSHAKE_REACH = 3.0;
    /** Per quanti tick resta valida una richiesta di presentazione. */
    public static final int HANDSHAKE_TICKS = 20 * 30;
    /** Tick minimi tra due bussate dello stesso giocatore. */
    public static final int KNOCK_COOLDOWN_TICKS = 10;
    public static final double KNOCK_HEARD_NORMAL = 10.0;
    public static final double KNOCK_HEARD_LOUD = 20.0;

    private ImmersionRules() {}

    public static boolean withinLabelRange(double distanceSq) {
        return distanceSq <= LABEL_RANGE * LABEL_RANGE;
    }
}
