package dev.qrm.police.logic;

import java.util.List;
import java.util.Optional;

/** Le infrazioni sanzionabili con una multa, per categoria. L'importo è sempre quello del catalogo. */
public final class FineCatalog {
    public record Infraction(String code, String name, long amount) {}

    public record Category(String name, String shortName, List<Infraction> infractions) {}

    public static final List<Category> CATEGORIES = List.of(
            new Category("Infrazioni stradali", "Stradali", List.of(
                    new Infraction("STR1", "Eccesso di velocità", 250),
                    new Infraction("STR2", "Semaforo rosso", 300),
                    new Infraction("STR3", "Sosta vietata", 100),
                    new Infraction("STR4", "Guida senza cintura", 150),
                    new Infraction("STR5", "Guida pericolosa", 500),
                    new Infraction("STR6", "Guida senza patente", 800))),
            new Category("Violazioni amministrative", "Amministr.", List.of(
                    new Infraction("AMM1", "Documenti non esibiti", 150),
                    new Infraction("AMM2", "Attività senza licenza", 600),
                    new Infraction("AMM3", "Porto d'armi scaduto", 400),
                    new Infraction("AMM4", "Occupazione abusiva di suolo pubblico", 250),
                    new Infraction("AMM5", "Mancato rispetto di un'ordinanza", 350))),
            new Category("Disturbo della quiete pubblica", "Quiete", List.of(
                    new Infraction("QUI1", "Musica ad alto volume", 200),
                    new Infraction("QUI2", "Schiamazzi notturni", 250),
                    new Infraction("QUI3", "Disturbo della quiete pubblica", 300),
                    new Infraction("QUI4", "Alterco in luogo pubblico", 350))),
            new Category("Aggressioni", "Aggressioni", List.of(
                    new Infraction("AGG1", "Minaccia", 800),
                    new Infraction("AGG2", "Percosse", 1000),
                    new Infraction("AGG3", "Aggressione lieve", 1500),
                    new Infraction("AGG4", "Aggressione aggravata", 3000),
                    new Infraction("AGG5", "Oltraggio a pubblico ufficiale", 600))),
            new Category("Possesso di oggetti illegali", "Illegali", List.of(
                    new Infraction("OGG1", "Possesso di stupefacenti", 1200),
                    new Infraction("OGG2", "Arma bianca vietata", 700),
                    new Infraction("OGG3", "Arma da fuoco non dichiarata", 3000),
                    new Infraction("OGG4", "Strumenti da scasso", 900),
                    new Infraction("OGG5", "Merce rubata", 1500))));

    private FineCatalog() {}

    public static Optional<Infraction> find(String code) {
        return CATEGORIES.stream().flatMap(c -> c.infractions().stream()).filter(i -> i.code().equals(code))
                .findFirst();
    }

    /** Il nome della categoria dell'infrazione, o stringa vuota. */
    public static String categoryOf(String code) {
        return CATEGORIES.stream().filter(c -> c.infractions().stream().anyMatch(i -> i.code().equals(code)))
                .map(Category::name).findFirst().orElse("");
    }
}
