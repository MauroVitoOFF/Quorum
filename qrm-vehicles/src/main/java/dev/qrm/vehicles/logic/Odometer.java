package dev.qrm.vehicles.logic;

/** Il contachilometri: strada totale in blocchi (1 km = 1000 blocchi). Non scende mai e non si azzera. */
public final class Odometer {

    private static final double BLOCKS_PER_KM = 1000.0;

    private double blocks;

    public Odometer(double blocks) {
        this.blocks = blocks > 0 ? blocks : 0;
    }

    public static Odometer fromKm(double km) {
        return new Odometer(km * BLOCKS_PER_KM);
    }

    public void add(double blocks) {
        if (blocks > 0) this.blocks += blocks;
    }

    public double blocks() {
        return blocks;
    }

    public double km() {
        return blocks / BLOCKS_PER_KM;
    }
}
