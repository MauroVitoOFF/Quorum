package dev.qrm.api.job;

import java.util.Collection;
import java.util.Optional;

public interface JobRegistry {
    /** Registra un lavoro. La stessa definizione è consentita; una diversa con lo stesso id lancia {@link IllegalArgumentException}. */
    void register(JobDefinition job);

    Optional<JobDefinition> find(String id);

    Collection<JobDefinition> all();
}
