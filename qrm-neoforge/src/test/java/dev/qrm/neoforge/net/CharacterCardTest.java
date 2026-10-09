package dev.qrm.neoforge.net;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CharacterCardTest {
    @Test
    void documentNumberIsStableAndFormatted() {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String n = CharacterCard.documentNumberOf(id);
        assertEquals(n, CharacterCard.documentNumberOf(id));
        assertTrue(n.matches("QRM-[0-9A-F]{5}"), n);
    }

    @Test
    void differentIdsUsuallyGiveDifferentNumbers() {
        assertNotEquals(CharacterCard.documentNumberOf(new UUID(1, 2)), CharacterCard.documentNumberOf(new UUID(3, 4)));
    }

    @Test
    void cardKeepsFields() {
        UUID id = UUID.randomUUID();
        CharacterCard c = new CharacterCard(id, "Anna", "Rossi", LocalDate.of(1990, 3, 12),
                CharacterCard.documentNumberOf(id), Optional.of("Polizia"));
        assertEquals("Anna", c.firstName());
        assertEquals(Optional.of("Polizia"), c.jobLabel());
    }

    @Test
    void resultCodeOutOfRangeIsError() {
        assertEquals(ResultCode.OK, ResultCode.fromOrdinal(0));
        assertEquals(ResultCode.ERROR, ResultCode.fromOrdinal(99));
        assertEquals(ResultCode.ERROR, ResultCode.fromOrdinal(-1));
    }

    @Test
    void actionOutOfRangeIsRoster() {
        assertEquals(CharacterAction.ARCHIVE, CharacterAction.fromOrdinal(3));
        assertEquals(CharacterAction.ROSTER, CharacterAction.fromOrdinal(42));
    }
}
