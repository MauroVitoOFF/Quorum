package dev.qrm.api.org;

/** Esito di un'operazione: {@link Ok} con il valore, oppure {@link Failed} con un {@link OrgError}. */
public sealed interface OrgResult<T> {
    record Ok<T>(T value) implements OrgResult<T> {}

    record Failed<T>(OrgError error) implements OrgResult<T> {}

    static <T> OrgResult<T> ok(T value) {
        return new Ok<>(value);
    }

    static <T> OrgResult<T> fail(OrgError error) {
        return new Failed<>(error);
    }
}
