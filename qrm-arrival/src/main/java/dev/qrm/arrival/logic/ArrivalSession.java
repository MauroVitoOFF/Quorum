package dev.qrm.arrival.logic;

/**
 * Le fasi del risveglio di un giocatore, tick per tick: a terra, il rialzo, finito. Il blocco ha una durata massima:
 * oltre quella la sessione finisce senza risultare completata (niente kit, niente contrassegno).
 */
public final class ArrivalSession {
    public enum Phase { LYING, RISING, DONE, ABORTED }

    private final int lyingTicks;
    private final int risingTicks;
    private final int maxLockTicks;
    private int elapsed;
    private Phase phase = Phase.LYING;
    private boolean completed;

    public ArrivalSession(int lyingTicks, int risingTicks, int maxLockTicks) {
        this.lyingTicks = lyingTicks;
        this.risingTicks = risingTicks;
        this.maxLockTicks = maxLockTicks;
    }

    /** Avanza di un tick e torna la fase corrente. Dopo DONE o ABORTED non cambia più niente. */
    public Phase tick() {
        if (phase == Phase.DONE || phase == Phase.ABORTED) return phase;
        elapsed++;
        if (elapsed >= lyingTicks + risingTicks) {
            phase = Phase.DONE;
            completed = true;
        } else if (elapsed >= maxLockTicks) {
            phase = Phase.DONE;
        } else if (elapsed >= lyingTicks) {
            phase = Phase.RISING;
        }
        return phase;
    }

    public Phase phase() {
        return phase;
    }

    public int elapsed() {
        return elapsed;
    }

    public boolean locksPlayer() {
        return phase == Phase.LYING || phase == Phase.RISING;
    }

    public void abort() {
        if (phase != Phase.DONE) phase = Phase.ABORTED;
    }

    /** Vero solo se il risveglio è arrivato alla fine da sé. */
    public boolean completed() {
        return completed;
    }
}
