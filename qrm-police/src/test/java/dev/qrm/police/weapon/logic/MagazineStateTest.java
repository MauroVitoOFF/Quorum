package dev.qrm.police.weapon.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MagazineStateTest {
    @Test
    void roundsOutsideRangeAreRejected() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new MagazineState(id, -1, 17, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new MagazineState(id, 18, 17, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> MagazineState.full(id, 17).withRounds(18));
        assertThrows(IllegalArgumentException.class, () -> MagazineState.full(id, 17).withRounds(-1));
    }

    @Test
    void capacityMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new MagazineState(UUID.randomUUID(), 0, 0, Set.of()));
    }

    @Test
    void fullStartsWithAllRoundsAndNobodyKnowsIt() {
        MagazineState m = MagazineState.full(UUID.randomUUID(), 17);
        assertEquals(17, m.rounds());
        assertTrue(m.knownBy().isEmpty());
    }

    @Test
    void knownToDoesNotMutateOriginal() {
        MagazineState m = MagazineState.full(UUID.randomUUID(), 17);
        MagazineState known = m.knownTo("char-1");
        assertFalse(m.isKnownBy("char-1"));
        assertTrue(known.isKnownBy("char-1"));
        assertEquals(m.id(), known.id());
    }

    @Test
    void withRoundsKeepsIdAndKnowledge() {
        MagazineState m = MagazineState.full(UUID.randomUUID(), 17).knownTo("a");
        MagazineState fewer = m.withRounds(5);
        assertEquals(5, fewer.rounds());
        assertEquals(m.id(), fewer.id());
        assertTrue(fewer.isKnownBy("a"));
    }

    @Test
    void knownByIsDefensivelyCopied() {
        Set<String> mutable = new HashSet<>(Set.of("a"));
        MagazineState m = new MagazineState(UUID.randomUUID(), 3, 17, mutable);
        mutable.add("b");
        assertFalse(m.isKnownBy("b"));
        assertThrows(UnsupportedOperationException.class, () -> m.knownBy().add("c"));
    }

    @Test
    void emptyWeaponHasNoMagazineAndIsNotChambered() {
        UUID id = UUID.randomUUID();
        WeaponState w = WeaponState.empty(id);
        assertEquals(id, w.id());
        assertEquals(Optional.empty(), w.magazine());
        assertFalse(w.chambered());
        assertFalse(w.slideLocked());
    }
}
