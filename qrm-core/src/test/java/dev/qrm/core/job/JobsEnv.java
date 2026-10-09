package dev.qrm.core.job;

import dev.qrm.api.Money;
import dev.qrm.api.character.CreateCharacterResult;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.DutyChangedEvent;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobChangedEvent;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.permission.PermissionChangedEvent;
import dev.qrm.core.CoreConfig;
import dev.qrm.core.TestClock;
import dev.qrm.core.character.DefaultCharacterService;
import dev.qrm.core.db.Database;
import dev.qrm.core.db.Migrator;
import dev.qrm.core.event.SimpleEventBus;
import dev.qrm.core.player.DefaultPlayerService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class JobsEnv implements AutoCloseable {
    private static final List<String> FIRST_NAMES =
            List.of("Anna", "Bruno", "Carla", "Dario", "Elena", "Fabio", "Giulia", "Hugo", "Irene", "Luca");

    public final Database db = Database.open("jdbc:sqlite::memory:");
    public final SimpleEventBus bus = new SimpleEventBus();
    public final TestClock clock = new TestClock(Instant.parse("2026-01-01T00:00:00Z"));
    public final InMemoryJobRegistry registry = new InMemoryJobRegistry();
    public final DefaultPlayerService players;
    public final DefaultCharacterService chars;
    public final DefaultJobService jobs;
    public final List<Object> events = new ArrayList<>();
    private int counter;

    public JobsEnv() {
        Migrator.migrate(db);
        CoreConfig cfg = CoreConfig.defaults();
        players = new DefaultPlayerService(db, clock, cfg);
        chars = new DefaultCharacterService(db, bus, clock, cfg);
        jobs = new DefaultJobService(db, bus, registry, clock);
        registry.register(police());
        registry.register(mechanic());
        bus.register(JobChangedEvent.class, "t", events::add);
        bus.register(DutyChangedEvent.class, "t", events::add);
        bus.register(PermissionChangedEvent.class, "t", events::add);
    }

    public static JobDefinition police() {
        return new JobDefinition("police", "Polizia", List.of(
                new GradeDefinition("cadet", "Recluta", 1, Set.of("police.duty"), Money.of("USD", 120000)),
                new GradeDefinition("officer", "Agente", 2, Set.of("police.arrest", "police.cuff"), null),
                new GradeDefinition("chief", "Comandante", 5, Set.of("police.*"), null)));
    }

    public static JobDefinition mechanic() {
        return new JobDefinition("mechanic", "Meccanico", List.of(
                new GradeDefinition("apprentice", "Apprendista", 1, Set.of("mechanic.repair"), null),
                new GradeDefinition("boss", "Capo", 3, Set.of("mechanic.*"), null)));
    }

    /** Un personaggio ACTIVE di un nuovo giocatore. */
    public PlayerCharacter newCharacter() {
        return newCharacter(new PlayerId(UUID.randomUUID()));
    }

    /** Un personaggio ACTIVE per il giocatore indicato (lo registra se serve). Al massimo 10 per fixture. */
    public PlayerCharacter newCharacter(PlayerId player) {
        players.onJoin(player);
        String first = FIRST_NAMES.get(counter++);
        return ((CreateCharacterResult.Created) chars.create(player, first, "Rossi", LocalDate.of(1990, 1, 1)))
                .character();
    }

    @Override
    public void close() {
        db.close();
    }
}
