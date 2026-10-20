package dev.qrm.immersion.mc;

import dev.qrm.medical.mc.MedicalServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Il modulo medico è una dipendenza morbida: senza di lui nessuno è mai a terra.
 * Il riferimento a MedicalServer sta solo qui, e si risolve solo se il modulo è caricato.
 */
final class MedicalLink {
    private static final boolean PRESENT = ModList.get().isLoaded("qrm_medical");

    private MedicalLink() {}

    /** Vero se il giocatore è a terra o svenuto (o peggio): non può agire né essere presentato. */
    static boolean unfit(ServerPlayer p) {
        return PRESENT && Probe.unfit(p);
    }

    private static final class Probe {
        static boolean unfit(ServerPlayer p) {
            return MedicalServer.stateOf(p) != dev.qrm.medical.logic.MedicalState.HEALTHY;
        }
    }
}
