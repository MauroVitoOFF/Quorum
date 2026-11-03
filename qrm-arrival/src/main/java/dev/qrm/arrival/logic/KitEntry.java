package dev.qrm.arrival.logic;

/** Un oggetto del kit: id di oggetto Minecraft (es. {@code qrm_bank:cash_qrm_10000}) e quantità 1..64. */
public record KitEntry(String itemId, int count) {
    public KitEntry {
        if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("item id is blank");
        if (count < 1 || count > 64) throw new IllegalArgumentException("count must be 1..64: " + count);
    }
}
