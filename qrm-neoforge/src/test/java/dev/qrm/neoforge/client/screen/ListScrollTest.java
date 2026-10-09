package dev.qrm.neoforge.client.screen;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ListScrollTest {
    @Test
    void offsetIsClampedToTheScrollableRange() {
        ListScroll s = new ListScroll();
        s.scroll(100, 20, 8);
        assertEquals(12, s.offset(20, 8));
        s.scroll(-100, 20, 8);
        assertEquals(0, s.offset(20, 8));
    }

    @Test
    void shortListNeverScrolls() {
        ListScroll s = new ListScroll();
        s.scroll(5, 3, 8);
        assertEquals(0, s.offset(3, 8));
    }

    @Test
    void offsetShrinksWhenTheListShrinks() {
        ListScroll s = new ListScroll();
        s.scroll(10, 30, 8);
        assertEquals(10, s.offset(30, 8));
        assertEquals(2, s.offset(10, 8));
    }

    @Test
    void indexAtHitsRowsAndAccountsForTheOffset() {
        ListScroll s = new ListScroll();
        s.scroll(3, 20, 4);
        assertEquals(3, s.indexAt(100, 100, 13, 20, 4));
        assertEquals(4, s.indexAt(100 + 13, 100, 13, 20, 4));
        assertEquals(6, s.indexAt(100 + 3 * 13 + 12, 100, 13, 20, 4));
    }

    @Test
    void indexAtReturnsMinusOneOutsideTheListOrPastTheEnd() {
        ListScroll s = new ListScroll();
        assertEquals(-1, s.indexAt(99, 100, 13, 20, 4));
        assertEquals(-1, s.indexAt(100 + 4 * 13, 100, 13, 20, 4));
        assertEquals(-1, s.indexAt(100 + 2 * 13, 100, 13, 2, 4));
        assertEquals(-1, s.indexAt(100, 100, 0, 5, 4));
    }
}
