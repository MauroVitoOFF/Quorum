package dev.qrm.api.point;

import java.time.Instant;
import java.util.Objects;

/**
 * Un blocco esistente marcato dallo staff come punto di interazione.
 *
 * @param id progressivo, usato nei comandi
 * @param minGradeId grado (job) o rank (organizzazione) minimo; null = tutti i membri
 * @param publicVisible se vero il punto è comunicato a tutti i client; l'uso resta vincolato alla regola di accesso
 * @param name nome scelto dallo staff; null se assente
 * @param face faccia su cui mostrare l'icona nel mondo; null = tutte e quattro le laterali
 */
public record InteractionPoint(int id, String typeId, PointOwner owner, PointPosition position, String minGradeId,
        boolean publicVisible, String name, PointFace face, Instant createdAt) {
    public static final int MAX_NAME = 64;

    public InteractionPoint {
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(createdAt, "createdAt");
        if (name != null && (name.isBlank() || name.length() > MAX_NAME)) {
            throw new IllegalArgumentException("name must be 1.." + MAX_NAME + " chars");
        }
    }
}
