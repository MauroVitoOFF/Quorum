package dev.qrm.core.org;

import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.org.OrgError;
import dev.qrm.api.org.OrgResult;
import dev.qrm.api.org.OrgStatus;
import dev.qrm.api.org.Organization;
import dev.qrm.api.org.OrganizationService;
import dev.qrm.core.db.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Tiene allineate le organizzazioni ai lavori definiti: ogni job con {@code orgId} ha la sua organizzazione, creata
 * all'avvio se manca. Se l'organizzazione ha lo stesso id del job ne segue anche il nome. Non cancella e non archivia
 * mai nulla: un'organizzazione il cui job è sparito dai file resta com'è.
 */
public final class JobOrgSync {
    private static final System.Logger LOG = System.getLogger("qrm.orgs");

    private JobOrgSync() {}

    /** @param created id creati; @param relabeled id il cui nome è stato allineato; @param archived id archiviati con un job attivo; @param orphanJobs job assegnati ma non più definiti */
    public record Report(List<String> created, List<String> relabeled, List<String> archived, List<String> orphanJobs) {}

    public static Report sync(Database db, OrganizationService orgs, JobRegistry jobs) {
        List<String> created = new ArrayList<>();
        List<String> relabeled = new ArrayList<>();
        List<String> archived = new ArrayList<>();
        for (JobDefinition job : jobs.all().stream().sorted(java.util.Comparator.comparing(JobDefinition::id)).toList()) {
            String orgId = job.orgId();
            if (orgId == null) continue;
            Optional<Organization> existing = orgs.find(orgId);
            if (existing.isEmpty()) {
                OrgResult<Organization> r = orgs.create(orgId, job.label());
                if (r instanceof OrgResult.Ok<Organization>) {
                    created.add(orgId);
                } else if (r instanceof OrgResult.Failed<Organization> f && !(f.error() instanceof OrgError.Exists)) {
                    LOG.log(System.Logger.Level.WARNING, "Could not create organization {0} for job {1}: {2}",
                            orgId, job.id(), f.error());
                }
                continue;
            }
            Organization org = existing.get();
            if (org.status() == OrgStatus.ARCHIVED) {
                archived.add(orgId);
                LOG.log(System.Logger.Level.WARNING,
                        "Organization {0} of job {1} is archived: the job grants no organization permissions until it is unarchived",
                        orgId, job.id());
            }
            if (orgId.equals(job.id()) && !org.label().equals(job.label())) {
                if (orgs.rename(orgId, orgId, job.label()) instanceof OrgResult.Ok<Organization>) relabeled.add(orgId);
            }
        }
        List<String> orphans = orphanJobs(db, jobs);
        for (String job : orphans) {
            LOG.log(System.Logger.Level.WARNING,
                    "Job {0} is assigned to characters but is no longer defined: organization {0} (if any) is kept as it is",
                    job);
        }
        return new Report(List.copyOf(created), List.copyOf(relabeled), List.copyOf(archived), orphans);
    }

    private static List<String> orphanJobs(Database db, JobRegistry jobs) {
        return db.read(c -> {
            TreeSet<String> out = new TreeSet<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT DISTINCT job_id FROM qrm_character_job");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString(1);
                    if (jobs.find(id).isEmpty()) out.add(id);
                }
            }
            return List.copyOf(out);
        });
    }
}
