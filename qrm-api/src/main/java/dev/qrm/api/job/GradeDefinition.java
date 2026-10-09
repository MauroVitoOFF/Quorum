package dev.qrm.api.job;

import dev.qrm.api.Money;
import dev.qrm.api.permission.PermissionNodes;
import java.util.Objects;
import java.util.Set;

/** Un grado di un lavoro. {@code salary} è solo un dato (può essere null): il Core non paga mai. */
public record GradeDefinition(String id, String label, int level, Set<String> permissions, Money salary) {
    public GradeDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(permissions, "permissions");
        if (!JobDefinition.isValidId(id)) throw new IllegalArgumentException("invalid grade id: " + id);
        if (label.isBlank() || label.length() > 64) throw new IllegalArgumentException("label must be 1..64 chars");
        if (level < 0) throw new IllegalArgumentException("level must be >= 0");
        permissions.forEach(PermissionNodes::requirePattern);
        permissions = Set.copyOf(permissions);
    }
}
