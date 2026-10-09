package dev.qrm.neoforge.server;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlayerInputParsingTest {
    @Test
    void itemWithoutNamespaceOrAmountDefaultsToMinecraftAndOne() {
        assertEquals(Optional.of(new ItemSpec("minecraft", "diamond", 1)), ItemSpec.parse("diamond"));
    }

    @Test
    void itemWithAmountAndNamespace() {
        assertEquals(Optional.of(new ItemSpec("minecraft", "diamond", 16)), ItemSpec.parse(" Diamond  16 "));
        assertEquals(Optional.of(new ItemSpec("create", "brass_ingot", 3)), ItemSpec.parse("create:brass_ingot x3"));
    }

    @Test
    void itemAmountsOutOfRangeOrGarbageAreRejected() {
        assertTrue(ItemSpec.parse("diamond 0").isEmpty());
        assertTrue(ItemSpec.parse("diamond 2305").isEmpty());
        assertEquals(ItemSpec.MAX_AMOUNT, ItemSpec.parse("diamond 2304").orElseThrow().amount());
        assertTrue(ItemSpec.parse("").isEmpty());
        assertTrue(ItemSpec.parse(null).isEmpty());
        assertTrue(ItemSpec.parse("dia mond").isEmpty());
        assertTrue(ItemSpec.parse("diamond -3").isEmpty());
    }

    @Test
    void banDurationUnits() {
        assertEquals(Optional.of(Duration.ofMinutes(30)), BanDuration.parse("30m"));
        assertEquals(Optional.of(Duration.ofHours(12)), BanDuration.parse("12h"));
        assertEquals(Optional.of(Duration.ofDays(7)), BanDuration.parse("7d"));
        assertEquals(Optional.of(Duration.ofDays(14)), BanDuration.parse(" 2 W "));
    }

    @Test
    void emptyDurationIsPermanent() {
        assertEquals(Optional.of(Duration.ZERO), BanDuration.parse(""));
        assertEquals(Optional.of(Duration.ZERO), BanDuration.parse("   "));
        assertEquals(Optional.of(Duration.ZERO), BanDuration.parse(null));
    }

    @Test
    void invalidOrHugeDurationsAreRejected() {
        assertTrue(BanDuration.parse("abc").isEmpty());
        assertTrue(BanDuration.parse("0d").isEmpty());
        assertTrue(BanDuration.parse("5").isEmpty());
        assertTrue(BanDuration.parse("-1d").isEmpty());
        assertTrue(BanDuration.parse("3651d").isEmpty());
        assertTrue(BanDuration.parse("99999w").isEmpty());
        assertEquals(Optional.of(Duration.ofDays(3650)), BanDuration.parse("3650d"));
    }

    @Test
    void reasonsAreNormalizedCappedAndDefaulted() {
        assertEquals("grief", Reasons.clean("  grief  ", "x"));
        assertEquals("a b c", Reasons.clean("a   b\t c", "x"));
        assertEquals("x", Reasons.clean("   ", "x"));
        assertEquals("x", Reasons.clean(null, "x"));
        assertEquals(Reasons.MAX_LENGTH, Reasons.clean("y".repeat(500), "x").length());
    }

    @Test
    void fieldsAreSplitByLineAndPaddedWithEmpties() {
        assertArrayEquals(new String[] {"7d", "spam"}, Reasons.fields("7d\nspam", 2));
        assertArrayEquals(new String[] {"7d", ""}, Reasons.fields("7d\n", 2));
        assertArrayEquals(new String[] {"", ""}, Reasons.fields("", 2));
        assertArrayEquals(new String[] {"solo"}, Reasons.fields("solo", 1));
    }
}
