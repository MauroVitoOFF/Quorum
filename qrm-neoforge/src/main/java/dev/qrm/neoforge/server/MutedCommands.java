package dev.qrm.neoforge.server;

import java.util.Locale;
import java.util.Set;

/** I comandi di messaggistica che un giocatore silenziato non può usare. Pura. */
public final class MutedCommands {
    private static final Set<String> BLOCKED = Set.of("msg", "tell", "w", "me", "teammsg", "tm", "say");
    private static final String NAMESPACE = "minecraft:";

    private MutedCommands() {}

    /** @param input il testo del comando, con o senza la {@code /} iniziale (anche con il prefisso {@code minecraft:}) */
    public static boolean isBlocked(String input) {
        if (input == null) return false;
        String s = input.strip();
        if (s.startsWith("/")) s = s.substring(1);
        int end = s.indexOf(' ');
        String name = (end < 0 ? s : s.substring(0, end)).toLowerCase(Locale.ROOT);
        if (name.startsWith(NAMESPACE)) name = name.substring(NAMESPACE.length());
        return BLOCKED.contains(name);
    }
}
