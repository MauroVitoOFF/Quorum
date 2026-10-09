package dev.qrm.api.job;

import dev.qrm.api.character.CharacterStatus;

public sealed interface AssignResult {
    record Assigned(Assignment assignment) implements AssignResult {}

    record UnknownCharacter() implements AssignResult {}

    /** Il personaggio non è ACTIVE (archiviato o deceduto). */
    record NotUsable(CharacterStatus status) implements AssignResult {}

    record UnknownJob(String jobId) implements AssignResult {}

    record UnknownGrade(String gradeId) implements AssignResult {}

    /** Solo per {@code setGrade}: il personaggio non ha un lavoro. */
    record NoAssignment() implements AssignResult {}

    record Cancelled(String by, String reason) implements AssignResult {}
}
