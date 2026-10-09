package dev.qrm.neoforge.server;

/** Pulizia dei motivi scritti dallo staff (kick e ban) e lettura dei campi di un input box. Pura. */
public final class Reasons {
    public static final int MAX_LENGTH = 128;

    private Reasons() {}

    /** Spazi normalizzati, lunghezza limitata a {@value #MAX_LENGTH}; se resta vuoto usa {@code fallback}. */
    public static String clean(String raw, String fallback) {
        return clean(raw, fallback, MAX_LENGTH);
    }

    /** Come {@link #clean(String, String)} con un massimo di {@code max} caratteri. */
    public static String clean(String raw, String fallback, int max) {
        String s = raw == null ? "" : raw.strip().replaceAll("\\s+", " ");
        if (s.length() > max) s = s.substring(0, max).strip();
        return s.isEmpty() ? fallback : s;
    }

    /** I valori dei campi di un input box, uno per riga; i mancanti diventano stringhe vuote. */
    public static String[] fields(String arg, int count) {
        String[] parts = (arg == null ? "" : arg).split("\n", -1);
        String[] out = new String[count];
        for (int i = 0; i < count; i++) out[i] = i < parts.length ? parts[i] : "";
        return out;
    }
}
