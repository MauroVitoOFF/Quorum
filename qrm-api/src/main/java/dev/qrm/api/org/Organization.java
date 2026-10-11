package dev.qrm.api.org;

import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.id.AccountId;
import dev.qrm.api.job.JobDefinition;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Un'organizzazione con un conto in comune ({@code account}). Si archivia ({@code ARCHIVED}); si elimina solo se archiviata, vuota e a saldo zero. */
public record Organization(String id, String label, OrgStatus status, AccountId account, Instant createdAt) {
    public Organization {
        requireValid(id, label);
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** @throws IllegalArgumentException se l'id non è {@code [a-z][a-z0-9_]{0,31}} o la label non è 1..64 caratteri */
    public static void requireValid(String id, String label) {
        if (!JobDefinition.isValidId(id)) throw new IllegalArgumentException("invalid organization id: " + id);
        Objects.requireNonNull(label, "label");
        if (label.isBlank() || label.length() > 64) throw new IllegalArgumentException("label must be 1..64 chars");
    }

    /** Titolare del conto dell'organizzazione: stabile e ricostruibile dal solo id. */
    public static OwnerRef ownerOf(String id) {
        UUID uuid = UUID.nameUUIDFromBytes(("qrm-org:" + id).getBytes(StandardCharsets.UTF_8));
        return new OwnerRef(OwnerRef.ORGANIZATION, uuid);
    }
}
