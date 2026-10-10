package dev.qrm.api.point;

/** Esito di un'operazione sui punti: {@link Ok} con il valore, oppure {@link Failed} con un {@link PointError}. */
public sealed interface PointResult<T> {
    record Ok<T>(T value) implements PointResult<T> {}

    record Failed<T>(PointError error) implements PointResult<T> {}

    static <T> PointResult<T> ok(T value) {
        return new Ok<>(value);
    }

    static <T> PointResult<T> fail(PointError error) {
        return new Failed<>(error);
    }
}
