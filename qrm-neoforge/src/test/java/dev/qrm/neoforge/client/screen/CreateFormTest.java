package dev.qrm.neoforge.client.screen;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CreateFormTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    @Test
    void validFormIsParsed() {
        CreateForm.Validation v = CreateForm.validate("  Anna ", "Rossi", "12/03/1990", TODAY);
        assertTrue(v.valid());
        assertEquals(new CreateForm.Parsed("Anna", "Rossi", 1990, 3, 12), v.parsed());
        assertNull(v.errorKey());
    }

    @Test
    void blankNamesAreRejected() {
        assertEquals("qrm.character.form.error.name", CreateForm.validate("", "Rossi", "12/03/1990", TODAY).errorKey());
        assertEquals("qrm.character.form.error.name", CreateForm.validate("Anna", "   ", "12/03/1990", TODAY).errorKey());
        assertEquals("qrm.character.form.error.name", CreateForm.validate(null, null, "12/03/1990", TODAY).errorKey());
    }

    @Test
    void malformedOrImpossibleDatesAreRejected() {
        for (String bad : new String[] {"", "1990-03-12", "12/3/1990", "31/02/1990", "00/01/1990", "12/13/1990", "ab/cd/efgh"}) {
            assertEquals("qrm.character.form.error.date", CreateForm.validate("Anna", "Rossi", bad, TODAY).errorKey(), bad);
        }
        assertEquals("qrm.character.form.error.date", CreateForm.validate("Anna", "Rossi", null, TODAY).errorKey());
    }

    @Test
    void dateBoundsAreInclusive() {
        assertTrue(CreateForm.validate("Anna", "Rossi", "01/01/1900", TODAY).valid());
        assertFalse(CreateForm.validate("Anna", "Rossi", "31/12/1899", TODAY).valid());
        assertTrue(CreateForm.validate("Anna", "Rossi", "15/06/2026", TODAY).valid());
        assertFalse(CreateForm.validate("Anna", "Rossi", "16/06/2026", TODAY).valid());
    }
}
