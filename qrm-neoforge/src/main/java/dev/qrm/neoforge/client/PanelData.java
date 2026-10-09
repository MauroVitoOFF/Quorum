package dev.qrm.neoforge.client;

import dev.qrm.neoforge.net.PanelSnapshot;
import java.util.Optional;

/** Ultimo elenco giocatori ricevuto dal server per il pannello staff HUD. */
public final class PanelData {
    private static volatile PanelSnapshot snapshot;

    private PanelData() {}

    public static void set(PanelSnapshot s) {
        snapshot = s;
    }

    public static Optional<PanelSnapshot> snapshot() {
        return Optional.ofNullable(snapshot);
    }

    public static void reset() {
        snapshot = null;
    }
}
