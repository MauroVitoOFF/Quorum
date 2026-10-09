package dev.qrm.core.job;

import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.JobRegistry;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryJobRegistry implements JobRegistry {
    private final Map<String, JobDefinition> jobs = new ConcurrentHashMap<>();

    @Override
    public void register(JobDefinition job) {
        JobDefinition existing = jobs.putIfAbsent(job.id(), job);
        if (existing != null && !existing.equals(job)) {
            throw new IllegalArgumentException("Job " + job.id() + " already registered with a different definition");
        }
    }

    /** Rimuove un lavoro (uso interno dell'adattatore, es. a fine mondo). No-op se assente. */
    public void remove(String id) {
        jobs.remove(id);
    }

    @Override
    public Optional<JobDefinition> find(String id) {
        return Optional.ofNullable(jobs.get(id));
    }

    @Override
    public Collection<JobDefinition> all() {
        return jobs.values();
    }
}
