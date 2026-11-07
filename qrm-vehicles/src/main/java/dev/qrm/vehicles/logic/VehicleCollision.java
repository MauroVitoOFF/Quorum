package dev.qrm.vehicles.logic;

/**
 * L'urto tra due veicoli, nel piano orizzontale e senza Minecraft: un impulso lungo la normale di contatto che
 * conserva la quantità di moto e toglie energia secondo il coefficiente di restituzione. Il veicolo più pesante
 * cambia meno velocità; la rotazione dovuta a un colpo fuori asse non è modellata.
 */
public final class VehicleCollision {
    private VehicleCollision() {}

    /** Quanto rimbalzano due carrozzerie: 0 restano attaccate, 1 urto perfettamente elastico. */
    public static final double RESTITUTION = 0.2;

    /**
     * Le variazioni di velocità dei due veicoli. La normale {@code (nx, nz)} è unitaria e punta dal veicolo 2 verso
     * il veicolo 1; le velocità sono in qualunque unità purché la stessa.
     *
     * @return {@code {dv1x, dv1z, dv2x, dv2z}}, o {@code null} se i veicoli si stanno già allontanando
     */
    public static double[] impulse(double m1, double v1x, double v1z, double m2, double v2x, double v2z, double nx,
            double nz, double restitution) {
        if (!(m1 > 0) || !(m2 > 0)) return null;
        double approach = (v1x - v2x) * nx + (v1z - v2z) * nz;
        if (!(approach < 0)) return null;
        double j = -(1 + restitution) * approach / (1 / m1 + 1 / m2);
        return new double[] {j / m1 * nx, j / m1 * nz, -j / m2 * nx, -j / m2 * nz};
    }

    /** Quota dello spostamento di separazione che spetta al veicolo 1: il più leggero si sposta di più. */
    public static double separationShare(double m1, double m2) {
        if (!(m1 > 0) || !(m2 > 0)) return 0.5;
        return m2 / (m1 + m2);
    }
}
