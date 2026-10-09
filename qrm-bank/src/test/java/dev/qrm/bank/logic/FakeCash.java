package dev.qrm.bank.logic;

import java.util.HashMap;
import java.util.Map;

/** Inventario finto: valuta → (taglio → quantità). */
final class FakeCash implements CashInventory {
    final Map<String, Map<Long, Integer>> notes = new HashMap<>();
    boolean full;
    boolean failRemove;

    @Override
    public long count(String currency) {
        long total = 0;
        for (Map.Entry<Long, Integer> e : notes.getOrDefault(currency, Map.of()).entrySet()) {
            total += e.getKey() * e.getValue();
        }
        return total;
    }

    @Override
    public boolean canFit(String currency, Map<Long, Integer> toGive) {
        return !full;
    }

    @Override
    public void give(String currency, Map<Long, Integer> toGive) {
        Map<Long, Integer> mine = notes.computeIfAbsent(currency, k -> new HashMap<>());
        toGive.forEach((denomination, n) -> mine.merge(denomination, n, Integer::sum));
    }

    @Override
    public boolean remove(String currency, long expectedTotal) {
        if (failRemove || count(currency) != expectedTotal) return false;
        notes.remove(currency);
        return true;
    }
}
