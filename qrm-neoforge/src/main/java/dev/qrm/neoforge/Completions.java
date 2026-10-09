package dev.qrm.neoforge;

import dev.qrm.api.economy.Currency;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.org.Organization;
import dev.qrm.api.org.OrganizationService;
import dev.qrm.api.org.Rank;
import java.util.Comparator;
import java.util.List;

/** Valori proposti dal completamento con Tab dei comandi. Pura: nessuna dipendenza da Minecraft. */
final class Completions {
    private Completions() {}

    static List<String> jobIds(JobRegistry jobs) {
        return jobs.all().stream().map(JobDefinition::id).sorted().toList();
    }

    /** I gradi del lavoro indicato, dal più basso al più alto; vuoto se il lavoro non esiste. */
    static List<String> gradeIds(JobRegistry jobs, String jobId) {
        if (jobId == null) return List.of();
        return jobs.find(jobId)
                .map(j -> j.grades().stream()
                        .sorted(Comparator.comparingInt(GradeDefinition::level))
                        .map(GradeDefinition::id)
                        .toList())
                .orElse(List.of());
    }

    static List<String> currencyCodes(CurrencyRegistry currencies) {
        return currencies.all().stream().map(Currency::code).sorted().toList();
    }

    static List<String> orgIds(OrganizationService orgs) {
        return orgs.all().stream().map(Organization::id).sorted().toList();
    }

    /** I rank dell'organizzazione indicata, dal più basso al più alto; vuoto se non esiste. */
    static List<String> rankIds(OrganizationService orgs, String orgId) {
        if (orgId == null) return List.of();
        return orgs.ranksOf(orgId).stream()
                .sorted(Comparator.comparingInt(Rank::level))
                .map(Rank::id)
                .toList();
    }
}
