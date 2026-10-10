package dev.qrm.locker.client;

/** Ponte dal canale di rete al client: caricato solo quando arriva davvero un pacchetto. */
public final class LockerClientBridge {
    private static final long VALID_NANOS = 2_000_000_000L;
    private static long armedAt;
    private static boolean armed;

    private LockerClientBridge() {}

    public static synchronized void arm() {
        armed = true;
        armedAt = System.nanoTime();
    }

    /** Vero una sola volta, entro due secondi dal pacchetto. */
    static synchronized boolean consume() {
        boolean valid = armed && System.nanoTime() - armedAt < VALID_NANOS;
        armed = false;
        return valid;
    }
}
