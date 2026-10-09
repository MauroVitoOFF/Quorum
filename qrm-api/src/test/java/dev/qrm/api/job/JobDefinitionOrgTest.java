package dev.qrm.api.job;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.economy.OwnerRef;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JobDefinitionOrgTest {
    private static List<GradeDefinition> grades() {
        return List.of(new GradeDefinition("cadet", "Recluta", 1, Set.of("police.duty"), null));
    }

    @Test
    void threeArgumentConstructorLeavesOrgEmpty() {
        assertNull(new JobDefinition("police", "Polizia", grades()).orgId());
    }

    @Test
    void orgIdIsKept() {
        assertEquals("pd", new JobDefinition("police", "Polizia", grades(), "pd").orgId());
    }

    @Test
    void malformedOrgIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JobDefinition("police", "Polizia", grades(), "Bad Id"));
        assertThrows(IllegalArgumentException.class, () -> new JobDefinition("police", "Polizia", grades(), ""));
    }

    @Test
    void orgIdParticipatesInEquality() {
        assertNotEquals(new JobDefinition("police", "Polizia", grades(), "pd"),
                new JobDefinition("police", "Polizia", grades()));
        assertEquals(new JobDefinition("police", "Polizia", grades(), "pd"),
                new JobDefinition("police", "Polizia", grades(), "pd"));
    }

    @Test
    void organizationOwnerType() {
        assertEquals("ORGANIZATION", OwnerRef.ORGANIZATION);
        assertEquals("ORGANIZATION", new OwnerRef(OwnerRef.ORGANIZATION, java.util.UUID.randomUUID()).type());
    }
}
