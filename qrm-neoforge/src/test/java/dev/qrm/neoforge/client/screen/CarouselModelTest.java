package dev.qrm.neoforge.client.screen;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.neoforge.net.CharacterCard;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CarouselModelTest {
    private static CharacterCard card(String first) {
        UUID id = UUID.randomUUID();
        return new CharacterCard(id, first, "Rossi", LocalDate.of(1990, 1, 1), CharacterCard.documentNumberOf(id), Optional.empty());
    }

    @Test
    void emptyRosterShowsOnlyNewOrLocked() {
        CarouselModel m = new CarouselModel();
        m.update(List.of(), 2, Optional.empty());
        assertEquals(1, m.size());
        assertEquals(CarouselModel.Kind.NEW, m.selected().kind());
        m.update(List.of(), 0, Optional.empty());
        assertEquals(CarouselModel.Kind.LOCKED, m.selected().kind());
    }

    @Test
    void trailingEntryIsNewWhenSlotsRemainElseLocked() {
        CharacterCard a = card("Anna");
        CharacterCard b = card("Bruno");
        CarouselModel m = new CarouselModel();
        m.update(List.of(a, b), 3, Optional.empty());
        assertEquals(List.of(CarouselModel.Kind.CARD, CarouselModel.Kind.CARD, CarouselModel.Kind.NEW),
                m.entries().stream().map(CarouselModel.Entry::kind).toList());
        m.update(List.of(a, b), 2, Optional.empty());
        assertEquals(CarouselModel.Kind.LOCKED, m.entries().get(2).kind());
    }

    @Test
    void selectionStartsOnTheActiveCharacterAndClampsAtTheEnds() {
        CharacterCard a = card("Anna");
        CharacterCard b = card("Bruno");
        CarouselModel m = new CarouselModel();
        m.update(List.of(a, b), 3, Optional.of(b.id()));
        assertEquals(1, m.selectedIndex());
        assertSame(b, m.selected().card());
        m.next();
        assertEquals(2, m.selectedIndex());
        m.next();
        assertEquals(2, m.selectedIndex());
        m.select(-5);
        assertEquals(0, m.selectedIndex());
        m.previous();
        assertEquals(0, m.selectedIndex());
        m.select(1);
        assertEquals(1, m.selectedIndex());
    }

    @Test
    void updateRealignsOnTheActiveCharacterAndKeepsIndexInRange() {
        CharacterCard a = card("Anna");
        CharacterCard b = card("Bruno");
        CarouselModel m = new CarouselModel();
        m.update(List.of(a, b), 3, Optional.empty());
        m.select(2);
        m.update(List.of(a), 3, Optional.of(a.id()));
        assertEquals(0, m.selectedIndex());
        m.update(List.of(a), 3, Optional.empty());
        assertTrue(m.selectedIndex() < m.size());
    }
}
