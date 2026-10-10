package dev.qrm.neoforge.server;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.character.CreateCharacterResult;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.DutyReason;
import dev.qrm.api.point.InactiveReason;
import dev.qrm.api.point.InteractionPoint;
import dev.qrm.api.point.PointFace;
import dev.qrm.api.point.PointOwner;
import dev.qrm.api.point.PointPosition;
import dev.qrm.api.point.PointResult;
import dev.qrm.api.point.PointType;
import dev.qrm.core.CoreConfig;
import dev.qrm.core.character.DefaultCharacterService;
import dev.qrm.core.db.Database;
import dev.qrm.core.db.Migrator;
import dev.qrm.core.economy.DefaultAccountService;
import dev.qrm.core.event.SimpleEventBus;
import dev.qrm.core.job.DefaultJobService;
import dev.qrm.core.job.DefaultPermissionService;
import dev.qrm.core.job.InMemoryJobRegistry;
import dev.qrm.core.org.DefaultOrganizationService;
import dev.qrm.core.player.DefaultPlayerService;
import dev.qrm.core.point.DefaultPointService;
import dev.qrm.core.point.InMemoryPointTypeRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Logica pura dei punti lato piattaforma: click, visibilità, tipo duty e comandi. */
class PointServerTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);
    private static final List<String> NAMES = List.of("Anna", "Bruno", "Carla", "Dario", "Elena", "Fabio", "Giulia", "Hugo");
    private static final PointPosition POS = new PointPosition("minecraft:overworld", 1, 64, 1);

    private Database db;
    private DefaultCharacterService chars;
    private DefaultJobService jobs;
    private DefaultPointService points;
    private InMemoryPointTypeRegistry types;
    private DefaultPlayerService players;
    private final List<String> messages = new ArrayList<>();
    private final List<String> handled = new ArrayList<>();
    private int names;

    @BeforeEach
    void setUp() {
        db = Database.open("jdbc:sqlite::memory:");
        Migrator.migrate(db);
        SimpleEventBus bus = new SimpleEventBus();
        CoreConfig cfg = CoreConfig.defaults();
        players = new DefaultPlayerService(db, CLOCK, cfg);
        chars = new DefaultCharacterService(db, bus, CLOCK, cfg);
        InMemoryJobRegistry registry = new InMemoryJobRegistry();
        registry.register(new JobDefinition("police", "Polizia", List.of(
                new GradeDefinition("cadet", "Recluta", 1, Set.of(), null),
                new GradeDefinition("chief", "Comandante", 5, Set.of(), null))));
        jobs = new DefaultJobService(db, bus, registry, CLOCK);
        DefaultAccountService accounts = new DefaultAccountService(db, bus, CLOCK);
        DefaultOrganizationService orgs = new DefaultOrganizationService(db, bus, accounts, registry, jobs,
                new DefaultPermissionService(db, bus, registry, jobs), CLOCK);
        types = new InMemoryPointTypeRegistry();
        types.register(DutyPointHandler.type(() -> jobs, (p, text) -> messages.add(text)));
        types.register(new PointType("locker", "qrm.point.locker", Set.of(PointOwner.Kind.JOB), true,
                use -> handled.add("locker:" + use.point().id())));
        points = new DefaultPointService(db, bus, types, registry, jobs, orgs, CLOCK);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private record Actor(PlayerId player, PlayerCharacter character) {}

    private Actor actor(String job, String grade) {
        PlayerId pid = new PlayerId(UUID.randomUUID());
        players.onJoin(pid);
        PlayerCharacter c = ((CreateCharacterResult.Created) chars.create(pid, NAMES.get(names++), "Rossi",
                LocalDate.of(1990, 1, 1))).character();
        if (job != null) jobs.assign(c.id(), job, grade);
        return new Actor(pid, c);
    }

    private InteractionPoint point(String type, String minGrade, boolean isPublic) {
        InteractionPoint p = ((PointResult.Ok<InteractionPoint>) points.create(type, PointOwner.job("police"), POS,
                minGrade, null)).value();
        if (isPublic) points.setPublicVisible(p.id(), true);
        return points.find(p.id()).orElseThrow();
    }

    private PointInteraction.Outcome click(Actor a, boolean worldActive) {
        return PointInteraction.use(points, types, chars, a.player(), POS, p -> worldActive);
    }

    // ---- click ---------------------------------------------------------------------------------------------

    @Test
    void clickOnPlainBlockDoesNothing() {
        assertEquals(PointInteraction.Outcome.NOT_A_POINT, click(actor("police", "cadet"), true));
        assertTrue(handled.isEmpty());
    }

    @Test
    void authorizedClickRunsTheHandlerOnce() {
        point("locker", null, false);
        Actor cop = actor("police", "cadet");
        jobs.setOnDuty(cop.character().id(), true, DutyReason.MANUAL);
        assertEquals(PointInteraction.Outcome.USED, click(cop, true));
        assertEquals(1, handled.size());
    }

    @Test
    void unauthorizedClickIsDeniedAndHandlerNotCalled() {
        point("locker", null, false);
        Actor civilian = actor(null, null);
        Actor offDuty = actor("police", "cadet");
        assertEquals(PointInteraction.Outcome.DENIED, click(civilian, true));
        assertEquals(PointInteraction.Outcome.DENIED, click(offDuty, true)); // locker richiede il servizio
        assertTrue(handled.isEmpty());
    }

    @Test
    void playerWithoutActiveCharacterIsToldSo() {
        point("duty", null, false);
        PlayerId nobody = new PlayerId(UUID.randomUUID());
        players.onJoin(nobody);
        assertEquals(PointInteraction.Outcome.NO_CHARACTER,
                PointInteraction.use(points, types, chars, nobody, POS, p -> true));
    }

    @Test
    void inactivePointBehavesLikeAPlainBlock() {
        point("duty", null, false);
        assertEquals(PointInteraction.Outcome.NOT_A_POINT, click(actor("police", "cadet"), false));
    }

    // ---- tipo duty -----------------------------------------------------------------------------------------

    @Test
    void dutyPointTogglesServiceBothWays() {
        point("duty", null, false);
        Actor cop = actor("police", "cadet");
        assertEquals(PointInteraction.Outcome.USED, click(cop, true));
        assertTrue(jobs.assignmentOf(cop.character().id()).orElseThrow().onDuty());
        assertEquals("Sei in servizio.", messages.get(0));
        click(cop, true);
        assertFalse(jobs.assignmentOf(cop.character().id()).orElseThrow().onDuty());
        assertEquals("Sei fuori servizio.", messages.get(1));
    }

    @Test
    void dutyPointRespectsMinimumGrade() {
        point("duty", "chief", false);
        Actor cadet = actor("police", "cadet");
        assertEquals(PointInteraction.Outcome.DENIED, click(cadet, true));
        assertFalse(jobs.assignmentOf(cadet.character().id()).orElseThrow().onDuty());
    }

    // ---- visibilità ----------------------------------------------------------------------------------------

    @Test
    void privatePointsAreSentOnlyToThoseWhoCanUseThem() {
        InteractionPoint p = point("duty", null, false);
        Actor cop = actor("police", "cadet");
        Actor civilian = actor(null, null);
        assertEquals(List.of(p.id()), ids(PointViews.visibleTo(points, types, Optional.of(cop.character().id()), q -> true)));
        assertTrue(PointViews.visibleTo(points, types, Optional.of(civilian.character().id()), q -> true).isEmpty());
        assertTrue(PointViews.visibleTo(points, types, Optional.empty(), q -> true).isEmpty());
    }

    @Test
    void publicPointsAreSentToEveryoneAndCarryTheLabel() {
        InteractionPoint p = point("duty", null, true);
        points.setName(p.id(), "Centrale");
        Actor civilian = actor(null, null);
        var list = PointViews.visibleTo(points, types, Optional.of(civilian.character().id()), q -> true);
        assertEquals(1, list.size());
        assertEquals("Centrale", list.get(0).name().orElseThrow());
        assertEquals("qrm.point.duty", list.get(0).labelKey());
        assertEquals(1, PointViews.visibleTo(points, types, Optional.empty(), q -> true).size());
    }

    @Test
    void inactivePointsAreNeverSent() {
        point("duty", null, true);
        assertTrue(PointViews.visibleTo(points, types, Optional.empty(), q -> false).isEmpty());
    }

    private static List<Integer> ids(List<dev.qrm.neoforge.net.PointEntry> entries) {
        return entries.stream().map(dev.qrm.neoforge.net.PointEntry::id).toList();
    }

    // ---- comandi -------------------------------------------------------------------------------------------

    private PointCommandLogic logic() {
        return new PointCommandLogic(points, p -> points.inactiveReason(p));
    }

    @Test
    void createListInfoAndRemoveFlow() {
        PointCommandLogic logic = logic();
        assertTrue(logic.create("duty", "job", "police", "cadet", POS, null).ok());
        assertFalse(logic.create("duty", "job", "police", null, POS, null).ok()); // blocco occupato
        assertEquals(1, points.all().size());
        assertTrue(logic.list(null, null).get(0).contains("attivo"));
        assertEquals(1, logic.list("job", "police").size());
        assertEquals("Nessun punto.", logic.list("job", "baker").get(0));
        assertTrue(logic.info(POS).get(0).startsWith("Punto #1"));
        assertTrue(logic.remove(null, POS).ok());
        assertFalse(logic.remove(null, POS).ok());
        assertTrue(points.all().isEmpty());
    }

    @Test
    void createRejectsBadArguments() {
        PointCommandLogic logic = logic();
        assertFalse(logic.create("duty", "gang", "police", null, POS, null).ok());
        assertFalse(logic.create("duty", "job", "Bad Id", null, POS, null).ok());
        assertFalse(logic.create("nope", "job", "police", null, POS, null).ok());
        assertFalse(logic.create("duty", "org", "gang", null, POS, null).ok());
        assertFalse(logic.create("duty", "job", "police", "general", POS, null).ok());
        assertTrue(points.all().isEmpty());
    }

    @Test
    void setCommandsAndMove() {
        PointCommandLogic logic = logic();
        logic.create("duty", "job", "police", null, POS, null);
        assertTrue(logic.setGrade(POS, "chief").ok());
        assertEquals("chief", points.find(1).orElseThrow().minGradeId());
        assertTrue(logic.setGrade(POS, "none").ok());
        assertNull(points.find(1).orElseThrow().minGradeId());
        assertTrue(logic.setPublic(POS, true).ok());
        assertTrue(points.find(1).orElseThrow().publicVisible());
        assertTrue(logic.setName(POS, "Centrale").ok());
        assertFalse(logic.setName(POS, "x".repeat(65)).ok());
        assertTrue(logic.setName(POS, "none").ok());
        assertNull(points.find(1).orElseThrow().name());
        PointPosition other = new PointPosition("minecraft:overworld", 9, 64, 9);
        assertTrue(logic.move(1, other).ok());
        assertFalse(logic.setGrade(POS, "chief").ok()); // il vecchio blocco non è più un punto
        assertFalse(logic.move(7, POS).ok());
    }

    @Test
    void createUsesTheLookedFaceAsDefault() {
        PointCommandLogic logic = logic();
        PointCommandLogic.Reply reply = logic.create("duty", "job", "police", null, POS, PointFace.WEST);
        assertTrue(reply.ok());
        assertTrue(reply.text().contains("ovest"));
        assertEquals(PointFace.WEST, points.find(1).orElseThrow().face());
        // guardando sopra o sotto non c'è una faccia laterale: tutte e quattro
        PointPosition other = new PointPosition("minecraft:overworld", 5, 64, 5);
        assertTrue(logic.create("duty", "job", "police", null, other, null).ok());
        assertNull(points.find(2).orElseThrow().face());
    }

    @Test
    void setFaceCommandSupportsAllHereAndExplicitSides() {
        PointCommandLogic logic = logic();
        logic.create("duty", "job", "police", null, POS, null);
        assertTrue(logic.setFace(POS, "north", null).ok());
        assertEquals(PointFace.NORTH, points.find(1).orElseThrow().face());
        assertTrue(logic.setFace(POS, "here", PointFace.EAST).ok());
        assertEquals(PointFace.EAST, points.find(1).orElseThrow().face());
        assertTrue(logic.info(POS).stream().anyMatch(l -> l.contains("solo sulla faccia est")));
        assertTrue(logic.setFace(POS, "all", null).ok());
        assertNull(points.find(1).orElseThrow().face());
        assertTrue(logic.info(POS).stream().anyMatch(l -> l.contains("su tutte le facce")));
    }

    @Test
    void setFaceCommandRejectsBadInput() {
        PointCommandLogic logic = logic();
        logic.create("duty", "job", "police", null, POS, null);
        assertFalse(logic.setFace(POS, "here", null).ok()); // si guarda sopra o sotto
        assertFalse(logic.setFace(POS, "up", null).ok());
        assertFalse(logic.setFace(POS, null, null).ok());
        assertFalse(logic.setFace(new PointPosition("minecraft:overworld", 9, 9, 9), "all", null).ok());
        assertNull(points.find(1).orElseThrow().face());
    }

    @Test
    void faceTravelsToTheClientEntry() {
        InteractionPoint p = point("duty", null, true);
        points.setFace(p.id(), PointFace.SOUTH);
        var entry = PointViews.visibleTo(points, types, Optional.empty(), q -> true).get(0);
        assertEquals(Optional.of("south"), entry.face());
    }

    @Test
    void listFlagsInactivePointsWithTheReason() {
        PointCommandLogic logic = new PointCommandLogic(points, p -> Optional.of(InactiveReason.MISSING_BLOCK));
        logic.create("duty", "job", "police", null, POS, null);
        assertTrue(logic.list(null, null).get(0).contains("inattivo (blocco mancante)"));
    }
}
