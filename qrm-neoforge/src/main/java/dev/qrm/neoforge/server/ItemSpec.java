package dev.qrm.neoforge.server;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cosa scrive lo staff nell'input box di "Give item": {@code [namespace:]percorso [quantità]}, per esempio {@code diamond 16}
 * o {@code minecraft:apple x3}. Pura: non sa se l'oggetto esiste (lo decide il registro di Minecraft).
 */
public record ItemSpec(String namespace, String path, int amount) {
    public static final int MAX_AMOUNT = 2304;
    private static final Pattern FORMAT =
            Pattern.compile("(?:([a-z0-9_.-]+):)?([a-z0-9_./-]+)(?:\\s+x?(\\d{1,5}))?");

    /** @return vuoto se il testo non è valido o la quantità è fuori da 1..{@value #MAX_AMOUNT} */
    public static Optional<ItemSpec> parse(String raw) {
        if (raw == null) return Optional.empty();
        Matcher m = FORMAT.matcher(raw.strip().toLowerCase(Locale.ROOT));
        if (!m.matches()) return Optional.empty();
        int amount = m.group(3) == null ? 1 : Integer.parseInt(m.group(3));
        if (amount < 1 || amount > MAX_AMOUNT) return Optional.empty();
        return Optional.of(new ItemSpec(m.group(1) == null ? "minecraft" : m.group(1), m.group(2), amount));
    }
}
