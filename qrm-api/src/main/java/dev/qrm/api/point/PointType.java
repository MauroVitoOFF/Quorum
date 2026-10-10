package dev.qrm.api.point;

import dev.qrm.api.job.JobDefinition;
import java.util.Objects;
import java.util.Set;

/**
 * Un tipo di punto registrato da un mod.
 *
 * @param labelKey chiave di traduzione del nome mostrato ai giocatori
 * @param owners proprietari ammessi (almeno uno)
 * @param requiresDuty se vero e il proprietario è un job, serve essere in servizio; per le organizzazioni non si applica
 */
public record PointType(String id, String labelKey, Set<PointOwner.Kind> owners, boolean requiresDuty,
        PointHandler handler) {
    public PointType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(labelKey, "labelKey");
        Objects.requireNonNull(owners, "owners");
        Objects.requireNonNull(handler, "handler");
        if (!JobDefinition.isValidId(id)) throw new IllegalArgumentException("invalid point type id: " + id);
        if (owners.isEmpty()) throw new IllegalArgumentException("a point type needs at least one owner kind");
        owners = Set.copyOf(owners);
    }
}
