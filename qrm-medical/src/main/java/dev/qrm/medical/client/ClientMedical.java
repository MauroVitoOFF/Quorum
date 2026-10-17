package dev.qrm.medical.client;

import dev.qrm.medical.logic.MedicalRules;
import dev.qrm.medical.logic.MedicalState;

/** Lo stato medico del giocatore locale, come l'ultimo che il server ha mandato, avanzato tick per tick. */
final class ClientMedical {
    private static MedicalState state = MedicalState.HEALTHY;
    private static int bleedOutTicks;
    private static int downedTicks;
    private static boolean rcp;
    private static int tickCount;

    private ClientMedical() {}

    static void set(MedicalState s, int bleed, int downed, boolean r) {
        state = s;
        bleedOutTicks = bleed;
        downedTicks = downed;
        rcp = r;
    }

    static void reset() {
        set(MedicalState.HEALTHY, 0, 0, false);
    }

    /** Il server manda lo stato ogni secondo; fra un messaggio e l'altro il tempo scorre da solo. */
    static void tick() {
        if (!state.incapacitated()) return;
        tickCount++;
        if (!rcp || tickCount % MedicalRules.RCP_SLOWDOWN == 0) bleedOutTicks = Math.max(0, bleedOutTicks - 1);
        if (!rcp) downedTicks++;
    }

    static MedicalState state() {
        return state;
    }

    static boolean incapacitated() {
        return state.incapacitated();
    }

    static int bleedOutTicks() {
        return bleedOutTicks;
    }

    static int downedTicks() {
        return downedTicks;
    }

    static boolean rcp() {
        return rcp;
    }

    static boolean canSurrender() {
        return state.incapacitated() && downedTicks >= MedicalRules.SURRENDER_AFTER_TICKS;
    }
}
