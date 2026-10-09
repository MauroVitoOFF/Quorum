package dev.qrm.neoforge.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StaffPanelLayoutTest {
    // pannello a (10,20) largo 100; titolo alto 12 (y 20..32); righe da y=32, alte 11; finestra di 3 righe su 5 totali, start 1
    private final StaffPanelLayout l = new StaffPanelLayout(10, 20, 100, 12, 12, 11, 3, 5, 1);

    @Test
    void titleHit() {
        assertTrue(l.isTitle(50, 25));
        assertFalse(l.isTitle(50, 40));
        assertFalse(l.isTitle(5, 25));
    }

    @Test
    void rowHitReturnsAbsoluteIndexOfTheWindow() {
        assertEquals(1, l.rowAt(50, 33));      // prima riga visibile = indice 1
        assertEquals(2, l.rowAt(50, 44));
        assertEquals(3, l.rowAt(50, 55));
    }

    @Test
    void outsideRowsIsMinusOne() {
        assertEquals(-1, l.rowAt(50, 31));     // sopra le righe
        assertEquals(-1, l.rowAt(50, 66));     // sotto la finestra
        assertEquals(-1, l.rowAt(120, 40));    // a destra
    }

    @Test
    void windowIsCappedByTheNumberOfRows() {
        StaffPanelLayout small = new StaffPanelLayout(0, 0, 100, 12, 12, 11, 8, 2, 0);
        assertEquals(1, small.rowAt(10, 12 + 11 + 1));
        assertEquals(-1, small.rowAt(10, 12 + 22 + 1));
    }

    @Test
    void containsCoversTitleAndRows() {
        assertTrue(l.contains(50, 25));
        assertTrue(l.contains(50, 60));
        assertFalse(l.contains(50, 200));
    }

    @Test
    void headerWithInfoLinesPushesRowsDownButTitleStaysOnTop() {
        StaffPanelLayout card = new StaffPanelLayout(0, 0, 100, 12, 52, 11, 8, 4, 0);
        assertTrue(card.isTitle(10, 5));
        assertFalse(card.isTitle(10, 20));
        assertEquals(-1, card.rowAt(10, 30));         // dentro l'intestazione
        assertEquals(0, card.rowAt(10, 53));
    }

    @Test
    void fitRowsFollowsTheAvailableHeightWithinBounds() {
        assertEquals(7, StaffPanelLayout.fitRows(240, 40, 74, 18, 3, 10));    // (240-40-74)/18
        assertEquals(3, StaffPanelLayout.fitRows(120, 40, 74, 18, 3, 10));    // troppo poco spazio: minimo
        assertEquals(10, StaffPanelLayout.fitRows(1000, 40, 74, 18, 3, 10));  // tanto spazio: massimo
    }
}
