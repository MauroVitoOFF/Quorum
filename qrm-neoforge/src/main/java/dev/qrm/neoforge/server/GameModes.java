package dev.qrm.neoforge.server;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Modalità di gioco accettate dall'azione "Set gamemode", in forma testuale (pura, senza Minecraft). */
public final class GameModes {
    public static final Set<String> IDS = Set.of("survival", "creative", "adventure", "spectator");

    private GameModes() {}

    /** @return l'id canonico ({@code survival}...) se {@code arg} è una modalità valida (senza badare a maiuscole e spazi) */
    public static Optional<String> parse(String arg) {
        if (arg == null) return Optional.empty();
        String id = arg.strip().toLowerCase(Locale.ROOT);
        return IDS.contains(id) ? Optional.of(id) : Optional.empty();
    }
}
