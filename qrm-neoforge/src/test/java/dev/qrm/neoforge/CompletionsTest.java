package dev.qrm.neoforge;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.economy.Currency;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.core.economy.InMemoryCurrencyRegistry;
import dev.qrm.core.job.InMemoryJobRegistry;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CompletionsTest {
    private static JobDefinition job(String id, String... grades) {
        List<GradeDefinition> list = new java.util.ArrayList<>();
        for (int i = 0; i < grades.length; i++) {
            list.add(new GradeDefinition(grades[i], grades[i], i + 1, Set.of(), null));
        }
        return new JobDefinition(id, id, list);
    }

    @Test
    void jobIdsAreSorted() {
        InMemoryJobRegistry registry = new InMemoryJobRegistry();
        registry.register(job("police", "cadet"));
        registry.register(job("ems", "medic"));
        assertEquals(List.of("ems", "police"), Completions.jobIds(registry));
    }

    @Test
    void gradeIdsFollowTheLevelOfTheChosenJob() {
        InMemoryJobRegistry registry = new InMemoryJobRegistry();
        registry.register(job("police", "cadet", "officer", "chief"));
        assertEquals(List.of("cadet", "officer", "chief"), Completions.gradeIds(registry, "police"));
    }

    @Test
    void gradeIdsOfAnUnknownJobAreEmpty() {
        assertEquals(List.of(), Completions.gradeIds(new InMemoryJobRegistry(), "nope"));
        assertEquals(List.of(), Completions.gradeIds(new InMemoryJobRegistry(), null));
    }

    @Test
    void currencyCodesAreSorted() {
        InMemoryCurrencyRegistry registry = new InMemoryCurrencyRegistry();
        registry.register(new Currency("USD", "$", 2));
        registry.register(new Currency("EUR", "E", 2));
        assertEquals(List.of("EUR", "USD"), Completions.currencyCodes(registry));
    }
}
