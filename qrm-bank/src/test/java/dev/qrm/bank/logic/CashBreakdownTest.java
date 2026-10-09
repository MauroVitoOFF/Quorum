package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CashBreakdownTest {
    private static final List<Long> DESC = List.of(10000L, 5000L, 1000L, 500L, 100L);

    @Test
    void breaksIntoFewestNotes() {
        Optional<Map<Long, Integer>> r = CashBreakdown.of(DESC, 17600);
        assertTrue(r.isPresent());
        assertEquals(Map.of(10000L, 1, 5000L, 1, 1000L, 2, 500L, 1, 100L, 1), r.get());
    }

    @Test
    void exactSingleNote() {
        assertEquals(Map.of(5000L, 1), CashBreakdown.of(DESC, 5000).orElseThrow());
    }

    @Test
    void rejectsAmountsNotMultipleOfSmallestNote() {
        assertTrue(CashBreakdown.of(DESC, 150).isEmpty());
    }

    @Test
    void rejectsZeroNegativeAndNoDenominations() {
        assertTrue(CashBreakdown.of(DESC, 0).isEmpty());
        assertTrue(CashBreakdown.of(DESC, -100).isEmpty());
        assertTrue(CashBreakdown.of(List.of(), 100).isEmpty());
    }

    @Test
    void hugeAmountsDoNotOverflow() {
        Optional<Map<Long, Integer>> r = CashBreakdown.of(List.of(100L), 100L * 3_000_000_000L);
        assertTrue(r.isEmpty());
    }
}
