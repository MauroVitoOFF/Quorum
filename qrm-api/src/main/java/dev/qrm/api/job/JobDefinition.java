package dev.qrm.api.job;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Un lavoro generico con i suoi gradi, ordinati per {@code level} crescente.
 * {@code orgId} è il legame opzionale con un'organizzazione (null se assente); l'esistenza dell'organizzazione
 * non è verificata qui. I job caricati dai file JSON hanno sempre l'organizzazione con il proprio id, che il Core crea
 * all'avvio se manca.
 */
public record JobDefinition(String id, String label, List<GradeDefinition> grades, String orgId) {
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]{0,31}");

    public JobDefinition(String id, String label, List<GradeDefinition> grades) {
        this(id, label, grades, null);
    }

    public JobDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(grades, "grades");
        if (!isValidId(id)) throw new IllegalArgumentException("invalid job id: " + id);
        if (orgId != null && !isValidId(orgId)) throw new IllegalArgumentException("invalid organization id: " + orgId);
        if (label.isBlank() || label.length() > 64) throw new IllegalArgumentException("label must be 1..64 chars");
        if (grades.isEmpty()) throw new IllegalArgumentException("a job needs at least one grade");
        Set<String> ids = new HashSet<>();
        Set<Integer> levels = new HashSet<>();
        for (GradeDefinition g : grades) {
            if (!ids.add(g.id())) throw new IllegalArgumentException("duplicate grade id: " + g.id());
            if (!levels.add(g.level())) throw new IllegalArgumentException("duplicate grade level: " + g.level());
        }
        grades = grades.stream().sorted(Comparator.comparingInt(GradeDefinition::level)).toList();
    }

    public static boolean isValidId(String s) {
        return s != null && ID.matcher(s).matches();
    }

    public Optional<GradeDefinition> grade(String gradeId) {
        return grades.stream().filter(g -> g.id().equals(gradeId)).findFirst();
    }
}
