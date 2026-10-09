package dev.qrm.neoforge.server;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Durata di un ban scritta dallo staff: {@code 30m}, {@code 12h}, {@code 7d}, {@code 2w}; vuoto = permanente. Pura. */
public final class BanDuration {
    public static final Duration MAX = Duration.ofDays(3650);
    private static final Pattern FORMAT = Pattern.compile("(\\d{1,5})\\s*([mhdw])");

    private BanDuration() {}

    /**
     * @return {@link Duration#ZERO} per un ban permanente (testo vuoto), la durata se valida, vuoto se il testo non è valido
     *         o supera {@link #MAX}
     */
    public static Optional<Duration> parse(String raw) {
        if (raw == null || raw.isBlank()) return Optional.of(Duration.ZERO);
        Matcher m = FORMAT.matcher(raw.strip().toLowerCase(Locale.ROOT));
        if (!m.matches()) return Optional.empty();
        long n = Long.parseLong(m.group(1));
        if (n < 1) return Optional.empty();
        Duration d = switch (m.group(2)) {
            case "m" -> Duration.ofMinutes(n);
            case "h" -> Duration.ofHours(n);
            case "d" -> Duration.ofDays(n);
            default -> Duration.ofDays(7 * n);
        };
        return d.compareTo(MAX) > 0 ? Optional.empty() : Optional.of(d);
    }
}
