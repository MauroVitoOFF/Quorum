package dev.qrm.wardrobe.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.id.CharacterId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WardrobeServiceTest {
    static final class MemoryStorage implements WardrobeStorage {
        final Map<CharacterId, String> data = new HashMap<>();

        @Override
        public Optional<String> read(CharacterId c) {
            return Optional.ofNullable(data.get(c));
        }

        @Override
        public void write(CharacterId c, String outfitIdOrNull) {
            if (outfitIdOrNull == null) data.remove(c);
            else data.put(c, outfitIdOrNull);
        }
    }

    final CharacterId anna = new CharacterId(UUID.randomUUID());
    final Wearer officer = new Wearer("police", "officer", true);
    final OutfitDefinition patrol = WardrobeRulesTest.outfit("patrol", "police", "officer");
    final OutfitDefinition gala = WardrobeRulesTest.outfit("gala", "police", "captain");
    final OutfitDefinition cab = WardrobeRulesTest.outfit("cab", "taxi", null);
    MemoryStorage storage;
    WardrobeService service;

    @BeforeEach
    void setUp() {
        storage = new MemoryStorage();
        service = new WardrobeService(storage, WardrobeRulesTest.JOBS);
        service.setCatalog(OutfitCatalog.of(List.of(patrol, gala, cab)));
    }

    @Test
    void validChoiceIsSavedAndReadBack() {
        assertEquals(ChooseResult.OK, service.choose(anna, "patrol", "police", Optional.of(officer)));
        assertEquals(Optional.of("patrol"), service.worn(anna));
        assertEquals(Optional.of("patrol"), storage.read(anna));
    }

    @Test
    void civilClothesRemoveTheOutfit() {
        service.choose(anna, "patrol", "police", Optional.of(officer));
        assertEquals(ChooseResult.OK, service.choose(anna, "", "police", Optional.of(officer)));
        assertTrue(service.worn(anna).isEmpty());
        assertTrue(storage.read(anna).isEmpty());
    }

    @Test
    void rejectsUnknownOtherJobAndTooLowGrade() {
        assertEquals(ChooseResult.UNKNOWN_OUTFIT, service.choose(anna, "nope", "police", Optional.of(officer)));
        assertEquals(ChooseResult.WRONG_JOB, service.choose(anna, "cab", "police", Optional.of(officer)));
        assertEquals(ChooseResult.NOT_ALLOWED, service.choose(anna, "gala", "police", Optional.of(officer)));
        assertEquals(ChooseResult.NOT_ALLOWED, service.choose(anna, "patrol", "police", Optional.empty()));
        assertTrue(service.worn(anna).isEmpty());
    }

    @Test
    void revalidateRemovesWhenNoLongerValid() {
        service.choose(anna, "patrol", "police", Optional.of(officer));
        assertFalse(service.revalidate(anna, Optional.of(officer)));
        assertTrue(service.revalidate(anna, Optional.of(new Wearer("police", "officer", false))));
        assertTrue(service.worn(anna).isEmpty());
        service.choose(anna, "patrol", "police", Optional.of(officer));
        assertTrue(service.revalidate(anna, Optional.empty()));
        assertTrue(storage.read(anna).isEmpty());
    }

    @Test
    void reloadWithoutTheOutfitRemovesItOnRevalidate() {
        service.choose(anna, "patrol", "police", Optional.of(officer));
        service.setCatalog(OutfitCatalog.of(List.of(gala)));
        assertTrue(service.revalidate(anna, Optional.of(officer)));
        assertTrue(service.worn(anna).isEmpty());
    }

    @Test
    void aNewServiceOnTheSameStorageRereadsTheOutfit() {
        service.choose(anna, "patrol", "police", Optional.of(officer));
        WardrobeService fresh = new WardrobeService(storage, WardrobeRulesTest.JOBS);
        fresh.setCatalog(OutfitCatalog.of(List.of(patrol)));
        assertEquals(Optional.of("patrol"), fresh.worn(anna));
        assertEquals(Map.of(anna, "patrol"), fresh.allWorn());
    }
}
