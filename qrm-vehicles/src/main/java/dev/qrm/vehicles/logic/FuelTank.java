package dev.qrm.vehicles.logic;

/** Il serbatoio: litri contenuti, mai sotto zero né sopra la capacità. Valori negativi o non numerici si ignorano. */
public final class FuelTank {

    private final double capacity;
    private double level;

    public FuelTank(double capacity, double level) {
        this.capacity = Math.max(0, capacity);
        this.level = Double.isNaN(level) ? 0 : Math.max(0, Math.min(this.capacity, level));
    }

    public double level() {
        return level;
    }

    public double capacity() {
        return capacity;
    }

    public double room() {
        return capacity - level;
    }

    public boolean isEmpty() {
        return level <= 0;
    }

    /** Consuma in proporzione alla strada fatta: {@code blocks} percorsi a {@code per100Blocks} litri ogni 100. */
    public void consume(double blocks, double per100Blocks) {
        if (!(blocks > 0) || !(per100Blocks > 0)) return;
        level = Math.max(0, level - blocks / 100.0 * per100Blocks);
    }

    /** Aggiunge fino alla capacità; restituisce i litri davvero aggiunti. */
    public double add(double liters) {
        if (!(liters > 0)) return 0;
        double added = Math.min(liters, room());
        level += added;
        return added;
    }
}
