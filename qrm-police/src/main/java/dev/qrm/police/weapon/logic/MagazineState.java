package dev.qrm.police.weapon.logic;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Stato di un caricatore: ogni caricatore ha un id proprio, i colpi rimasti e l'elenco dei personaggi
 * che ne conoscono il contenuto esatto.
 */
public record MagazineState(UUID id, int rounds, int capacity, Set<String> knownBy) {
    public MagazineState {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0: " + capacity);
        }
        if (rounds < 0 || rounds > capacity) {
            throw new IllegalArgumentException("rounds out of range 0.." + capacity + ": " + rounds);
        }
        knownBy = Set.copyOf(knownBy);
    }

    public static MagazineState full(UUID id, int capacity) {
        return new MagazineState(id, capacity, capacity, Set.of());
    }

    public MagazineState withRounds(int newRounds) {
        return new MagazineState(id, newRounds, capacity, knownBy);
    }

    /** Copia in cui anche {@code characterId} conosce il contenuto esatto. */
    public MagazineState knownTo(String characterId) {
        Set<String> next = new HashSet<>(knownBy);
        next.add(characterId);
        return new MagazineState(id, rounds, capacity, next);
    }

    public boolean isKnownBy(String characterId) {
        return knownBy.contains(characterId);
    }
}
