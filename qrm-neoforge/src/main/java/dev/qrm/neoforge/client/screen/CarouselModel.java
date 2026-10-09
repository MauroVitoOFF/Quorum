package dev.qrm.neoforge.client.screen;

import dev.qrm.neoforge.net.CharacterCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stato di selezione del carosello (puro): carte, poi "+ Nuovo" se ci sono slot liberi, altrimenti "bloccata". */
public final class CarouselModel {
    public enum Kind { CARD, NEW, LOCKED }

    public record Entry(Kind kind, CharacterCard card) {}

    private List<CharacterCard> cards = List.of();
    private int slots;
    private int index;

    public void update(List<CharacterCard> newCards, int newSlots, Optional<UUID> activeId) {
        this.cards = List.copyOf(newCards);
        this.slots = newSlots;
        int target = 0;
        if (activeId.isPresent()) {
            for (int i = 0; i < cards.size(); i++) {
                if (cards.get(i).id().equals(activeId.get())) target = i;
            }
        }
        this.index = clamp(target);
    }

    public List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        for (CharacterCard c : cards) out.add(new Entry(Kind.CARD, c));
        out.add(new Entry(cards.size() < slots ? Kind.NEW : Kind.LOCKED, null));
        return out;
    }

    public int size() {
        return cards.size() + 1;
    }

    public int selectedIndex() {
        return index;
    }

    public Entry selected() {
        return entries().get(index);
    }

    public void next() {
        index = clamp(index + 1);
    }

    public void previous() {
        index = clamp(index - 1);
    }

    public void select(int i) {
        index = clamp(i);
    }

    private int clamp(int i) {
        return Math.max(0, Math.min(i, size() - 1));
    }
}
