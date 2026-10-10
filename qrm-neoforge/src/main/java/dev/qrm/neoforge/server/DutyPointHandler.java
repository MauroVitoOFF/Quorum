package dev.qrm.neoforge.server;

import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.Assignment;
import dev.qrm.api.job.DutyReason;
import dev.qrm.api.job.JobService;
import dev.qrm.api.point.PointHandler;
import dev.qrm.api.point.PointOwner;
import dev.qrm.api.point.PointType;
import dev.qrm.api.point.PointUse;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Il tipo di punto {@code duty}: un click inverte il servizio del personaggio. Funziona anche con
 * {@code selfDuty = false}: l'opzione governa solo {@code /duty} e il pulsante dell'hub.
 */
public final class DutyPointHandler implements PointHandler {
    public static final String TYPE_ID = "duty";
    public static final String LABEL_KEY = "qrm.point.duty";

    private final Supplier<JobService> jobs;
    private final BiConsumer<PlayerId, String> messenger;

    /** @param jobs risolto a ogni click: il servizio esiste solo a mondo avviato */
    public DutyPointHandler(Supplier<JobService> jobs, BiConsumer<PlayerId, String> messenger) {
        this.jobs = jobs;
        this.messenger = messenger;
    }

    public static PointType type(Supplier<JobService> jobs, BiConsumer<PlayerId, String> messenger) {
        return new PointType(TYPE_ID, LABEL_KEY, Set.of(PointOwner.Kind.JOB), false,
                new DutyPointHandler(jobs, messenger));
    }

    @Override
    public void onUse(PointUse use) {
        JobService service = jobs.get();
        Optional<Assignment> assignment = service.assignmentOf(use.character());
        if (assignment.isEmpty()) {
            messenger.accept(use.player(), "Non hai un lavoro.");
            return;
        }
        boolean on = !assignment.get().onDuty();
        if (service.setOnDuty(use.character(), on, DutyReason.MANUAL)) {
            messenger.accept(use.player(), on ? "Sei in servizio." : "Sei fuori servizio.");
        } else {
            messenger.accept(use.player(), "Il tuo lavoro non è più definito: contatta un amministratore.");
        }
    }
}
