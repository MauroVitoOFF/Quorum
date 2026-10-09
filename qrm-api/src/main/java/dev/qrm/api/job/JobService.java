package dev.qrm.api.job;

import dev.qrm.api.id.CharacterId;
import java.util.List;
import java.util.Optional;

public interface JobService {
    /** L'assegnazione memorizzata, anche se job/grade non esistono più ({@code known = false}). */
    Optional<Assignment> assignmentOf(CharacterId id);

    /**
     * Assegna un lavoro e un grade. Un job diverso azzera {@code onDuty} e {@code since}; lo stesso job con grade diverso li conserva;
     * lo stesso job e grade non scrive nulla.
     */
    AssignResult assign(CharacterId id, String jobId, String gradeId);

    /** Cambia solo il grade nel job corrente ({@link AssignResult.NoAssignment} se il personaggio non ha lavoro). */
    AssignResult setGrade(CharacterId id, String gradeId);

    /** @return false se il personaggio non aveva lavoro o se un handler di {@link JobChangePreEvent} ha annullato */
    boolean dismiss(CharacterId id);

    /**
     * @return false se il personaggio non ha lavoro, è già nello stato richiesto, oppure si vuole andare in servizio con un
     *         lavoro non noto
     */
    boolean setOnDuty(CharacterId id, boolean onDuty, DutyReason reason);

    /** Personaggi ACTIVE con questo lavoro (anche se il job non è più nelle definizioni). */
    List<CharacterId> membersOf(String jobId);
}
