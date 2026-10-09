package dev.qrm.api.economy;

import dev.qrm.api.Money;
import dev.qrm.api.id.AccountId;
import java.util.Objects;
import java.util.UUID;

public record TransferRequest(
        AccountId from,
        AccountId to,
        Money amount,
        String reason,
        String idempotencyKey,
        String metadataJson,
        UUID groupId) {

    public TransferRequest {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(metadataJson, "metadataJson");
        if (reason.isBlank() || reason.length() > 128) throw new IllegalArgumentException("reason must be 1..128 chars");
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("idempotencyKey must be 1..128 chars");
        }
    }

    public TransferRequest withAmount(Money newAmount) {
        return new TransferRequest(from, to, newAmount, reason, idempotencyKey, metadataJson, groupId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private AccountId from;
        private AccountId to;
        private Money amount;
        private String reason;
        private String idempotencyKey;
        private String metadataJson = "{}";
        private UUID groupId;

        public Builder from(AccountId v) { this.from = v; return this; }
        public Builder to(AccountId v) { this.to = v; return this; }
        public Builder amount(Money v) { this.amount = v; return this; }
        public Builder reason(String v) { this.reason = v; return this; }
        public Builder idempotencyKey(String v) { this.idempotencyKey = v; return this; }
        public Builder metadataJson(String v) { this.metadataJson = v; return this; }
        /** Collega più transazioni (es. cambio valuta). Se assente, coincide con l'id della transazione. */
        public Builder groupId(UUID v) { this.groupId = v; return this; }

        public TransferRequest build() {
            return new TransferRequest(from, to, amount, reason, idempotencyKey, metadataJson, groupId);
        }
    }
}
