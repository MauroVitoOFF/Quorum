package dev.qrm.api.economy;

import dev.qrm.api.id.AccountId;

public record AuditIssue(AccountId account, String currency, long expected, long actual) {}
