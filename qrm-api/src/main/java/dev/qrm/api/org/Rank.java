package dev.qrm.api.org;

import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.permission.PermissionNodes;
import java.util.Objects;
import java.util.Set;

/** Un rank di un'organizzazione: eredita i nodi dei rank con {@code level} inferiore della stessa organizzazione. */
public record Rank(String id, String label, int level, Set<String> permissions) {
    public Rank {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(permissions, "permissions");
        if (!JobDefinition.isValidId(id)) throw new IllegalArgumentException("invalid rank id: " + id);
        if (label.isBlank() || label.length() > 64) throw new IllegalArgumentException("label must be 1..64 chars");
        if (level < 0) throw new IllegalArgumentException("level must be >= 0");
        permissions.forEach(PermissionNodes::requirePattern);
        permissions = Set.copyOf(permissions);
    }
}
