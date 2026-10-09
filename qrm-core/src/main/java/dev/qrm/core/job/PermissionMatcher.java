package dev.qrm.core.job;

import dev.qrm.api.permission.PermissionEffect;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** Regole di risoluzione dei permessi, pure (nessun accesso al database). */
public final class PermissionMatcher {
    private PermissionMatcher() {}

    public static boolean matches(String pattern, String node) {
        if (pattern.equals("*")) return true;
        if (pattern.endsWith(".*")) return node.startsWith(pattern.substring(0, pattern.length() - 1));
        return pattern.equals(node);
    }

    /** Nodo esatto = massima; jolly = lunghezza del prefisso con il punto; {@code *} = 0. */
    public static int specificity(String pattern) {
        if (pattern.equals("*")) return 0;
        if (pattern.endsWith(".*")) return pattern.length() - 1;
        return Integer.MAX_VALUE;
    }

    /** L'effetto dell'override più specifico che corrisponde al nodo (a parità vince DENY). */
    public static Optional<PermissionEffect> overrideEffect(Map<String, PermissionEffect> overrides, String node) {
        PermissionEffect best = null;
        int bestSpecificity = -1;
        for (Map.Entry<String, PermissionEffect> e : overrides.entrySet()) {
            if (!matches(e.getKey(), node)) continue;
            int spec = specificity(e.getKey());
            if (spec > bestSpecificity || (spec == bestSpecificity && e.getValue() == PermissionEffect.DENY)) {
                best = e.getValue();
                bestSpecificity = spec;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * @param jobGrantsApply true se il personaggio ha un lavoro noto ed è in servizio
     * @param jobPatterns pattern ereditati dal grade (ignorati se {@code jobGrantsApply} è false)
     */
    public static boolean has(String node, boolean jobGrantsApply, Set<String> jobPatterns,
            Map<String, PermissionEffect> overrides) {
        Optional<PermissionEffect> override = overrideEffect(overrides, node);
        if (override.isPresent()) return override.get() == PermissionEffect.GRANT;
        if (!jobGrantsApply) return false;
        return jobPatterns.stream().anyMatch(p -> matches(p, node));
    }

    /** Pattern concessi (lavoro + GRANT) meno i nodi esatti negati, in ordine alfabetico. */
    public static Set<String> effective(boolean jobGrantsApply, Set<String> jobPatterns,
            Map<String, PermissionEffect> overrides) {
        Set<String> out = new TreeSet<>();
        if (jobGrantsApply) out.addAll(jobPatterns);
        overrides.forEach((pattern, effect) -> {
            if (effect == PermissionEffect.GRANT) out.add(pattern);
        });
        overrides.forEach((pattern, effect) -> {
            if (effect == PermissionEffect.DENY && specificity(pattern) == Integer.MAX_VALUE) out.remove(pattern);
        });
        return out;
    }
}
