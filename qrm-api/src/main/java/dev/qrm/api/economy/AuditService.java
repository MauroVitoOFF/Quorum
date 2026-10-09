package dev.qrm.api.economy;

import java.util.List;

public interface AuditService {
    /** Confronta ogni saldo con la somma delle transazioni. Lista vuota = tutto coerente. */
    List<AuditIssue> verify();
}
