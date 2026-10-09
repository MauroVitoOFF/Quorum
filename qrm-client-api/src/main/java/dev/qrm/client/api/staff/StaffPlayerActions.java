package dev.qrm.client.api.staff;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Registro delle azioni della scheda giocatore del pannello staff. Un'azione compare solo se l'account ha il suo nodo
 * ({@code staff.player.<id>} per quelle di QRM). Ha una di tre forme: semplice (una pressione), a scelte (il pannello apre
 * una pagina di voci e passa la scelta come {@link Context#arg()}) o con input (il pannello apre una finestrella con uno o più
 * campi e passa i valori, uno per riga, come {@link Context#arg()}). Senza esecuzione ({@link Action#planned}) il pulsante è
 * visibile ma disattivato con l'etichetta "presto"; registrando l'esecuzione si accende da solo. Il server deve comunque
 * ricontrollare il nodo a ogni pacchetto dell'azione.
 */
public final class StaffPlayerActions {
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]{0,31}");
    private static final Pattern NODE = Pattern.compile("[a-z0-9_]+(\\.[a-z0-9_]+)*");
    private static final List<Action> ACTIONS = new CopyOnWriteArrayList<>();

    private StaffPlayerActions() {}

    /**
     * Il giocatore su cui si agisce e come tornare al gioco ({@code back}, chiude la schermata eventualmente aperta).
     * {@code arg} è la scelta (azioni a scelte) o i valori dei campi separati da {@code \n} (azioni con input); vuoto per le
     * azioni semplici.
     */
    public record Context(UUID player, String name, Runnable back, String arg) {
        public Context {
            Objects.requireNonNull(player, "player");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(back, "back");
            Objects.requireNonNull(arg, "arg");
        }
    }

    /** Una voce di un'azione a scelte. */
    public record Choice(String id, String labelKey) {
        public Choice {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(labelKey, "labelKey");
        }
    }

    /** Un campo della finestrella di input di un'azione. */
    public record Field(String labelKey, String hintKey, int maxLength, boolean required) {
        public Field {
            Objects.requireNonNull(labelKey, "labelKey");
            Objects.requireNonNull(hintKey, "hintKey");
            if (maxLength < 1 || maxLength > 256) throw new IllegalArgumentException("invalid field length: " + maxLength);
        }
    }

    /**
     * @param id {@code [a-z][a-z0-9_]{0,31}}, unico
     * @param node nodo esatto richiesto
     * @param danger azione pericolosa: riga in rosso e, se semplice, seconda pressione di conferma
     * @param run esecuzione; vuota = azione pianificata, non ancora implementata
     * @param choices scelte (non vuote = azione a scelte)
     * @param fields campi di input (non vuoti = azione con input)
     */
    public record Action(String id, String labelKey, String node, boolean danger, Optional<Consumer<Context>> run,
            List<Choice> choices, List<Field> fields) {
        public Action {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(labelKey, "labelKey");
            Objects.requireNonNull(node, "node");
            Objects.requireNonNull(run, "run");
            choices = List.copyOf(choices);
            fields = List.copyOf(fields);
            if (!ID.matcher(id).matches()) throw new IllegalArgumentException("invalid staff action id: " + id);
            if (!NODE.matcher(node).matches()) throw new IllegalArgumentException("invalid staff node: " + node);
            if (!choices.isEmpty() && !fields.isEmpty()) {
                throw new IllegalArgumentException("an action cannot have both choices and input fields: " + id);
            }
        }

        /** Azione pianificata: visibile ma disattivata ("presto"). */
        public static Action planned(String id, String labelKey, String node, boolean danger) {
            return new Action(id, labelKey, node, danger, Optional.empty(), List.of(), List.of());
        }

        /** Azione semplice: una pressione esegue {@code run}. */
        public static Action of(String id, String labelKey, String node, boolean danger, Consumer<Context> run) {
            return new Action(id, labelKey, node, danger, Optional.of(Objects.requireNonNull(run, "run")), List.of(),
                    List.of());
        }

        /** Azione a scelte: la scelta arriva in {@link Context#arg()}. */
        public static Action withChoices(String id, String labelKey, String node, boolean danger, List<Choice> choices,
                Consumer<Context> run) {
            if (choices.isEmpty()) throw new IllegalArgumentException("choices must not be empty: " + id);
            return new Action(id, labelKey, node, danger, Optional.of(Objects.requireNonNull(run, "run")), choices,
                    List.of());
        }

        /** Azione con input: i valori dei campi, uno per riga, arrivano in {@link Context#arg()}. */
        public static Action withInput(String id, String labelKey, String node, boolean danger, List<Field> fields,
                Consumer<Context> run) {
            if (fields.isEmpty()) throw new IllegalArgumentException("fields must not be empty: " + id);
            return new Action(id, labelKey, node, danger, Optional.of(Objects.requireNonNull(run, "run")), List.of(), fields);
        }
    }

    /** @throws IllegalStateException se l'id è già registrato */
    public static synchronized void register(Action action) {
        Objects.requireNonNull(action, "action");
        if (ACTIONS.stream().anyMatch(a -> a.id().equals(action.id()))) {
            throw new IllegalStateException("staff action already registered: " + action.id());
        }
        ACTIONS.add(action);
    }

    /** Le azioni registrate, nell'ordine di registrazione. */
    public static List<Action> all() {
        return List.copyOf(ACTIONS);
    }

    /** Solo per i test. */
    static synchronized void clearForTests() {
        ACTIONS.clear();
    }
}
