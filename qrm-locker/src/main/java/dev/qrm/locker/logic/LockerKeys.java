package dev.qrm.locker.logic;

import dev.qrm.api.point.PointOwner;
import java.util.Locale;

/** Chiave dei dati di un armadietto: un armadietto per Character e per proprietario del punto. */
public final class LockerKeys {
    public static final String NAMESPACE = "qrm_locker";

    private LockerKeys() {}

    public static String of(PointOwner owner) {
        return "locker." + (owner.kind() == PointOwner.Kind.JOB ? "job" : "org") + "." + owner.id().toLowerCase(Locale.ROOT);
    }
}
