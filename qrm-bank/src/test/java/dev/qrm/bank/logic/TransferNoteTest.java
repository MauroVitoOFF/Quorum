package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TransferNoteTest {
    @Test
    void cleanStripsControlCharactersAndEdges() {
        assertEquals("ciao mondo", TransferNote.clean("  ciao\n mondo\t\u0000 "));
        assertEquals("", TransferNote.clean(null));
        assertEquals("", TransferNote.clean("   \n"));
    }

    @Test
    void cleanTruncatesByCodePoints() {
        String longText = "a".repeat(100);
        assertEquals(TransferNote.MAX_LENGTH, TransferNote.clean(longText).length());
        String emoji = "😀".repeat(100);
        assertEquals(TransferNote.MAX_LENGTH, TransferNote.clean(emoji).codePointCount(0, TransferNote.clean(emoji).length()));
    }

    @Test
    void jsonRoundTripsQuotesAndUnicode() {
        String note = "Affitto \"ottobre\" \\ è ok";
        assertEquals(note, TransferNote.fromJson(TransferNote.toJson(note)));
        assertEquals("{}", TransferNote.toJson(""));
    }

    @Test
    void fromJsonToleratesAnythingElse() {
        assertEquals("", TransferNote.fromJson(null));
        assertEquals("", TransferNote.fromJson(""));
        assertEquals("", TransferNote.fromJson("{}"));
        assertEquals("", TransferNote.fromJson("not json"));
        assertEquals("", TransferNote.fromJson("[1,2]"));
        assertEquals("", TransferNote.fromJson("{\"note\":{\"a\":1}}"));
    }
}
