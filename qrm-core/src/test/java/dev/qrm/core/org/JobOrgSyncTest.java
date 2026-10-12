package dev.qrm.core.org;

import static dev.qrm.core.org.OrgsEnv.ok;
import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.org.OrgStatus;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JobOrgSyncTest {
    private static JobDefinition job(String id, String label, String orgId) {
        return new JobDefinition(id, label, List.of(new GradeDefinition("boss", "Capo", 1, Set.of("boss.*"), null)), orgId);
    }

    @Test
    void createsTheOrganizationOfEveryLinkedJobAndIsIdempotent() {
        try (OrgsEnv env = new OrgsEnv()) {
            env.registry.register(job("mechanic", "Meccanico", "mechanic"));
            env.registry.register(job("freelance", "Freelance", null));

            var first = JobOrgSync.sync(env.db, env.orgs, env.registry);
            assertTrue(first.created().containsAll(List.of("mechanic", "pd")));
            assertEquals("Meccanico", env.orgs.find("mechanic").orElseThrow().label());
            assertTrue(env.orgs.find("freelance").isEmpty());
            assertEquals(OrgStatus.ACTIVE, env.orgs.find("mechanic").orElseThrow().status());

            var second = JobOrgSync.sync(env.db, env.orgs, env.registry);
            assertTrue(second.created().isEmpty());
            assertTrue(second.relabeled().isEmpty());
        }
    }

    @Test
    void existingOrganizationIsKeptWithItsMembersAndRanks() {
        try (OrgsEnv env = new OrgsEnv()) {
            ok(env.orgs.create("mechanic", "Officina"));
            var chief = env.newCharacter();
            ok(env.orgs.defineRank("mechanic", new dev.qrm.api.org.Rank("boss", "boss", 5, Set.of("boss.*"))));
            ok(env.orgs.addMember("mechanic", chief.id(), "boss"));
            env.registry.register(job("mechanic", "Meccanico", "mechanic"));

            JobOrgSync.sync(env.db, env.orgs, env.registry);

            assertEquals(1, env.orgs.membersOf("mechanic").size());
            assertEquals(1, env.orgs.ranksOf("mechanic").size());
        }
    }

    @Test
    void renamingTheJobRenamesTheOrganization() {
        try (OrgsEnv env = new OrgsEnv()) {
            env.registry.register(job("mechanic", "Meccanico", "mechanic"));
            JobOrgSync.sync(env.db, env.orgs, env.registry);

            env.registry.remove("mechanic");
            env.registry.register(job("mechanic", "Officina Rossi", "mechanic"));
            var report = JobOrgSync.sync(env.db, env.orgs, env.registry);

            assertEquals(List.of("mechanic"), report.relabeled());
            assertEquals("Officina Rossi", env.orgs.find("mechanic").orElseThrow().label());
        }
    }

    @Test
    void jobLinkedToAnotherOrganizationDoesNotRenameIt() {
        try (OrgsEnv env = new OrgsEnv()) {
            ok(env.orgs.create("holding", "Holding"));
            env.registry.register(job("shop", "Negozio", "holding"));

            JobOrgSync.sync(env.db, env.orgs, env.registry);

            assertEquals("Holding", env.orgs.find("holding").orElseThrow().label());
        }
    }

    @Test
    void organizationOfARemovedJobIsKeptAndTheOrphanJobIsReported() {
        try (OrgsEnv env = new OrgsEnv()) {
            env.registry.register(job("mechanic", "Meccanico", "mechanic"));
            JobOrgSync.sync(env.db, env.orgs, env.registry);
            var worker = env.newCharacter();
            env.jobs.assign(worker.id(), "mechanic", "boss");

            env.registry.remove("mechanic");
            var report = JobOrgSync.sync(env.db, env.orgs, env.registry);

            assertTrue(env.orgs.find("mechanic").isPresent());
            assertEquals(List.of("mechanic"), report.orphanJobs());
        }
    }

    @Test
    void archivedOrganizationIsReportedNotReactivated() {
        try (OrgsEnv env = new OrgsEnv()) {
            env.registry.register(job("mechanic", "Meccanico", "mechanic"));
            JobOrgSync.sync(env.db, env.orgs, env.registry);
            ok(env.orgs.archive("mechanic"));

            var report = JobOrgSync.sync(env.db, env.orgs, env.registry);

            assertEquals(List.of("mechanic"), report.archived());
            assertEquals(OrgStatus.ARCHIVED, env.orgs.find("mechanic").orElseThrow().status());
        }
    }
}
