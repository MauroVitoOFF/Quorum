package dev.qrm.api.job;

import dev.qrm.api.id.CharacterId;
import java.time.Instant;

/**
 * @param since istante della prima assegnazione a questo job (un cambio di grade nello stesso job non lo modifica)
 * @param known false se job o grade non esistono più nelle definizioni caricate
 */
public record Assignment(CharacterId character, String jobId, String gradeId, boolean onDuty, Instant since,
        boolean known) {}
