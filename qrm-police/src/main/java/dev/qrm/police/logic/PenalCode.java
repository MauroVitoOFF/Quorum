package dev.qrm.police.logic;

import java.util.List;

/** Codice penale di consultazione rapida: sanzione consigliata per ogni reato. */
public final class PenalCode {
    public record Offense(String name, String category, long fine, int jailMonths) {}

    public static final List<Offense> OFFENSES = List.of(
            new Offense("Guida pericolosa", "Traffico", 500, 0),
            new Offense("Eccesso di velocità", "Traffico", 250, 0),
            new Offense("Guida senza patente", "Traffico", 800, 1),
            new Offense("Fuga dal posto di blocco", "Traffico", 1500, 6),
            new Offense("Resistenza a pubblico ufficiale", "Ordine pubblico", 1200, 3),
            new Offense("Disturbo della quiete pubblica", "Ordine pubblico", 300, 0),
            new Offense("Oltraggio a pubblico ufficiale", "Ordine pubblico", 600, 1),
            new Offense("Possesso illegale di arma da fuoco", "Armi", 3000, 12),
            new Offense("Porto abusivo di arma", "Armi", 1800, 6),
            new Offense("Esplosione di colpi in luogo pubblico", "Armi", 2500, 8),
            new Offense("Furto", "Patrimonio", 900, 4),
            new Offense("Rapina", "Patrimonio", 4000, 24),
            new Offense("Danneggiamento", "Patrimonio", 700, 2),
            new Offense("Aggressione", "Persona", 1500, 6),
            new Offense("Rapimento", "Persona", 6000, 48),
            new Offense("Tentato omicidio", "Persona", 9000, 96),
            new Offense("Omicidio", "Persona", 20000, 240),
            new Offense("Spaccio di stupefacenti", "Droga", 3500, 18));

    private PenalCode() {}

    public static List<Offense> filter(String query) {
        String q = query.trim().toLowerCase(java.util.Locale.ROOT);
        if (q.isEmpty()) return OFFENSES;
        return OFFENSES.stream().filter(o -> o.name().toLowerCase(java.util.Locale.ROOT).contains(q)
                || o.category().toLowerCase(java.util.Locale.ROOT).contains(q)).toList();
    }
}
