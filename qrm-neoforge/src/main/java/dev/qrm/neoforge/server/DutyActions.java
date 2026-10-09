package dev.qrm.neoforge.server;

import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.Assignment;
import dev.qrm.api.job.DutyReason;
import dev.qrm.api.job.JobService;
import java.util.Optional;

/** Il giocatore cambia da solo il proprio servizio (comando e GUI). Pura: nessuna dipendenza da Minecraft. */
public final class DutyActions {
    public enum Result { OK, DISABLED, NO_CHARACTER, NO_JOB, ALREADY, UNKNOWN_JOB }

    /** Esito di {@link #toggle}: {@code on} è lo stato richiesto (quello opposto al precedente). */
    public record Toggle(Result result, boolean on) {}

    private DutyActions() {}

    /** Inverte il servizio del personaggio attivo (comando {@code /duty} senza argomenti). */
    public static Toggle toggle(CharacterService characters, JobService jobs, PlayerId player,
            boolean selfDutyAllowed) {
        if (!selfDutyAllowed) return new Toggle(Result.DISABLED, false);
        Optional<PlayerCharacter> active = characters.activeOf(player);
        if (active.isEmpty()) return new Toggle(Result.NO_CHARACTER, false);
        Optional<Assignment> assignment = jobs.assignmentOf(active.get().id());
        if (assignment.isEmpty()) return new Toggle(Result.NO_JOB, false);
        boolean on = !assignment.get().onDuty();
        return new Toggle(setDuty(characters, jobs, player, on, true), on);
    }

    public static Result setDuty(CharacterService characters, JobService jobs, PlayerId player, boolean on,
            boolean selfDutyAllowed) {
        if (!selfDutyAllowed) return Result.DISABLED;
        Optional<PlayerCharacter> active = characters.activeOf(player);
        if (active.isEmpty()) return Result.NO_CHARACTER;
        CharacterId id = active.get().id();
        if (jobs.setOnDuty(id, on, DutyReason.MANUAL)) return Result.OK;
        Optional<Assignment> assignment = jobs.assignmentOf(id);
        if (assignment.isEmpty()) return Result.NO_JOB;
        if (assignment.get().onDuty() == on) return Result.ALREADY;
        return Result.UNKNOWN_JOB;
    }
}
