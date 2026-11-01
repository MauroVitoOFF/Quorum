package dev.qrm.police.weapon.logic.anim;

/**
 * Sequenza di animazione del riempimento a mano di un caricatore (logica pura): preparazione, un giro del ciclo per
 * ogni proiettile spostato, chiusura. Chi la usa (l'animatore del client) le dice quali clip sono in corso e da
 * quanto, e lei risponde quale avviare dopo.
 *
 * <p>I proiettili arrivano da due fonti per lo stesso evento: la previsione del client (subito) e il pacchetto del
 * server (con il numero vero). Il server ha l'ultima parola sulla quantità: la parte già prevista non si conta due
 * volte e l'eventuale eccedenza si aggiunge.
 *
 * <p>Non dipende dalla GUI: continua con l'inventario aperto o chiuso. A fermarla è il server (colpi finiti o
 * riempimento interrotto) con {@link #finish()}: si finisce il giro in corso e si passa alla chiusura, senza
 * interruzioni brusche.
 */
public final class RefillSequence {
    public static final String IN = "refill_in";
    public static final String LOOP = "refill_loop";
    public static final String OUT = "refill_out";
    /** Quanti giri al massimo restano in coda. */
    public static final int MAX_PENDING = 64;

    /** Cosa fare adesso. */
    public enum Step { NONE, PLAY_LOOP, PLAY_OUT, STOP }

    private enum State { IDLE, IN, LOOP, OUT }

    private State state = State.IDLE;
    private int pending;
    private int unconfirmed;

    public static boolean isRefillClip(String clip) {
        return IN.equals(clip) || LOOP.equals(clip) || OUT.equals(clip);
    }

    public boolean active() {
        return state != State.IDLE;
    }

    /** Colpi ancora da inserire (senza il giro in corso). */
    public int pending() {
        return pending;
    }

    /**
     * Nuovi proiettili.
     *
     * @param rounds quanti
     * @param predicted vero se è la previsione del client, falso se arriva dal server
     * @return la clip da avviare subito, o {@code null} se la sequenza è già in corso
     */
    public String request(int rounds, boolean predicted) {
        int fresh = rounds;
        if (predicted) {
            unconfirmed += rounds;
        } else {
            int already = Math.min(unconfirmed, rounds);
            unconfirmed -= already;
            fresh = rounds - already;
        }
        if (fresh <= 0) return null;
        pending = Math.min(MAX_PENDING, pending + fresh);
        if (state == State.IDLE) {
            state = State.IN;
            return IN;
        }
        if (state == State.OUT) {
            // già in chiusura: si riprende dal ciclo
            pending--;
            state = State.LOOP;
            return LOOP;
        }
        return null;
    }

    /** Niente altri colpi: si finisce il giro in corso e si passa alla chiusura. */
    public void finish() {
        pending = 0;
        unconfirmed = 0;
    }

    /** Annulla tutto: un'altra azione ha preso il posto, l'arma non è più in mano o la sequenza non può partire. */
    public void cancel() {
        state = State.IDLE;
        pending = 0;
        unconfirmed = 0;
    }

    /**
     * Avanza.
     *
     * @param currentClip clip d'azione in corso, o {@code null}
     * @param elapsed secondi dall'inizio della clip in corso
     * @param length durata della clip in corso
     */
    public Step tick(String currentClip, double elapsed, double length) {
        if (state == State.IDLE) return Step.NONE;
        if (currentClip == null || !isRefillClip(currentClip)) {
            cancel();
            return Step.NONE;
        }
        if (elapsed < length) return Step.NONE;
        switch (state) {
            case IN, LOOP -> {
                if (pending > 0) {
                    pending--;
                    state = State.LOOP;
                    return Step.PLAY_LOOP;
                }
                state = State.OUT;
                return Step.PLAY_OUT;
            }
            case OUT -> {
                cancel();
                return Step.STOP;
            }
            default -> {
                return Step.NONE;
            }
        }
    }
}
