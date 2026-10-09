package dev.qrm.api.permission;

import dev.qrm.api.id.CharacterId;
import java.util.Map;
import java.util.Set;

public interface PermissionService {
    /**
     * @param node nodo esatto (mai un pattern con {@code *})
     * @throws IllegalArgumentException se {@code node} non è un nodo esatto valido
     */
    boolean has(CharacterId id, String node);

    /** Pattern concessi: nodi del lavoro (se in servizio) più i GRANT, meno i nodi esatti negati. Vuoto se il personaggio non è ACTIVE. */
    Set<String> effective(CharacterId id);

    /** @throws IllegalArgumentException se {@code node} non è un pattern valido; non scrive nulla per un personaggio inesistente */
    void grant(CharacterId id, String node);

    /** Come {@link #grant} ma con effetto DENY. */
    void deny(CharacterId id, String node);

    /** Rimuove l'override del nodo (no-op se assente). */
    void clear(CharacterId id, String node);

    /** Override del personaggio (nodo → GRANT/DENY), ordinati per nodo. */
    Map<String, PermissionEffect> overridesOf(CharacterId id);
}
