package dev.qrm.vehicles.logic;

import dev.qrm.client.api.rig.Rig;

/** Domande sulla gerarchia delle ossa di un modello. */
public final class RigParts {
    private RigParts() {}

    /** {@code bone} è {@code ancestor} o ne discende: un pezzo con dei figli si illumina tutto insieme. */
    public static boolean inside(Rig rig, String bone, String ancestor) {
        String cur = bone;
        for (int guard = 0; cur != null && guard < 64; guard++) {
            if (cur.equals(ancestor)) return true;
            // Il genitore dell'osso radice è null: niente Optional/stream, che non accettano il null.
            String next = null;
            for (Rig.Bone b : rig.bones()) {
                if (b.name().equals(cur)) {
                    next = b.parent();
                    break;
                }
            }
            cur = next;
        }
        return false;
    }
}
