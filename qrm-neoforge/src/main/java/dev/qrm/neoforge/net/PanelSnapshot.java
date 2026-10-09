package dev.qrm.neoforge.net;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Giocatori online mostrati dal pannello staff. */
public record PanelSnapshot(List<PlayerEntry> players) {
    public static final PanelSnapshot EMPTY = new PanelSnapshot(List.of());

    public PanelSnapshot {
        players = List.copyOf(players);
    }

    /**
     * {@code character}: nome del personaggio attivo; {@code job}: etichetta del lavoro; {@code organization}: etichette delle
     * organizzazioni attive separate da virgola. Vuoti se il giocatore non li ha. {@code standing}: stato di moderazione.
     */
    public record PlayerEntry(UUID id, String name, Optional<String> character, Optional<String> job,
            Optional<String> organization, Standing standing) {
        public PlayerEntry(UUID id, String name, Optional<String> character, Optional<String> job,
                Optional<String> organization) {
            this(id, name, character, job, organization, Standing.NONE);
        }
    }

    /**
     * Stato di moderazione di un giocatore: {@code muteUntil} = 0 nessun mute, -1 permanente, altrimenti l'istante di fine in
     * millisecondi; poi il numero di avvertimenti e di note.
     */
    public record Standing(long muteUntil, int warnings, int notes) {
        public static final Standing NONE = new Standing(0, 0, 0);
        public static final long PERMANENT = -1;
    }
}
