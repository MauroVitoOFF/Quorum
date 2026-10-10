package dev.qrm.api.point;

/** Motivi attesi per cui un'operazione sui punti non viene eseguita. */
public sealed interface PointError {
    record UnknownType(String typeId) implements PointError {}

    /** Il tipo non accetta questo tipo di proprietario. */
    record OwnerNotAllowed(String typeId, PointOwner.Kind kind) implements PointError {}

    /** Job inesistente, organizzazione inesistente o archiviata. */
    record UnknownOwner(PointOwner owner) implements PointError {}

    record UnknownGrade(String gradeId) implements PointError {}

    /** Un altro punto occupa già questo blocco. */
    record PositionTaken(int pointId) implements PointError {}

    record NotFound(int pointId) implements PointError {}
}
