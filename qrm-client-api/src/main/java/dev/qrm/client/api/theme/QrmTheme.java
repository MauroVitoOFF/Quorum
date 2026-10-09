package dev.qrm.client.api.theme;

/** Tema attivo del client (aggiornato dal listener di ricarica delle risorse di QRM). */
public final class QrmTheme {
    private static volatile ThemeManifest current;

    private QrmTheme() {}

    /** @throws IllegalStateException se il tema non è ancora stato installato (QRM lo fa all'avvio del client) */
    public static ThemeManifest current() {
        ThemeManifest t = current;
        if (t == null) throw new IllegalStateException("QRM theme is not installed yet");
        return t;
    }

    public static void set(ThemeManifest manifest) {
        current = java.util.Objects.requireNonNull(manifest, "manifest");
    }
}
