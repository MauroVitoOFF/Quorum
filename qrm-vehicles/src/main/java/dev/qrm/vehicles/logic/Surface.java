package dev.qrm.vehicles.logic;

/** Il fondo su cui poggiano le ruote: quanto tiene e quanto permette di correre rispetto alla strada. */
public enum Surface {
    PAVED(1.0, 1.0),
    DIRT(0.85, 0.9),
    ROUGH(0.6, 0.65);

    private final double gripFactor;
    private final double speedFactor;

    Surface(double gripFactor, double speedFactor) {
        this.gripFactor = gripFactor;
        this.speedFactor = speedFactor;
    }

    public double gripFactor() {
        return gripFactor;
    }

    public double speedFactor() {
        return speedFactor;
    }
}
