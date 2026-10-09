package dev.qrm.neoforge.server;

import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.moderation.ModerationService;
import dev.qrm.api.org.Organization;
import dev.qrm.api.org.OrganizationService;
import dev.qrm.neoforge.net.PanelSnapshot;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Costruisce lo snapshot dei giocatori del pannello staff, senza dipendenze da Minecraft (testabile con servizi in memoria). */
public final class PanelSnapshots {
    /** Un giocatore online, come lo vede il server. */
    public record Online(UUID id, String name) {}

    private static final int MAX_LABEL = 64;

    private PanelSnapshots() {}

    public static PanelSnapshot build(List<Online> online, CharacterService characters, JobService jobs,
            JobRegistry registry, OrganizationService orgs, ModerationService moderation) {
        List<PanelSnapshot.PlayerEntry> players = online.stream()
                .sorted(Comparator.comparing(o -> o.name().toLowerCase(Locale.ROOT)))
                .map(o -> {
                    Optional<PlayerCharacter> pc = characters.activeOf(new PlayerId(o.id()));
                    Optional<String> job = pc.flatMap(c -> jobs.assignmentOf(c.id()))
                            .map(a -> registry.find(a.jobId()).map(JobDefinition::label).orElse(a.jobId()))
                            .map(l -> cap(l, 2 * MAX_LABEL));
                    Optional<String> org = pc.map(c -> orgs.organizationsOf(c.id()).stream().map(Organization::label)
                                    .collect(Collectors.joining(", ")))
                            .filter(l -> !l.isEmpty()).map(l -> cap(l, 4 * MAX_LABEL));
                    long mute = moderation.activeMute(o.id())
                            .map(m -> m.expiresAt().map(Instant::toEpochMilli).orElse(PanelSnapshot.Standing.PERMANENT)).orElse(0L);
                    PanelSnapshot.Standing standing = new PanelSnapshot.Standing(mute, moderation.warningCount(o.id()),
                            moderation.noteCount(o.id()));
                    return new PanelSnapshot.PlayerEntry(o.id(), cap(o.name(), MAX_LABEL),
                            pc.map(c -> cap(c.firstName() + " " + c.lastName(), 2 * MAX_LABEL)), job, org, standing);
                })
                .toList();
        return new PanelSnapshot(players);
    }

    private static String cap(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
