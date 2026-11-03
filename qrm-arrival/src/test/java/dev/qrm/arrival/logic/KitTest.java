package dev.qrm.arrival.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class KitTest {
    @Test
    void defaults_hasOneCashEntryOf10000() {
        assertEquals(List.of(new KitEntry("qrm_bank:cash_qrm_10000", 1)), Kit.defaults().entries());
    }

    @Test
    void parse_readsItemsAndCounts() {
        Kit kit = Kit.parse("{\"items\":[{\"item\":\"minecraft:bread\",\"count\":3},{\"item\":\"minecraft:apple\",\"count\":1}]}");
        assertEquals(List.of(new KitEntry("minecraft:bread", 3), new KitEntry("minecraft:apple", 1)), kit.entries());
    }

    @Test
    void parse_rejectsCountZeroOrOver64() {
        assertThrows(IllegalArgumentException.class, () -> Kit.parse("{\"items\":[{\"item\":\"minecraft:bread\",\"count\":0}]}"));
        assertThrows(IllegalArgumentException.class, () -> Kit.parse("{\"items\":[{\"item\":\"minecraft:bread\",\"count\":65}]}"));
    }

    @Test
    void parse_rejectsBlankItem() {
        assertThrows(IllegalArgumentException.class, () -> Kit.parse("{\"items\":[{\"item\":\" \",\"count\":1}]}"));
    }

    @Test
    void parse_rejectsBrokenJson() {
        assertThrows(IllegalArgumentException.class, () -> Kit.parse("{not json"));
        assertThrows(IllegalArgumentException.class, () -> Kit.parse("{}"));
    }

    @Test
    void parse_emptyItemsIsAnEmptyKit() {
        assertTrue(Kit.parse("{\"items\":[]}").entries().isEmpty());
    }
}
