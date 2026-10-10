package dev.qrm.api.point;

import dev.qrm.api.job.JobDefinition;
import java.util.Objects;

/** Proprietario di un punto: un lavoro o un'organizzazione, identificato dal suo id. */
public record PointOwner(Kind kind, String id) {
    public enum Kind { JOB, ORGANIZATION }

    public PointOwner {
        Objects.requireNonNull(kind, "kind");
        if (!JobDefinition.isValidId(id)) throw new IllegalArgumentException("invalid owner id: " + id);
    }

    public static PointOwner job(String id) {
        return new PointOwner(Kind.JOB, id);
    }

    public static PointOwner organization(String id) {
        return new PointOwner(Kind.ORGANIZATION, id);
    }
}
