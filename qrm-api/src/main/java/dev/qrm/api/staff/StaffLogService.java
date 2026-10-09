package dev.qrm.api.staff;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Registro persistente delle azioni staff che cambiano qualcosa (e dei tentativi negati dove il codice li vede). Le voci non
 * scadono mai: si cancellano solo con {@link #pruneOlderThan}.
 */
public interface StaffLogService {
    /** Scrive una voce con l'istante corrente; {@code detail} oltre 256 caratteri viene troncato. */
    void record(Optional<UUID> staffId, String staffName, String action, Optional<UUID> targetId,
            Optional<String> targetName, String detail, StaffLogEntry.Outcome outcome);

    /** Le voci più recenti per prime; {@code limit} limitato a 1..200. */
    List<StaffLogEntry> recent(int limit);

    /** Le voci in cui il giocatore è lo staff <b>o</b> il bersaglio, dalla più recente; {@code limit} limitato a 1..200. */
    List<StaffLogEntry> about(UUID player, int limit);

    /** Cancella le voci precedenti a {@code cutoff}; restituisce quante ne ha cancellate. */
    int pruneOlderThan(Instant cutoff);
}
