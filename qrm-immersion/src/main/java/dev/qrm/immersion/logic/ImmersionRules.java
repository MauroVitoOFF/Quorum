package dev.qrm.immersion.logic;

import java.util.UUID;

/** Le regole numeriche e pure del modulo: nessuna dipendenza da Minecraft, così sono provabili. */
public final class ImmersionRules {
    /** Oltre questa distanza (in blocchi) un'etichetta non si vede mai. */
    public static final double LABEL_RANGE = 5.0;
    /** Coseno dell'angolo massimo tra lo sguardo e il bersaglio perché l'etichetta compaia (circa 16°). */
    public static final double LABEL_VIEW_DOT = 0.96;
    /** Distanza massima per stringere la mano. */
    public static final double HANDSHAKE_REACH = 3.0;
    /** Per quanti tick resta valida una richiesta di presentazione. */
    public static final int HANDSHAKE_TICKS = 20 * 30;
    /** Tick minimi tra due bussate dello stesso giocatore. */
    public static final int KNOCK_COOLDOWN_TICKS = 10;
    public static final double KNOCK_HEARD_NORMAL = 10.0;
    public static final double KNOCK_HEARD_LOUD = 20.0;
    /** Distanza massima dal blocco per sedersi. */
    public static final double SIT_REACH = 2.5;
    /** Dove si siede il giocatore rispetto alla faccia superiore del blocco (vedi SeatEntity). */
    public static final double SEAT_LIFT = 0.1;

    private ImmersionRules() {}

    /** Le 4 cifre di "Sconosciuto #XXXX": stabili per lo stesso UUID, uguali su server e client. */
    public static String strangerCode(UUID id) {
        int n = (int) ((id.getMostSignificantBits() ^ id.getLeastSignificantBits()) & 0x7fffffffL) % 10000;
        return String.format("%04d", n);
    }

    /** Vero se il bersaglio sta dentro il cono di sguardo ({@code look} è unitario). */
    public static boolean inViewCone(double lookX, double lookY, double lookZ, double toX, double toY, double toZ) {
        double len = Math.sqrt(toX * toX + toY * toY + toZ * toZ);
        if (len < 1.0e-6) return true;
        return (lookX * toX + lookY * toY + lookZ * toZ) / len >= LABEL_VIEW_DOT;
    }

    public static boolean withinLabelRange(double distanceSq) {
        return distanceSq <= LABEL_RANGE * LABEL_RANGE;
    }
}
