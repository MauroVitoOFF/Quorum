package dev.qrm.client.api.staff;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Stato di navigazione del pannello staff, senza nulla di Minecraft: uno stack di pagine (Home → sezione, oppure
 * Home → Players → scheda giocatore), righe, selezione, finestra scorrevole e conferma delle azioni pericolose. Legge tutto
 * da una {@link Source} e si ricalcola con {@link #refresh()} (ogni tick).
 */
public final class StaffPanelModel {
    public enum State { ENABLED, DISABLED, SOON }

    /** {@code label} non nullo = testo letterale (es. nome del giocatore); altrimenti si traduce {@code labelKey}. */
    public record Row(String id, String labelKey, String label, String detail, State state, boolean danger,
            boolean deeper, boolean info) {
        public Row(String id, String labelKey, String label, String detail, State state, boolean danger, boolean deeper) {
            this(id, labelKey, label, detail, state, danger, deeper, false);
        }
    }

    /**
     * @param muteUntil 0 = nessun mute, -1 = permanente, altrimenti l'istante di fine in millisecondi
     */
    public record PlayerInfo(UUID id, String name, String character, String job, String organization, long muteUntil,
            int warnings, int notes) {
        public PlayerInfo(UUID id, String name, String character, String job, String organization) {
            this(id, name, character, job, organization, 0, 0, 0);
        }
    }

    /** Riga informativa della scheda; {@code translateValue}: il valore è una chiave di traduzione. */
    public record HeaderLine(String labelKey, String value, boolean translateValue) {}

    /** Riga di una pagina informativa: etichetta (chiave di traduzione) e valore. */
    public record InfoLine(String labelKey, String value) {}

    /** Titolo della pagina: chiave di traduzione oppure testo letterale. */
    public record Title(String key, String literal) {}

    public interface Source {
        List<StaffEntries.Entry> entries();

        List<StaffPlayerActions.Action> playerActions();

        /** Giocatori online; vuoto finché il primo snapshot non è arrivato. */
        Optional<List<PlayerInfo>> players();

        boolean allows(String node);

        boolean hasTarget();
    }

    public sealed interface Outcome {
        record None() implements Outcome {}

        /** Prima pressione di un'azione pericolosa: il chiamante mostra la richiesta di conferma. */
        record Confirm(String actionId) implements Outcome {}

        record Open(StaffEntries.Entry entry) implements Outcome {}

        /** Eseguire l'azione; {@code arg} è la scelta fatta (azioni a scelte) o vuoto. */
        record Run(StaffPlayerActions.Action action, PlayerInfo player, String arg) implements Outcome {}

        /** Aprire la finestrella di input dell'azione: i valori dei campi diventeranno l'argomento. */
        record Input(StaffPlayerActions.Action action, PlayerInfo player) implements Outcome {}
    }

    private enum Kind { HOME, SECTION, PLAYERS, PLAYER, CHOICES, INFO }

    private record Page(Kind kind, String arg) {}

    private static final String PLAYERS_ID = "players";
    private static final String DASH = "—";
    private static final String UNKNOWN_COUNT = "…";

    private final Source source;
    private final Consumer<String> onFailure;
    private final List<Page> stack = new ArrayList<>(List.of(new Page(Kind.HOME, "")));
    private List<StaffEntries.Entry> visible = List.of();
    private Optional<List<PlayerInfo>> players = Optional.empty();
    private boolean hasTarget;
    private List<Row> rows = List.of();
    private List<HeaderLine> header = List.of();
    private Title title = new Title("qrm.staff.panel.title", null);
    private int selected;
    private int scroll;
    private String armed;
    private List<InfoLine> infoLines = List.of();
    private String infoTitleKey = "";

    /** @param onFailure riceve l'id di una voce la cui {@code available} ha lanciato un'eccezione (la voce viene saltata) */
    public StaffPanelModel(Source source, Consumer<String> onFailure) {
        this.source = source;
        this.onFailure = onFailure;
    }

    public void refresh() {
        List<StaffEntries.Entry> out = new ArrayList<>();
        for (StaffEntries.Entry entry : source.entries()) {
            if (!source.allows(entry.node())) continue;
            try {
                if (entry.available().getAsBoolean()) out.add(entry);
            } catch (RuntimeException e) {
                onFailure.accept(entry.id());
            }
        }
        this.visible = out;
        this.players = source.players();
        this.hasTarget = source.hasTarget();
        while (stack.size() > 1) {
            Page p = top();
            boolean stale = (p.kind() == Kind.SECTION && entriesOf(sectionOf(p.arg())).isEmpty())
                    || (p.kind() == Kind.PLAYER && findPlayer(p.arg()).isEmpty())
                    || (p.kind() == Kind.CHOICES && (choiceAction(p.arg()).isEmpty() || playerBelow().isEmpty()));
            if (!stale) break;
            stack.remove(stack.size() - 1);
        }
        rebuild();
    }

    // ---- lettura -------------------------------------------------------------------------------------------

    public List<Row> rows() {
        return rows;
    }

    public List<HeaderLine> header() {
        return header;
    }

    public Title title() {
        return title;
    }

    public int selected() {
        return selected;
    }

    /** 0 = Home, 1 = sezione o lista giocatori, 2 = scheda giocatore. */
    public int depth() {
        return stack.size() - 1;
    }

    /** Il giocatore della scheda aperta, se la scheda è aperta. */
    public Optional<PlayerInfo> currentPlayer() {
        Page p = top();
        return p.kind() == Kind.PLAYER ? findPlayer(p.arg()) : p.kind() == Kind.CHOICES ? playerBelow() : Optional.empty();
    }

    /** Apre una pagina informativa (es. "View permissions"); {@link #back()} la chiude. */
    public void pushInfo(String titleKey, List<InfoLine> lines) {
        this.infoTitleKey = titleKey;
        this.infoLines = List.copyOf(lines);
        push(new Page(Kind.INFO, ""));
    }

    /** Id della riga in attesa di conferma, o null. */
    public String armedId() {
        return armed;
    }

    // ---- navigazione ---------------------------------------------------------------------------------------

    public void move(int delta) {
        if (rows.isEmpty()) return;
        selected = Math.floorMod(selected + delta, rows.size());
        armed = null;
    }

    public void select(int index) {
        if (index >= 0 && index < rows.size()) {
            selected = index;
            if (armed != null && !rows.get(index).id().equals(armed)) armed = null;
        }
    }

    public Outcome enter() {
        if (rows.isEmpty()) return new Outcome.None();
        Row row = rows.get(selected);
        if (row.state() != State.ENABLED) return new Outcome.None();
        Page p = top();
        switch (p.kind()) {
            case HOME -> {
                push(row.id().equals(PLAYERS_ID) ? new Page(Kind.PLAYERS, "") : new Page(Kind.SECTION, row.id()));
                return new Outcome.None();
            }
            case SECTION -> {
                return visible.stream().filter(e -> e.id().equals(row.id())).findFirst()
                        .<Outcome>map(Outcome.Open::new).orElseGet(Outcome.None::new);
            }
            case PLAYERS -> {
                push(new Page(Kind.PLAYER, row.id()));
                return new Outcome.None();
            }
            case PLAYER -> {
                Optional<StaffPlayerActions.Action> action = source.playerActions().stream()
                        .filter(a -> a.id().equals(row.id())).findFirst();
                Optional<PlayerInfo> player = findPlayer(p.arg());
                if (action.isEmpty() || player.isEmpty() || action.get().run().isEmpty()) return new Outcome.None();
                if (!action.get().choices().isEmpty()) {
                    push(new Page(Kind.CHOICES, action.get().id()));
                    return new Outcome.None();
                }
                if (!action.get().fields().isEmpty()) return new Outcome.Input(action.get(), player.get());
                if (action.get().danger() && !row.id().equals(armed)) {
                    armed = row.id();
                    return new Outcome.Confirm(row.id());
                }
                armed = null;
                return new Outcome.Run(action.get(), player.get(), "");
            }
            case CHOICES -> {
                Optional<StaffPlayerActions.Action> action = choiceAction(p.arg());
                Optional<PlayerInfo> player = playerBelow();
                if (action.isEmpty() || player.isEmpty()) return new Outcome.None();
                return new Outcome.Run(action.get(), player.get(), row.id());
            }
            case INFO -> {
                return new Outcome.None();
            }
        }
        return new Outcome.None();
    }

    /** @return true se è tornato alla pagina precedente, false se era già alla Home (il chiamante chiude il pannello) */
    public boolean back() {
        if (stack.size() <= 1) return false;
        Page left = stack.remove(stack.size() - 1);
        armed = null;
        scroll = 0;
        rebuild();
        String id = left.kind() == Kind.PLAYERS ? PLAYERS_ID : left.kind() == Kind.INFO ? "" : left.arg();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).id().equals(id)) selected = i;
        }
        return true;
    }

    // ---- finestra ------------------------------------------------------------------------------------------

    /** Primo indice visibile di una finestra di {@code maxRows} righe che contiene sempre la selezione. */
    public int windowStart(int maxRows) {
        if (selected < scroll) scroll = selected;
        if (selected >= scroll + maxRows) scroll = selected - maxRows + 1;
        return clampScroll(maxRows);
    }

    /** Primo indice della finestra corrente (dopo {@link #windowStart} o {@link #scrollBy}). */
    public int scroll() {
        return scroll;
    }

    /** Rotella: sposta la finestra senza toccare la selezione. */
    public void scrollBy(int delta, int maxRows) {
        scroll += delta;
        clampScroll(maxRows);
    }

    private int clampScroll(int maxRows) {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - maxRows)));
        return scroll;
    }

    // ---- interni -------------------------------------------------------------------------------------------

    private Page top() {
        return stack.get(stack.size() - 1);
    }

    private void push(Page page) {
        stack.add(page);
        selected = 0;
        scroll = 0;
        armed = null;
        rebuild();
    }

    private static StaffEntries.Section sectionOf(String id) {
        for (StaffEntries.Section s : StaffEntries.Section.values()) if (s.id().equals(id)) return s;
        return StaffEntries.Section.SERVER_MANAGEMENT;
    }

    private List<StaffEntries.Entry> entriesOf(StaffEntries.Section section) {
        return visible.stream().filter(e -> e.section() == section).toList();
    }

    private Optional<PlayerInfo> findPlayer(String id) {
        return players.flatMap(list -> list.stream().filter(p -> p.id().toString().equals(id)).findFirst());
    }

    private Optional<StaffPlayerActions.Action> choiceAction(String id) {
        return source.playerActions().stream().filter(a -> a.id().equals(id) && !a.choices().isEmpty()).findFirst();
    }

    /** Il giocatore della pagina PLAYER che sta sotto nello stack (per le pagine a scelte). */
    private Optional<PlayerInfo> playerBelow() {
        for (int i = stack.size() - 1; i >= 0; i--) {
            if (stack.get(i).kind() == Kind.PLAYER) return findPlayer(stack.get(i).arg());
        }
        return Optional.empty();
    }

    private static final DateTimeFormatter MUTE_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    /** La riga *Mute*: — se non c'è (o è scaduto), "permanente" (tradotto) oppure la data di fine. */
    private static HeaderLine muteLine(long muteUntil) {
        String label = "qrm.staff.panel.mute";
        if (muteUntil == -1) return new HeaderLine(label, "qrm.staff.panel.permanent", true);
        if (muteUntil <= System.currentTimeMillis()) return new HeaderLine(label, DASH, false);
        return new HeaderLine(label, MUTE_TIME.format(Instant.ofEpochMilli(muteUntil).atZone(ZoneId.systemDefault())), false);
    }

    private static String dash(String s) {
        return s == null || s.isBlank() ? DASH : s;
    }

    private void rebuild() {
        Page p = top();
        List<Row> out = new ArrayList<>();
        List<HeaderLine> head = List.of();
        Title t = new Title("qrm.staff.panel.title", null);
        switch (p.kind()) {
            case HOME -> {
                out.add(new Row(PLAYERS_ID, StaffEntries.Section.PLAYERS.labelKey(), null,
                        players.map(l -> String.valueOf(l.size())).orElse(UNKNOWN_COUNT), State.ENABLED, false, true));
                for (StaffEntries.Section s : StaffEntries.Section.values()) {
                    if (s == StaffEntries.Section.PLAYERS) continue;
                    out.add(new Row(s.id(), s.labelKey(), null, null,
                            entriesOf(s).isEmpty() ? State.SOON : State.ENABLED, false, true));
                }
            }
            case SECTION -> {
                StaffEntries.Section s = sectionOf(p.arg());
                t = new Title(s.labelKey(), null);
                for (StaffEntries.Entry e : entriesOf(s)) {
                    out.add(new Row(e.id(), e.labelKey(), null, null,
                            !e.requiresTarget() || hasTarget ? State.ENABLED : State.DISABLED, false, false));
                }
            }
            case PLAYERS -> {
                t = new Title(StaffEntries.Section.PLAYERS.labelKey(), null);
                players.ifPresent(list -> list.stream()
                        .sorted(Comparator.comparing(pl -> pl.name().toLowerCase(Locale.ROOT)))
                        .forEach(pl -> out.add(new Row(pl.id().toString(), null, pl.name(), pl.character(),
                                State.ENABLED, false, true))));
            }
            case PLAYER -> {
                Optional<PlayerInfo> found = findPlayer(p.arg());
                if (found.isPresent()) {
                    PlayerInfo pl = found.get();
                    t = new Title(null, pl.name());
                    head = List.of(new HeaderLine("qrm.staff.panel.online", "qrm.staff.panel.yes", true),
                            new HeaderLine("qrm.staff.panel.character", dash(pl.character()), false),
                            new HeaderLine("qrm.staff.panel.job", dash(pl.job()), false),
                            new HeaderLine("qrm.staff.panel.organization", dash(pl.organization()), false),
                            muteLine(pl.muteUntil()),
                            new HeaderLine("qrm.staff.panel.warnings", String.valueOf(pl.warnings()), false),
                            new HeaderLine("qrm.staff.panel.notes", String.valueOf(pl.notes()), false));
                    for (StaffPlayerActions.Action a : source.playerActions()) {
                        if (!source.allows(a.node())) continue;
                        out.add(new Row(a.id(), a.labelKey(), null, null,
                                a.run().isPresent() ? State.ENABLED : State.SOON, a.danger(), false));
                    }
                }
            }
            case CHOICES -> {
                Optional<StaffPlayerActions.Action> action = choiceAction(p.arg());
                if (action.isPresent()) {
                    t = new Title(action.get().labelKey(), null);
                    for (StaffPlayerActions.Choice c : action.get().choices()) {
                        out.add(new Row(c.id(), c.labelKey(), null, null, State.ENABLED, false, false));
                    }
                }
            }
            case INFO -> {
                t = new Title(infoTitleKey, null);
                int i = 0;
                for (InfoLine l : infoLines) {
                    out.add(new Row("info" + i++, l.labelKey(), null, l.value(), State.ENABLED, false, false, true));
                }
            }
        }
        this.rows = out;
        this.header = head;
        this.title = t;
        this.selected = out.isEmpty() ? 0 : Math.max(0, Math.min(selected, out.size() - 1));
        if (armed != null && (out.isEmpty() || !out.get(selected).id().equals(armed))) armed = null;
    }
}
