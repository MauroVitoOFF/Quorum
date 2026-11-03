package dev.qrm.arrival.client;

import dev.qrm.arrival.logic.ArrivalSession.Phase;

/** Punto d'ingresso dei pacchetti sul client. Caricata solo quando un pacchetto arriva davvero. */
public final class ArrivalClientBridge {
    private ArrivalClientBridge() {}

    public static void onPhase(int phase, int elapsed, int lyingTicks, int risingTicks) {
        Phase[] all = Phase.values();
        Phase p = phase >= 0 && phase < all.length ? all[phase] : Phase.ABORTED;
        ClientArrival.set(p, elapsed, lyingTicks, risingTicks);
    }
}
