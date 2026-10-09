package dev.qrm.api.economy;

import dev.qrm.api.id.AccountId;
import java.time.Instant;

public record Account(
        AccountId id, OwnerRef owner, AccountStatus status, boolean overdraftAllowed, Instant createdAt) {}
