package dev.qrm.api.job;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.Money;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JobDefinitionTest {
    private static GradeDefinition grade(String id, int level, String... perms) {
        return new GradeDefinition(id, "Label " + id, level, Set.of(perms), null);
    }

    @Test
    void gradesAreSortedByLevelAndLookupWorks() {
        JobDefinition j = new JobDefinition("police", "Polizia",
                List.of(grade("chief", 5, "police.*"), grade("cadet", 1), grade("officer", 2)));
        assertEquals(List.of("cadet", "officer", "chief"), j.grades().stream().map(GradeDefinition::id).toList());
        assertEquals(2, j.grade("officer").orElseThrow().level());
        assertTrue(j.grade("nope").isEmpty());
    }

    @Test
    void salaryIsOptional() {
        GradeDefinition paid = new GradeDefinition("cadet", "Recluta", 1, Set.of(), Money.of("USD", 120000));
        assertEquals(Money.of("USD", 120000), paid.salary());
        assertNull(grade("officer", 2).salary());
    }

    @Test
    void permissionsAreValidatedAndCopied() {
        assertThrows(IllegalArgumentException.class, () -> grade("g", 1, "Bad Node"));
        assertThrows(IllegalArgumentException.class, () -> grade("g", 1, "a.*.b"));
        assertEquals(Set.of("police.*", "x"), grade("g", 1, "police.*", "x").permissions());
    }

    @Test
    void invalidIdsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JobDefinition("Police", "P", List.of(grade("a", 1))));
        assertThrows(IllegalArgumentException.class, () -> new JobDefinition("1police", "P", List.of(grade("a", 1))));
        assertThrows(IllegalArgumentException.class, () -> new JobDefinition("x".repeat(33), "P", List.of(grade("a", 1))));
        assertThrows(IllegalArgumentException.class, () -> grade("Bad-Id", 1));
    }

    @Test
    void labelsAndLevelsValidated() {
        assertThrows(IllegalArgumentException.class, () -> new JobDefinition("police", " ", List.of(grade("a", 1))));
        assertThrows(IllegalArgumentException.class,
                () -> new JobDefinition("police", "x".repeat(65), List.of(grade("a", 1))));
        assertThrows(IllegalArgumentException.class, () -> grade("a", -1));
        assertThrows(IllegalArgumentException.class,
                () -> new GradeDefinition("a", "", 1, Set.of(), null));
    }

    @Test
    void gradesMustBeNonEmptyWithUniqueIdsAndLevels() {
        assertThrows(IllegalArgumentException.class, () -> new JobDefinition("police", "P", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new JobDefinition("police", "P", List.of(grade("a", 1), grade("a", 2))));
        assertThrows(IllegalArgumentException.class,
                () -> new JobDefinition("police", "P", List.of(grade("a", 1), grade("b", 1))));
    }

    @Test
    void equalDefinitionsAreEqual() {
        JobDefinition a = new JobDefinition("police", "P", List.of(grade("b", 2), grade("a", 1, "x")));
        JobDefinition b = new JobDefinition("police", "P", List.of(grade("a", 1, "x"), grade("b", 2)));
        assertEquals(a, b);
    }
}
