package dev.qrm.police.weapon.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Inventario finto per i test: capienza configurabile e registro dei caricatori lasciati a terra. */
final class FakeInventory implements MagazineInventory {
    final List<MagazineState> magazines = new ArrayList<>();
    final List<MagazineState> dropped = new ArrayList<>();
    private final int capacity;

    FakeInventory(int capacity, MagazineState... initial) {
        this.capacity = capacity;
        magazines.addAll(List.of(initial));
    }

    /** Inventario senza limiti pratici di spazio. */
    FakeInventory(MagazineState... initial) {
        this(Integer.MAX_VALUE, initial);
    }

    @Override
    public boolean hasLoaded(UUID excluding) {
        return magazines.stream().anyMatch(m -> m.rounds() > 0 && !m.id().equals(excluding));
    }

    @Override
    public Optional<MagazineState> takeFullest(UUID excluding) {
        MagazineState best = null;
        for (MagazineState m : magazines) {
            if (m.rounds() > 0 && !m.id().equals(excluding) && (best == null || m.rounds() > best.rounds())) {
                best = m;
            }
        }
        if (best != null) {
            magazines.remove(best);
        }
        return Optional.ofNullable(best);
    }

    @Override
    public boolean store(MagazineState m) {
        if (magazines.size() >= capacity) {
            return false;
        }
        magazines.add(m);
        return true;
    }

    @Override
    public void drop(MagazineState m) {
        dropped.add(m);
    }

    /** Il giocatore butta a terra un caricatore dell'inventario (fuori dalla sessione). */
    void throwAway(MagazineState m) {
        if (magazines.remove(m)) {
            dropped.add(m);
        }
    }

    Optional<MagazineState> find(UUID id) {
        return magazines.stream().filter(m -> m.id().equals(id)).findFirst();
    }

    int totalRounds() {
        int sum = 0;
        for (MagazineState m : magazines) {
            sum += m.rounds();
        }
        for (MagazineState m : dropped) {
            sum += m.rounds();
        }
        return sum;
    }
}
