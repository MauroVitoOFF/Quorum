package dev.qrm.client.api.theme;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import net.minecraft.resources.Identifier;

/**
 * Frammenti di tema dei moduli. Un modulo registra, nel proprio costruttore client, una risorsa nel formato di
 * {@code default.json} le cui chiavi iniziano per {@code "<modId>."}; QRM la unisce al tema a ogni ricarica delle
 * risorse e un resource pack può sostituirne i valori.
 */
public final class ThemeFragments {
    private static final Pattern MOD_ID = Pattern.compile("[a-z][a-z0-9_]{0,31}");
    private static final Map<String, Fragment> FRAGMENTS = new LinkedHashMap<>();

    public record Fragment(String modId, Identifier resource) {}

    private ThemeFragments() {}

    /**
     * @param modId id del modulo: {@code [a-z][a-z0-9_]{0,31}}, una sola registrazione
     * @throws IllegalArgumentException se l'id non è valido
     * @throws IllegalStateException se il modulo ha già registrato un frammento
     */
    public static void register(String modId, Identifier resource) {
        Objects.requireNonNull(modId, "modId");
        Objects.requireNonNull(resource, "resource");
        if (!MOD_ID.matcher(modId).matches()) throw new IllegalArgumentException("invalid mod id: " + modId);
        synchronized (FRAGMENTS) {
            if (FRAGMENTS.containsKey(modId)) {
                throw new IllegalStateException("theme fragment already registered: " + modId);
            }
            FRAGMENTS.put(modId, new Fragment(modId, resource));
        }
    }

    /** Copia dei frammenti registrati, nell'ordine di registrazione. */
    public static List<Fragment> all() {
        synchronized (FRAGMENTS) {
            return new ArrayList<>(FRAGMENTS.values());
        }
    }
}
