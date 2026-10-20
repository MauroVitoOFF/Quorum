package dev.qrm.police.weapon.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArmoryRulesTest {
    @Test
    void unCaricatoreDaCinqueDiventaTreDaDiciassetteTuttiNoti() {
        UUID id = UUID.randomUUID();
        MagazineState carried = new MagazineState(id, 5, 17, java.util.Set.of());
        List<MagazineState> out =
                ArmoryRules.restock(List.of(carried), 3, 17, "pg1", UUID::randomUUID);
        assertEquals(3, out.size());
        assertEquals(id, out.get(0).id());
        for (MagazineState m : out) {
            assertEquals(17, m.rounds());
            assertTrue(m.isKnownBy("pg1"));
        }
    }

    @Test
    void conQuattroCaricatoriNessunoVieneTolto() {
        List<MagazineState> four = List.of(
                MagazineState.full(UUID.randomUUID(), 17).withRounds(1),
                MagazineState.full(UUID.randomUUID(), 17),
                MagazineState.full(UUID.randomUUID(), 17),
                MagazineState.full(UUID.randomUUID(), 17));
        List<MagazineState> out = ArmoryRules.restock(four, 3, 17, "pg1", UUID::randomUUID);
        assertEquals(4, out.size());
        assertEquals(17, out.get(0).rounds());
    }
}
