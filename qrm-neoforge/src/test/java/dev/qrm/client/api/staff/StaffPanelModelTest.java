package dev.qrm.client.api.staff;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.client.api.staff.StaffPanelModel.Outcome;
import dev.qrm.client.api.staff.StaffPanelModel.PlayerInfo;
import dev.qrm.client.api.staff.StaffPanelModel.State;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class StaffPanelModelTest {
    private static final class FakeSource implements StaffPanelModel.Source {
        final List<StaffEntries.Entry> entries = new ArrayList<>();
        final List<StaffPlayerActions.Action> actions = new ArrayList<>();
        Optional<List<PlayerInfo>> players = Optional.empty();
        Set<String> allowed;            // null = tutto consentito
        boolean target;

        @Override public List<StaffEntries.Entry> entries() { return entries; }
        @Override public List<StaffPlayerActions.Action> playerActions() { return actions; }
        @Override public Optional<List<PlayerInfo>> players() { return players; }
        @Override public boolean allows(String node) { return allowed == null || allowed.contains(node); }
        @Override public boolean hasTarget() { return target; }
    }

    private final FakeSource src = new FakeSource();
    private final List<String> failures = new ArrayList<>();
    private final StaffPanelModel model = new StaffPanelModel(src, failures::add);
    private final UUID marcoId = UUID.randomUUID();
    private final UUID annaId = UUID.randomUUID();

    private static StaffEntries.Entry entry(String id, StaffEntries.Section s, String node, boolean needsTarget,
            BooleanSupplier available) {
        return new StaffEntries.Entry(id, s, "k." + id, node, needsTarget, available, ctx -> {});
    }

    private void withPlayers() {
        src.players = Optional.of(List.of(new PlayerInfo(marcoId, "Marco", "Marco Rossi", "Mechanic", "Benny's"),
                new PlayerInfo(annaId, "anna", "", "", "")));
    }

    private List<String> ids() {
        return model.rows().stream().map(StaffPanelModel.Row::id).toList();
    }

    private void openCardOfMarco() {
        withPlayers();
        model.refresh();
        model.select(0);
        model.enter();                                   // Players
        model.refresh();
        model.select(ids().indexOf(marcoId.toString()));
        model.enter();                                   // scheda
    }

    @Test
    void homeListsPlayersWithCountThenAllSectionsInOrder() {
        withPlayers();
        model.refresh();
        assertEquals(List.of("players", "player_management", "server_management", "moderation", "economy",
                "organizations", "logs", "quick_actions"), ids());
        assertEquals("2", model.rows().get(0).detail());
        assertEquals(0, model.depth());
    }

    @Test
    void playersCountIsAnEllipsisUntilTheSnapshotArrives() {
        model.refresh();
        assertEquals("…", model.rows().get(0).detail());
    }

    @Test
    void sectionsWithoutEntriesAreSoonAndNotEnterable() {
        src.entries.add(entry("bank", StaffEntries.Section.ECONOMY, "staff.bank", false, () -> true));
        model.refresh();
        assertEquals(State.ENABLED, model.rows().get(ids().indexOf("economy")).state());
        assertEquals(State.SOON, model.rows().get(ids().indexOf("moderation")).state());
        model.select(ids().indexOf("moderation"));
        assertInstanceOf(Outcome.None.class, model.enter());
        assertEquals(0, model.depth());
    }

    @Test
    void sectionShowsAllowedEntriesAndDisablesTargetOnesWithoutTarget() {
        src.entries.add(entry("jobs", StaffEntries.Section.PLAYER_MANAGEMENT, "staff.jobs", true, () -> true));
        src.entries.add(entry("orgs", StaffEntries.Section.PLAYER_MANAGEMENT, "staff.orgs", false, () -> true));
        src.entries.add(entry("hidden", StaffEntries.Section.PLAYER_MANAGEMENT, "staff.hidden", false, () -> true));
        src.allowed = Set.of("staff.jobs", "staff.orgs");
        model.refresh();
        model.select(ids().indexOf("player_management"));
        model.enter();
        assertEquals(List.of("jobs", "orgs"), ids());
        assertEquals(State.DISABLED, model.rows().get(0).state());
        assertEquals(State.ENABLED, model.rows().get(1).state());
        assertInstanceOf(Outcome.None.class, model.enter());          // jobs disattivata
        src.target = true;
        model.refresh();
        Outcome o = model.enter();
        assertEquals("jobs", assertInstanceOf(Outcome.Open.class, o).entry().id());
    }

    @Test
    void throwingAvailabilityIsSkippedAndReported() {
        src.entries.add(entry("boom", StaffEntries.Section.QUICK_ACTIONS, "staff.module.boom", false, () -> {
            throw new IllegalStateException("x");
        }));
        model.refresh();
        assertEquals(State.SOON, model.rows().get(ids().indexOf("quick_actions")).state());
        assertEquals(List.of("boom"), failures);
    }

    @Test
    void playersPageListsPlayersSortedWithTheirCharacter() {
        withPlayers();
        model.refresh();
        model.select(0);
        model.enter();
        assertEquals(List.of(annaId.toString(), marcoId.toString()), ids());   // "anna" < "marco"
        assertEquals("anna", model.rows().get(0).label());
        assertEquals("Marco Rossi", model.rows().get(1).detail());
    }

    @Test
    void playerCardShowsHeaderAndAllowedActionsWithSoonOnesDisabled() {
        src.actions.add(StaffPlayerActions.Action.planned("heal", "k.heal", "staff.player.heal", false));
        src.actions.add(StaffPlayerActions.Action.of("view_permissions", "k.vp", "staff.player.view_permissions", false, c -> {}));
        src.actions.add(StaffPlayerActions.Action.planned("ban", "k.ban", "staff.player.ban", true));
        src.allowed = Set.of("staff.player.heal", "staff.player.view_permissions");
        openCardOfMarco();
        assertEquals("Marco", model.title().literal());
        assertEquals(List.of("qrm.staff.panel.online", "qrm.staff.panel.character", "qrm.staff.panel.job",
                "qrm.staff.panel.organization", "qrm.staff.panel.mute", "qrm.staff.panel.warnings", "qrm.staff.panel.notes"),
                model.header().stream().map(StaffPanelModel.HeaderLine::labelKey).toList());
        assertEquals("Mechanic", model.header().get(2).value());
        assertEquals("Benny's", model.header().get(3).value());
        assertEquals(List.of("heal", "view_permissions"), ids());           // ban non consentita: non compare
        assertEquals(State.SOON, model.rows().get(0).state());
        assertEquals(State.ENABLED, model.rows().get(1).state());
        assertEquals(marcoId, model.currentPlayer().orElseThrow().id());
    }

    @Test
    void cardUsesDashesForMissingCharacterJobAndOrganization() {
        withPlayers();
        model.refresh();
        model.select(0);
        model.enter();
        model.select(0);                                                     // anna
        model.enter();
        assertEquals("—", model.header().get(1).value());
        assertEquals("—", model.header().get(2).value());
        assertEquals("—", model.header().get(3).value());
    }

    @Test
    void plannedActionDoesNothing() {
        src.actions.add(StaffPlayerActions.Action.planned("heal", "k.heal", "staff.player.heal", false));
        openCardOfMarco();
        assertInstanceOf(Outcome.None.class, model.enter());
    }

    @Test
    void dangerousActionNeedsASecondPress() {
        src.actions.add(StaffPlayerActions.Action.of("kick", "k.kick", "staff.player.kick", true, c -> {}));
        openCardOfMarco();
        assertEquals("kick", assertInstanceOf(Outcome.Confirm.class, model.enter()).actionId());
        assertEquals("kick", model.armedId());
        Outcome second = model.enter();
        assertEquals("kick", assertInstanceOf(Outcome.Run.class, second).action().id());
        assertNull(model.armedId());
    }

    @Test
    void movingTheSelectionDisarmsTheConfirmation() {
        src.actions.add(StaffPlayerActions.Action.of("kick", "k.kick", "staff.player.kick", true, c -> {}));
        src.actions.add(StaffPlayerActions.Action.of("view", "k.view", "staff.player.view", false, c -> {}));
        openCardOfMarco();
        model.enter();
        model.move(1);
        model.move(-1);
        assertInstanceOf(Outcome.Confirm.class, model.enter());              // di nuovo la prima pressione
    }

    @Test
    void normalActionRunsImmediatelyWithThePlayer() {
        src.actions.add(StaffPlayerActions.Action.of("view", "k.view", "staff.player.view", false, c -> {}));
        openCardOfMarco();
        Outcome.Run run = assertInstanceOf(Outcome.Run.class, model.enter());
        assertEquals(marcoId, run.player().id());
    }

    @Test
    void backReturnsOneLevelKeepingTheRowWeCameFrom() {
        openCardOfMarco();
        assertEquals(2, model.depth());
        assertTrue(model.back());
        assertEquals(ids().indexOf(marcoId.toString()), model.selected());
        assertTrue(model.back());
        assertEquals("players", ids().get(model.selected()));
        assertFalse(model.back());
        assertEquals(0, model.depth());
    }

    @Test
    void cardClosesWhenThePlayerLeaves() {
        openCardOfMarco();
        src.players = Optional.of(List.of(new PlayerInfo(annaId, "anna", "", "", "")));
        model.refresh();
        assertEquals(1, model.depth());
        assertEquals(List.of(annaId.toString()), ids());
        assertTrue(model.currentPlayer().isEmpty());
    }

    @Test
    void openSectionClosesWhenItsEntriesDisappear() {
        src.entries.add(entry("bank", StaffEntries.Section.ECONOMY, "staff.bank", false, () -> true));
        model.refresh();
        model.select(ids().indexOf("economy"));
        model.enter();
        assertEquals(1, model.depth());
        src.entries.clear();
        model.refresh();
        assertEquals(0, model.depth());
    }

    @Test
    void moveWrapsAround() {
        model.refresh();
        model.move(-1);
        assertEquals(7, model.selected());
        model.move(1);
        assertEquals(0, model.selected());
    }

    @Test
    void windowFollowsSelectionAndScrollsByWheel() {
        model.refresh();                                  // 8 righe
        model.select(6);
        assertEquals(3, model.windowStart(4));
        model.scrollBy(100, 4);
        assertEquals(4, model.scroll());
        model.scrollBy(-100, 4);
        assertEquals(0, model.scroll());
    }

    @Test
    void choiceActionOpensAPageAndReturnsTheChoiceAsArgument() {
        src.actions.add(StaffPlayerActions.Action.withChoices("set_gamemode", "k.gm", "staff.player.set_gamemode", false,
                List.of(new StaffPlayerActions.Choice("survival", "k.s"), new StaffPlayerActions.Choice("creative", "k.c")),
                c -> {}));
        openCardOfMarco();
        assertInstanceOf(Outcome.None.class, model.enter());                 // apre la pagina delle scelte
        assertEquals(3, model.depth());
        assertEquals(List.of("survival", "creative"), ids());
        assertEquals("k.gm", model.title().key());
        assertEquals(marcoId, model.currentPlayer().orElseThrow().id());     // il bersaglio resta quello della scheda
        model.select(1);
        Outcome.Run run = assertInstanceOf(Outcome.Run.class, model.enter());
        assertEquals("creative", run.arg());
        assertEquals(marcoId, run.player().id());
        assertTrue(model.back());
        assertEquals(2, model.depth());
    }

    @Test
    void choicesPageClosesIfThePlayerLeaves() {
        src.actions.add(StaffPlayerActions.Action.withChoices("set_gamemode", "k.gm", "staff.player.set_gamemode", false,
                List.of(new StaffPlayerActions.Choice("survival", "k.s")), c -> {}));
        openCardOfMarco();
        model.enter();
        src.players = Optional.of(List.of(new PlayerInfo(annaId, "anna", "", "", "")));
        model.refresh();
        assertEquals(1, model.depth());
    }

    @Test
    void inputActionAsksForTheInputBoxWithoutConfirmation() {
        src.actions.add(StaffPlayerActions.Action.withInput("kick", "k.kick", "staff.player.kick", true,
                List.of(new StaffPlayerActions.Field("k.reason", "k.hint", 64, false)), c -> {}));
        openCardOfMarco();
        Outcome o = model.enter();
        Outcome.Input in = assertInstanceOf(Outcome.Input.class, o);
        assertEquals("kick", in.action().id());
        assertEquals(marcoId, in.player().id());
        assertNull(model.armedId());
        assertEquals(2, model.depth());                                       // resta sulla scheda
    }

    @Test
    void infoPageShowsLinesAndBackClosesIt() {
        openCardOfMarco();
        model.pushInfo("k.info", List.of(new StaffPanelModel.InfoLine("k.a", "1"), new StaffPanelModel.InfoLine("k.b", "2")));
        assertEquals(3, model.depth());
        assertEquals("k.info", model.title().key());
        assertEquals(2, model.rows().size());
        assertEquals("1", model.rows().get(0).detail());
        assertEquals("k.a", model.rows().get(0).labelKey());
        assertInstanceOf(Outcome.None.class, model.enter());                  // le righe informative non fanno nulla
        assertTrue(model.back());
        assertEquals(2, model.depth());
    }

    @Test
    void headerShowsMuteWarningsAndNotes() {
        long future = System.currentTimeMillis() + 3_600_000;
        src.players = Optional.of(List.of(
                new PlayerInfo(marcoId, "Marco", "", "", "", -1, 2, 5),
                new PlayerInfo(annaId, "anna", "", "", "", future, 0, 0),
                new PlayerInfo(UUID.randomUUID(), "zed", "", "", "", 0, 0, 0),
                new PlayerInfo(UUID.randomUUID(), "old", "", "", "", System.currentTimeMillis() - 1000, 0, 0)));
        model.refresh();
        model.select(0);
        model.enter();
        model.refresh();
        for (int i = 0; i < 4; i++) {                                         // le righe sono ordinate per nome: anna, marco, old, zed
            model.select(i);
            model.enter();
            StaffPanelModel.HeaderLine mute = model.header().get(4);
            switch (model.title().literal()) {
                case "Marco" -> {
                    assertEquals("qrm.staff.panel.permanent", mute.value());
                    assertTrue(mute.translateValue());
                    assertEquals("2", model.header().get(5).value());
                    assertEquals("5", model.header().get(6).value());
                }
                case "anna" -> {
                    assertFalse(mute.translateValue());
                    assertTrue(mute.value().matches("\\d\\d/\\d\\d \\d\\d:\\d\\d"), mute.value());
                }
                default -> assertEquals("—", mute.value());                  // nessun mute oppure scaduto
            }
            model.back();
        }
    }
}
