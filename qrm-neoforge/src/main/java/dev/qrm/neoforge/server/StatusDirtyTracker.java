package dev.qrm.neoforge.server;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giocatori il cui stato va reinviato, più la mappa giocatore → personaggio attivo (per risalire dal personaggio al
 * giocatore). Sicuro per l'accesso da più thread: gli eventi del Core possono arrivare da qualunque thread, lo
 * svuotamento avviene nel thread del server.
 */
public final class StatusDirtyTracker {
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final Map<UUID, UUID> activeCharacterByPlayer = new ConcurrentHashMap<>();

    public void markPlayer(UUID player) {
        dirty.add(player);
    }

    public void markAll(Collection<UUID> players) {
        dirty.addAll(players);
    }

    /** Segna il giocatore che ha questo personaggio attivo; ignorato se nessuno lo ha (secondo l'ultima mappa). */
    public void markCharacter(UUID character) {
        activeCharacterByPlayer.forEach((player, active) -> {
            if (active.equals(character)) dirty.add(player);
        });
    }

    /** @param character null se il giocatore non ha un personaggio attivo */
    public void setActiveCharacter(UUID player, UUID character) {
        if (character == null) activeCharacterByPlayer.remove(player);
        else activeCharacterByPlayer.put(player, character);
    }

    public void forget(UUID player) {
        dirty.remove(player);
        activeCharacterByPlayer.remove(player);
    }

    /** Restituisce e rimuove i giocatori da aggiornare. */
    public Set<UUID> drain() {
        Set<UUID> out = new HashSet<>();
        for (UUID id : dirty) {
            if (dirty.remove(id)) out.add(id);
        }
        return out;
    }
}
