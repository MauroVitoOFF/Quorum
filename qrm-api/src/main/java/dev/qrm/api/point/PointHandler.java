package dev.qrm.api.point;

/** Logica di un tipo di punto. */
@FunctionalInterface
public interface PointHandler {
    /** Chiamato sul thread del server, solo dopo che il Core ha verificato l'accesso. */
    void onUse(PointUse use);
}
