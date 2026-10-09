package dev.qrm.api.economy;

import dev.qrm.api.Money;
import dev.qrm.api.id.AccountId;
import java.time.Instant;
import java.util.UUID;

public record Transaction(
        UUID id,
        UUID groupId,
        AccountId from,
        AccountId to,
        Money amount,
        String reason,
        Instant createdAt,
        String metadataJson,
        String idempotencyKey) {}
