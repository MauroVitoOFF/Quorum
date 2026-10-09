package dev.qrm.core.job;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InMemoryJobRegistryTest {
    private static JobDefinition job(String id, String gradeLabel) {
        return new JobDefinition(id, "Job " + id, List.of(new GradeDefinition("g", gradeLabel, 1, Set.of(), null)));
    }

    @Test
    void registerFindAllAndRemove() {
        InMemoryJobRegistry reg = new InMemoryJobRegistry();
        reg.register(job("police", "A"));
        reg.register(job("mechanic", "A"));
        assertEquals(2, reg.all().size());
        assertEquals("Job police", reg.find("police").orElseThrow().label());
        assertTrue(reg.find("nope").isEmpty());
        reg.remove("police");
        reg.remove("police");
        assertTrue(reg.find("police").isEmpty());
        assertEquals(1, reg.all().size());
    }

    @Test
    void sameDefinitionIsAllowedDifferentOneRejected() {
        InMemoryJobRegistry reg = new InMemoryJobRegistry();
        reg.register(job("police", "A"));
        reg.register(job("police", "A"));
        assertEquals(1, reg.all().size());
        assertThrows(IllegalArgumentException.class, () -> reg.register(job("police", "B")));
    }
}
