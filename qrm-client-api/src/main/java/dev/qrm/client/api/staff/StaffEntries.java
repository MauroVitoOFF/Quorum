package dev.qrm.client.api.staff;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Registro delle voci del pannello staff (HUD). Ogni voce appartiene a una sezione, dichiara il nodo permesso che
 * serve per vederla e apre una propria schermata: il pannello è solo il navigatore, le azioni vivono nelle schermate.
 */
public final class StaffEntries {
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]{0,31}");
    private static final Pattern NODE = Pattern.compile("[a-z0-9_]+(\\.[a-z0-9_]+)*");
    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private StaffEntries() {}

    /** Le sezioni del pannello, nell'ordine in cui compaiono. {@code PLAYERS} è la lista giocatori e non ha voci. */
    public enum Section {
        PLAYERS("players"), PLAYER_MANAGEMENT("player_management"), SERVER_MANAGEMENT("server_management"),
        MODERATION("moderation"), ECONOMY("economy"), ORGANIZATIONS("organizations"), LOGS("logs"),
        QUICK_ACTIONS("quick_actions");

        private final String id;

        Section(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        /** Chiave di traduzione dell'etichetta della sezione. */
        public String labelKey() {
            return "qrm.staff.section." + id;
        }
    }

    /**
     * Contesto passato all'apertura di una voce: il giocatore bersaglio (la scheda aperta, se c'è, altrimenti quello inquadrato),
     * il suo nome e come tornare al gioco ({@code back}, chiude la schermata della voce).
     */
    public record Context(Optional<UUID> target, String targetName, Runnable back) {
        public Context {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(targetName, "targetName");
            Objects.requireNonNull(back, "back");
        }
    }

    /**
     * @param id {@code [a-z][a-z0-9_]{0,31}}, unico
     * @param node nodo esatto richiesto (es. {@code staff.bank}); l'operatore li ha tutti
     * @param requiresTarget se vero la voce è disattivata finché non c'è un bersaglio
     * @param available leggero: falso = la voce non compare (es. il server non ha il modulo)
     * @param open chiamato all'apertura, sul thread del client; il modulo apre una propria schermata e gestisce la propria rete
     */
    public record Entry(String id, Section section, String labelKey, String node, boolean requiresTarget,
            BooleanSupplier available, Consumer<Context> open) {
        public Entry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(section, "section");
            Objects.requireNonNull(labelKey, "labelKey");
            Objects.requireNonNull(node, "node");
            Objects.requireNonNull(available, "available");
            Objects.requireNonNull(open, "open");
            if (!ID.matcher(id).matches()) throw new IllegalArgumentException("invalid staff entry id: " + id);
            if (!NODE.matcher(node).matches()) throw new IllegalArgumentException("invalid staff node: " + node);
            if (section == Section.PLAYERS) throw new IllegalArgumentException("the players section has no entries");
        }
    }

    /** @throws IllegalStateException se l'id è già registrato */
    public static synchronized void register(Entry entry) {
        Objects.requireNonNull(entry, "entry");
        if (ENTRIES.stream().anyMatch(e -> e.id().equals(entry.id()))) {
            throw new IllegalStateException("staff entry already registered: " + entry.id());
        }
        ENTRIES.add(entry);
    }

    /** Le voci registrate, nell'ordine di registrazione. */
    public static List<Entry> all() {
        return List.copyOf(ENTRIES);
    }

    /** Solo per i test. */
    static synchronized void clearForTests() {
        ENTRIES.clear();
    }
}
