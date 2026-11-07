package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** La Charger deve comportarsi da muscle car: scatto, velocità di punta e stabilità ad alta velocità. */
class ChargerPerformanceTest {

    private static final VehicleDef CHARGER = VehicleDef.CHARGER_BLACK;

    @Test
    void daZeroACentoOrariInMenoDiSetteSecondi() {
        DriveState s = DriveState.rest(0);
        int ticks = 0;
        while (s.speed() * 3.6 * DrivingModel.TPS < 100 && ticks < 2000) {
            s = DrivingModel.step(CHARGER, s, new DriveInput(1, 0, false), Surface.PAVED, true);
            ticks++;
        }
        double seconds = ticks / DrivingModel.TPS;
        assertTrue(seconds > 3.5 && seconds < 7, "0-100 km/h in " + seconds + " s");
    }

    @Test
    void laVelocitaDiPuntaSuperaICentoQuindici() {
        DriveState s = DriveState.rest(0);
        for (int i = 0; i < 1000; i++) s = DrivingModel.step(CHARGER, s, new DriveInput(1, 0, false), Surface.PAVED, true);
        double kmh = s.speed() * 3.6 * DrivingModel.TPS;
        assertTrue(kmh > 115 && kmh < 140, "punta " + kmh + " km/h");
    }

    @Test
    void giraStrettoAncheAVelocitaSostenuta() {
        // Scheda di gioco: curve strette in citta' e anche a 80 km/h un raggio da strada, non da autostrada.
        // Guida classica: lo sterzo perde efficacia con la velocità, quindi i raggi crescono (come nel riferimento).
        double[][] cases = {{50, 40}, {80, 80}};
        for (double[] c : cases) {
            DriveState s = new DriveState(0, 0, 0, c[0] / 3.6 / DrivingModel.TPS);
            for (int i = 0; i < 40; i++) s = DrivingModel.step(CHARGER, s, new DriveInput(1, 1, false), Surface.PAVED, true);
            double radius = s.speed() * DrivingModel.TPS / Math.abs(s.yawRate());
            assertTrue(radius < c[1], "a " + c[0] + " km/h il raggio e' " + radius + " m");
        }
    }

    @Test
    void adAltaVelocitaLoSterzoCasualeNonMandaInTestacoda() {
        Random rnd = new Random(7);
        for (int n = 0; n < 300; n++) {
            DriveState s = new DriveState(0, 0, 0, 1.2);
            double steer = 0;
            int hold = 0;
            for (int i = 0; i < 300; i++) {
                if (hold-- <= 0) {
                    steer = rnd.nextInt(3) - 1;
                    hold = 3 + rnd.nextInt(25);
                }
                s = DrivingModel.step(CHARGER, s, new DriveInput(1, steer, false), Surface.PAVED, true);
                if (s.speed() * DrivingModel.TPS < 6) continue;
                double along = s.forwardSpeed();
                double lateral = Math.sqrt(Math.max(0, s.speed() * s.speed() - along * along));
                double slip = Math.toDegrees(Math.atan2(lateral, Math.abs(along)));
                assertTrue(slip < 30, "prova " + n + ": deriva " + slip + " al tick " + i);
            }
        }
    }
}
