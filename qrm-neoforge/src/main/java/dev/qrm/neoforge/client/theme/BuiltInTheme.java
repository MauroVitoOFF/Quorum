package dev.qrm.neoforge.client.theme;

import dev.qrm.client.api.theme.ThemeManifest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Tema predefinito di QRM, letto dalle risorse di questo mod (non sovrascrivibile da un pack). */
public final class BuiltInTheme {
    private static final String RESOURCE = "/assets/qrm/theme/default.json";
    private static volatile ThemeManifest manifest;

    private BuiltInTheme() {}

    public static ThemeManifest manifest() {
        ThemeManifest m = manifest;
        if (m == null) {
            synchronized (BuiltInTheme.class) {
                if (manifest == null) manifest = load();
                m = manifest;
            }
        }
        return m;
    }

    private static ThemeManifest load() {
        try (InputStream in = BuiltInTheme.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("Missing built-in theme: " + RESOURCE);
            return ThemeManifest.parseBuiltIn(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Built-in theme could not be read: " + e.getMessage(), e);
        }
    }
}
