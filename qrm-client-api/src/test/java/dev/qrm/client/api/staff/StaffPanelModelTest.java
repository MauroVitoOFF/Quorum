package dev.qrm.client.api.staff;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.client.api.staff.StaffPanelModel.Outcome;
import dev.qrm.client.api.staff.StaffPanelModel.PlayerInfo;
import dev.qrm.client.api.staff.StaffPanelModel.Row;
import dev.qrm.client.api.staff.StaffPanelModel.State;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaffPanelModelTest {
    private static final UUID ANNA_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID ZED_ID = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private final Set<String> allowed = new HashSet<>();
    private final List<StaffEntries.Entry> entries = new ArrayList<>();
    private final List<StaffPlayerActions.Action> actions = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();
    private Optional<List<PlayerInfo>> players = Optional.empty();
    private boolean target;
    private StaffPanelModel model;

    @BeforeEach
    void setUp() {
        model = new StaffPanelModel(new StaffPanelModel.Source() {
            @Override
            public List<StaffEntries.Entry> entries() {
                return entries;
            }

            @Override
            public List<StaffPlayerActions.Action> playerActions() {
                return actions;
            }

            @Override
            public Optional<List<PlayerInfo>> players() {
                return players;
            }

            @Override
            public boolean allows(String node) {
                return allowed.contains(node);
            }

            @Override
            public boolean hasTarget() {
                return target;
            }
        }, failures::add);
    }

    private static StaffEntries.Entry entry(String id, StaffEntries.Section s, String node, boolean needsTarget,
            BooleanSupplier available) {
        return new StaffEntries.Entry(id, s, "k." + id, node, needsTarget, available, c -> {});
    }

    private Row row(String id) {
        return model.rows().stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }

    /** Seleziona la riga e la apre (solo per le pagine di navigazione). */
    private void go(String rowId) {
        model.select(model.rows().indexOf(row(rowId)));
        model.enter();
    }

    private void twoPlayers() {
        players = Optional.of(List.of(new PlayerInfo(ZED_ID, "Zed", "Zed Rossi", null, null),
                new PlayerInfo(ANNA_ID, "anna", "Anna Bianchi", "police", "acme")));
    }

    /** Home → Players → scheda di anna. */
    private void openAnna() {
        model.refresh();
        go("players");
        model.enter();
    }

    @Test
    void homeListsPlayersThenEverySectionAndMarksEmptyOnesAsSoon() {
        allowed.add("staff.bank");
        entries.add(entry("bank", StaffEntries.Section.ECONOMY, "staff.bank", false, () -> true));
        model.refresh();

        assertEquals(0, model.depth());
        assertEquals("players", model.rows().get(0).id());
        assertEquals("…", row("players").detail(), "elenco non ancora arrivato");
        assertEquals(State.ENABLED, row("economy").state());
        assertEquals(State.SOON, row("moderation").state());
        assertEquals(StaffEntries.Section.values().length, model.rows().size());
    }

    @Test
    void entriesWithoutThePermissionAreHiddenAndFailingOnesAreReported() {
        allowed.addAll(Set.of("staff.ok", "staff.boom"));
        entries.add(entry("ok", StaffEntries.Section.LOGS, "staff.ok", false, () -> true));
        entries.add(entry("secret", StaffEntries.Section.LOGS, "staff.secret", false, () -> true));
        entries.add(entry("off", StaffEntries.Section.LOGS, "staff.ok", false, () -> false));
        entries.add(entry("boom", StaffEntries.Section.LOGS, "staff.boom", false, () -> {
            throw new IllegalStateException("x");
        }));
        model.refresh();
        go("logs");

        assertEquals(List.of("ok"), model.rows().stream().map(Row::id).toList());
        assertEquals(List.of("boom"), failures);
    }

    @Test
    void entryNeedingATargetIsDisabledUntilOneExists() {
        allowed.add("staff.bank");
        entries.add(entry("bank", StaffEntries.Section.ECONOMY, "staff.bank", true, () -> true));
        model.refresh();
        go("economy");
        assertEquals(State.DISABLED, row("bank").state());
        assertInstanceOf(Outcome.None.class, model.enter());

        target = true;
        model.refresh();
        assertEquals(State.ENABLED, row("bank").state());
        assertInstanceOf(Outcome.Open.class, model.enter());
    }

    @Test
    void playersAreSortedIgnoringCaseAndOpenTheirCard() {
        twoPlayers();
        actions.add(StaffPlayerActions.Action.of("heal", "k.heal", "staff.player.heal", false, c -> {}));
        allowed.add("staff.player.heal");
        model.refresh();
        assertEquals("2", row("players").detail());

        go("players");
        assertEquals(1, model.depth());
        assertEquals(List.of(ANNA_ID.toString(), ZED_ID.toString()), model.rows().stream().map(Row::id).toList());

        model.enter();
        assertEquals(2, model.depth());
        assertEquals("anna", model.currentPlayer().orElseThrow().name());
        assertEquals("anna", model.title().literal());
        assertEquals(7, model.header().size());
        assertEquals(List.of("heal"), model.rows().stream().map(Row::id).toList());
    }

    @Test
    void dangerousSimpleActionNeedsASecondPress() {
        twoPlayers();
        actions.add(StaffPlayerActions.Action.of("kick", "k.kick", "staff.player.kick", true, c -> {}));
        allowed.add("staff.player.kick");
        openAnna();

        assertEquals(new Outcome.Confirm("kick"), model.enter());
        assertEquals("kick", model.armedId());

        Outcome.Run run = assertInstanceOf(Outcome.Run.class, model.enter());
        assertEquals("kick", run.action().id());
        assertEquals("anna", run.player().name());
        assertEquals("", run.arg());
        assertNull(model.armedId());
    }

    @Test
    void movingAwayDisarmsAPendingConfirmation() {
        twoPlayers();
        actions.add(StaffPlayerActions.Action.of("kick", "k", "staff.player.kick", true, c -> {}));
        actions.add(StaffPlayerActions.Action.of("heal", "k", "staff.player.heal", false, c -> {}));
        allowed.addAll(Set.of("staff.player.kick", "staff.player.heal"));
        openAnna();

        model.enter();
        assertEquals("kick", model.armedId());
        model.move(1);
        assertNull(model.armedId());

        model.select(0);
        assertEquals(new Outcome.Confirm("kick"), model.enter(), "serve di nuovo una prima pressione");
    }

    @Test
    void plannedActionsAreShownAsSoonAndDoNothing() {
        twoPlayers();
        actions.add(StaffPlayerActions.Action.planned("ban", "k.ban", "staff.player.ban", true));
        allowed.add("staff.player.ban");
        openAnna();

        assertEquals(State.SOON, row("ban").state());
        assertInstanceOf(Outcome.None.class, model.enter());
    }

    @Test
    void actionsWithoutThePermissionAreNotListed() {
        twoPlayers();
        actions.add(StaffPlayerActions.Action.of("heal", "k", "staff.player.heal", false, c -> {}));
        openAnna();

        assertTrue(model.rows().isEmpty());
        assertInstanceOf(Outcome.None.class, model.enter());
    }

    @Test
    void choiceActionOpensAPageThenRunsWithTheChosenId() {
        twoPlayers();
        actions.add(StaffPlayerActions.Action.withChoices("mute", "k.mute", "staff.player.mute", false,
                List.of(new StaffPlayerActions.Choice("1h", "k.1h"), new StaffPlayerActions.Choice("1d", "k.1d")), c -> {}));
        allowed.add("staff.player.mute");
        openAnna();

        assertInstanceOf(Outcome.None.class, model.enter());
        assertEquals(List.of("1h", "1d"), model.rows().stream().map(Row::id).toList());
        model.select(1);
        Outcome.Run run = assertInstanceOf(Outcome.Run.class, model.enter());
        assertEquals("1d", run.arg());
        assertEquals("anna", run.player().name());
    }

    @Test
    void inputActionAsksForTheFields() {
        twoPlayers();
        actions.add(StaffPlayerActions.Action.withInput("warn", "k.warn", "staff.player.warn", false,
                List.of(new StaffPlayerActions.Field("k.reason", "k.hint", 80, true)), c -> {}));
        allowed.add("staff.player.warn");
        openAnna();

        Outcome.Input input = assertInstanceOf(Outcome.Input.class, model.enter());
        assertEquals("warn", input.action().id());
        assertEquals("anna", input.player().name());
    }

    @Test
    void backWalksUpAndStopsAtHome() {
        twoPlayers();
        openAnna();

        assertTrue(model.back());
        assertEquals(1, model.depth());
        assertEquals(ANNA_ID.toString(), model.rows().get(model.selected()).id(), "torna sulla riga da cui era entrato");
        assertTrue(model.back());
        assertEquals("players", model.rows().get(model.selected()).id());
        assertFalse(model.back());
        assertEquals(0, model.depth());
    }

    @Test
    void cardClosesWhenThePlayerGoesOffline() {
        twoPlayers();
        openAnna();
        assertEquals(2, model.depth());

        players = Optional.of(List.of(new PlayerInfo(ZED_ID, "Zed", "Zed Rossi", null, null)));
        model.refresh();

        assertEquals(1, model.depth());
        assertEquals(1, model.rows().size());
    }

    @Test
    void sectionClosesWhenItsLastEntryDisappears() {
        allowed.add("staff.bank");
        entries.add(entry("bank", StaffEntries.Section.ECONOMY, "staff.bank", false, () -> true));
        model.refresh();
        go("economy");
        assertEquals(1, model.depth());

        allowed.clear();
        model.refresh();

        assertEquals(0, model.depth());
    }

    @Test
    void infoPageShowsLinesAndIsReadOnly() {
        twoPlayers();
        openAnna();

        model.pushInfo("k.perms", List.of(new StaffPanelModel.InfoLine("k.a", "1"), new StaffPanelModel.InfoLine("k.b", "2")));

        assertEquals("k.perms", model.title().key());
        assertEquals(2, model.rows().size());
        assertTrue(model.rows().get(0).info());
        assertInstanceOf(Outcome.None.class, model.enter());
        assertTrue(model.back());
        assertEquals(2, model.depth());
    }

    @Test
    void muteLineShowsDashPermanentOrAnEndDate() {
        for (long muteUntil : new long[] {0L, System.currentTimeMillis() - 1000L}) {
            players = Optional.of(List.of(new PlayerInfo(ANNA_ID, "anna", null, null, null, muteUntil, 0, 0)));
            openAnna();
            var mute = model.header().get(4);
            assertEquals("qrm.staff.panel.mute", mute.labelKey());
            assertEquals("—", mute.value(), "nessun mute o già scaduto");
            while (model.back()) {
                // torna alla Home
            }
        }

        players = Optional.of(List.of(new PlayerInfo(ANNA_ID, "anna", null, null, null, -1, 0, 0)));
        openAnna();
        assertTrue(model.header().get(4).translateValue(), "mute permanente");
        assertEquals("—", model.header().get(1).value(), "personaggio assente = trattino");

        while (model.back()) {
            // torna alla Home
        }
        players = Optional.of(List.of(
                new PlayerInfo(ANNA_ID, "anna", null, null, null, System.currentTimeMillis() + 3_600_000L, 0, 0)));
        openAnna();
        var until = model.header().get(4);
        assertFalse(until.translateValue());
        assertTrue(until.value().matches("\\d{2}/\\d{2} \\d{2}:\\d{2}"), until.value());
    }

    @Test
    void windowAlwaysContainsTheSelection() {
        players = Optional.of(IntStream.range(0, 20)
                .mapToObj(i -> new PlayerInfo(new UUID(0, i), "p%02d".formatted(i), null, null, null)).toList());
        model.refresh();
        go("players");

        model.select(15);
        int start = model.windowStart(5);
        assertTrue(start <= 15 && 15 < start + 5);
        assertEquals(11, start);

        model.select(0);
        assertEquals(0, model.windowStart(5));

        model.scrollBy(100, 5);
        assertEquals(15, model.scroll(), "non oltre righe meno finestra");
        model.scrollBy(-100, 5);
        assertEquals(0, model.scroll());
    }

    @Test
    void moveWrapsAround() {
        model.refresh();
        int n = model.rows().size();
        model.move(-1);
        assertEquals(n - 1, model.selected());
        model.move(1);
        assertEquals(0, model.selected());
    }
}
