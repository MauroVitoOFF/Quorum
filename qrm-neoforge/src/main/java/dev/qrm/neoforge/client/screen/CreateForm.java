package dev.qrm.neoforge.client.screen;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validazione del modulo di creazione (puro): solo comodità lato client, l'esito vero è del server. */
public final class CreateForm {
    public static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    public static final String ERROR_NAME = "qrm.character.form.error.name";
    public static final String ERROR_DATE = "qrm.character.form.error.date";
    private static final Pattern DATE = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{4})");

    public record Parsed(String firstName, String lastName, int year, int month, int day) {}

    public record Validation(Parsed parsed, String errorKey) {
        public boolean valid() {
            return parsed != null;
        }
    }

    private CreateForm() {}

    public static Validation validate(String first, String last, String date, LocalDate today) {
        String f = first == null ? "" : first.strip();
        String l = last == null ? "" : last.strip();
        if (f.isEmpty() || l.isEmpty()) return new Validation(null, ERROR_NAME);
        Matcher m = DATE.matcher(date == null ? "" : date.strip());
        if (!m.matches()) return new Validation(null, ERROR_DATE);
        int day = Integer.parseInt(m.group(1));
        int month = Integer.parseInt(m.group(2));
        int year = Integer.parseInt(m.group(3));
        LocalDate parsed;
        try {
            parsed = LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            return new Validation(null, ERROR_DATE);
        }
        if (parsed.isBefore(MIN_DATE) || parsed.isAfter(today)) return new Validation(null, ERROR_DATE);
        return new Validation(new Parsed(f, l, year, month, day), null);
    }
}
