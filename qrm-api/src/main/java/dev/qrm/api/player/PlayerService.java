package dev.qrm.api.player;

import dev.qrm.api.id.PlayerId;
import java.util.Optional;

public interface PlayerService {
    /** Crea il giocatore se non esiste, altrimenti aggiorna {@code lastSeen}. */
    QrmPlayer onJoin(PlayerId id);

    Optional<QrmPlayer> find(PlayerId id);
}
