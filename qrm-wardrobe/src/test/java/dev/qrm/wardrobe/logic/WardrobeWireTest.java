package dev.qrm.wardrobe.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Round-trip e limiti del formato dei pacchetti: puro Java, si prova senza Minecraft. */
class WardrobeWireTest {
    private static WardrobeWire.CatalogEntry entry(int pngBytes) {
        byte[] png = new byte[pngBytes];
        for (int i = 0; i < png.length; i++) png[i] = (byte) i;
        return new WardrobeWire.CatalogEntry("agente", "Agente è di pattuglia", "police", "officer", true, png);
    }

    @Test
    void catalogEntryRoundTrips() {
        WardrobeWire.CatalogEntry e = entry(2000);
        WardrobeWire.CatalogEntry back = WardrobeWire.decodeEntry(WardrobeWire.encode(e));
        assertEquals(e.id(), back.id());
        assertEquals(e.label(), back.label());
        assertEquals(e.job(), back.job());
        assertEquals(e.minGrade(), back.minGrade());
        assertTrue(back.slim());
        assertArrayEquals(e.png(), back.png());
    }

    @Test
    void entryWithoutMinGradeKeepsItEmpty() {
        WardrobeWire.CatalogEntry e = new WardrobeWire.CatalogEntry("a", "A", "police", "", false, new byte[] {1});
        assertEquals("", WardrobeWire.decodeEntry(WardrobeWire.encode(e)).minGrade());
    }

    @Test
    void pngOverTheLimitIsRejectedBothWays() {
        assertThrows(IllegalArgumentException.class, () -> WardrobeWire.encode(entry(OutfitCatalog.MAX_PNG_BYTES + 1)));
        byte[] ok = WardrobeWire.encode(entry(OutfitCatalog.MAX_PNG_BYTES));
        assertEquals(OutfitCatalog.MAX_PNG_BYTES, WardrobeWire.decodeEntry(ok).png().length);
    }

    @Test
    void truncatedOrGarbageEntryIsRejected() {
        byte[] good = WardrobeWire.encode(entry(100));
        byte[] cut = java.util.Arrays.copyOf(good, good.length / 2);
        assertThrows(IllegalArgumentException.class, () -> WardrobeWire.decodeEntry(cut));
        assertThrows(IllegalArgumentException.class, () -> WardrobeWire.decodeEntry(new byte[] {9, 9, 9}));
    }

    @Test
    void wornRoundTripsAndEmptyMeansNobody() {
        Map<UUID, String> worn = new LinkedHashMap<>();
        worn.put(UUID.randomUUID(), "agente");
        worn.put(UUID.randomUUID(), "gala");
        assertEquals(worn, WardrobeWire.decodeWorn(WardrobeWire.encodeWorn(worn)));
        assertTrue(WardrobeWire.decodeWorn(WardrobeWire.encodeWorn(Map.of())).isEmpty());
    }

    @Test
    void openRoundTripsAndRejectsTooManyIds() {
        WardrobeWire.Open open = new WardrobeWire.Open(List.of("agente", "gala"), "agente");
        assertEquals(open, WardrobeWire.decodeOpen(WardrobeWire.encode(open)));
        List<String> many = java.util.stream.IntStream.range(0, OutfitCatalog.MAX_OUTFITS + 1).mapToObj(i -> "d" + i).toList();
        assertThrows(IllegalArgumentException.class, () -> WardrobeWire.encode(new WardrobeWire.Open(many, "")));
    }

    @Test
    void chooseRoundTripsAndRejectsLongIds() {
        assertEquals("agente", WardrobeWire.decodeChoose(WardrobeWire.encodeChoose("agente")));
        assertEquals("", WardrobeWire.decodeChoose(WardrobeWire.encodeChoose("")));
        assertThrows(IllegalArgumentException.class, () -> WardrobeWire.encodeChoose("x".repeat(65)));
        assertThrows(IllegalArgumentException.class, () -> WardrobeWire.decodeChoose(new byte[] {0, 100}));
    }
}
