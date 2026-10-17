package dev.qrm.medical.client;

import dev.qrm.medical.logic.MedicalState;

/** Punto d'ingresso dei pacchetti sul client. Caricata solo quando un pacchetto arriva davvero. */
public final class MedicalClientBridge {
    private MedicalClientBridge() {}

    public static void onStatus(int state, int bleedOutTicks, int downedTicks, boolean rcp) {
        ClientMedical.set(MedicalState.byOrdinal(state), bleedOutTicks, downedTicks, rcp);
    }
}
