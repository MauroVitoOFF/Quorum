package dev.qrm.arrival.client;

import dev.qrm.arrival.logic.ArrivalSession.Phase;

/** La fase del risveglio del giocatore locale, come l'ha comunicata il server. */
final class ClientArrival {
    private static Phase phase = Phase.DONE;
    private static int elapsed;
    private static int lyingTicks;
    private static int risingTicks;

    private ClientArrival() {}

    static void set(Phase p, int e, int lying, int rising) {
        phase = p;
        elapsed = e;
        lyingTicks = lying;
        risingTicks = rising;
    }

    static void clear() {
        phase = Phase.DONE;
    }

    /** Vero finché il risveglio è in corso. */
    static boolean active() {
        return phase == Phase.LYING || phase == Phase.RISING;
    }

    static Phase phase() {
        return phase;
    }

    static int elapsed() {
        return elapsed;
    }

    static int lyingTicks() {
        return lyingTicks;
    }

    static int risingTicks() {
        return risingTicks;
    }
}
