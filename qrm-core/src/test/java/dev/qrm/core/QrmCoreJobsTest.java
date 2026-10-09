package dev.qrm.core;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.CreateCharacterResult;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.event.EventBus;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.AssignResult;
import dev.qrm.api.job.DutyChangedEvent;
import dev.qrm.api.job.DutyReason;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.JobService;
import dev.qrm.api.permission.PermissionService;
import dev.qrm.api.player.PlayerService;
import dev.qrm.core.economy.InMemoryCurrencyRegistry;
import dev.qrm.core.event.SimpleEventBus;
import dev.qrm.core.job.InMemoryJobRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QrmCoreJobsTest {
    private QrmCore core;
    private SimpleEventBus bus;
    private InMemoryJobRegistry jobs;

    @AfterEach
    void tearDown() {
        if (core != null) core.close();
        QRM.clear();
    }

    private void start() {
        start("jdbc:sqlite::memory:");
    }

    private void start(String url) {
        bus = new SimpleEventBus();
        InMemoryCurrencyRegistry currencies = new InMemoryCurrencyRegistry();
        jobs = new InMemoryJobRegistry();
        jobs.register(new JobDefinition("police", "Polizia", List.of(
                new GradeDefinition("cadet", "Recluta", 1, Set.of("police.duty"), null))));
        QRM.register(EventBus.class, bus);
        QRM.register(CurrencyRegistry.class, currencies);
        core = QrmCore.start(bus, currencies, jobs, url, null, null, CoreConfig.defaults(),
                Clock.systemUTC());
    }

    private PlayerCharacter character(PlayerId pid, String first) {
        return ((CreateCharacterResult.Created) QRM.get(CharacterService.class)
                .create(pid, first, "Rossi", LocalDate.of(1990, 1, 1))).character();
    }

    @Test
    void jobsAndPermissionsEndToEndWithCharacterSwitch() {
        start();
        List<DutyChangedEvent> duty = new ArrayList<>();
        bus.register(DutyChangedEvent.class, "t", duty::add);
        PlayerId pid = new PlayerId(UUID.randomUUID());
        QRM.get(PlayerService.class).onJoin(pid);
        PlayerCharacter c1 = character(pid, "Anna");
        PlayerCharacter c2 = character(pid, "Bruno");

        JobService js = QRM.get(JobService.class);
        PermissionService ps = QRM.get(PermissionService.class);
        assertInstanceOf(AssignResult.Assigned.class, js.assign(c1.id(), "police", "cadet"));
        assertFalse(ps.has(c1.id(), "police.duty"));
        assertTrue(js.setOnDuty(c1.id(), true, DutyReason.MANUAL));
        assertTrue(ps.has(c1.id(), "police.duty"));

        QRM.get(CharacterService.class).select(pid, c2.id());

        assertFalse(js.assignmentOf(c1.id()).orElseThrow().onDuty());
        assertFalse(ps.has(c1.id(), "police.duty"));
        assertEquals(List.of(new DutyChangedEvent(c1.id(), true, DutyReason.MANUAL),
                new DutyChangedEvent(c1.id(), false, DutyReason.SWITCH)), duty);
    }

    @Test
    void closeUnregistersJobServicesAndTheSwitchHandler() {
        start();
        core.close();
        core = null;
        assertThrows(IllegalStateException.class, () -> QRM.get(JobService.class));
        assertThrows(IllegalStateException.class, () -> QRM.get(PermissionService.class));

        // Un secondo core sullo stesso bus non deve lasciare handler del primo: una sola notifica per cambio personaggio.
        InMemoryCurrencyRegistry currencies = new InMemoryCurrencyRegistry();
        core = QrmCore.start(bus, currencies, new InMemoryJobRegistry(), "jdbc:sqlite::memory:", null, null,
                CoreConfig.defaults(), Clock.systemUTC());
        jobs = new InMemoryJobRegistry();
        List<DutyChangedEvent> duty = new ArrayList<>();
        bus.register(DutyChangedEvent.class, "t", duty::add);
        PlayerId pid = new PlayerId(UUID.randomUUID());
        QRM.get(PlayerService.class).onJoin(pid);
        PlayerCharacter c1 = character(pid, "Anna");
        PlayerCharacter c2 = character(pid, "Bruno");
        QRM.get(CharacterService.class).select(pid, c2.id());
        assertEquals(List.of(), duty);   // nessun personaggio in servizio: nessun evento, nessun errore dal vecchio handler
        assertNotNull(c1);
    }

    @Test
    void dutyIsResetOnStartAfterACrash() throws Exception {
        Path file = Files.createTempFile("qrm-test", ".db");
        try {
            String url = "jdbc:sqlite:" + file;
            start(url);
            PlayerId pid = new PlayerId(UUID.randomUUID());
            QRM.get(PlayerService.class).onJoin(pid);
            PlayerCharacter c = character(pid, "Anna");
            JobService js = QRM.get(JobService.class);
            assertInstanceOf(AssignResult.Assigned.class, js.assign(c.id(), "police", "cadet"));
            assertTrue(js.setOnDuty(c.id(), true, DutyReason.MANUAL));
            core.close();   // close non azzera il servizio: simula un crash
            QRM.clear();

            start(url);
            assertFalse(QRM.get(JobService.class).assignmentOf(c.id()).orElseThrow().onDuty());
        } finally {
            if (core != null) core.close();
            core = null;
            QRM.clear();
            Files.deleteIfExists(file);
        }
    }
}
