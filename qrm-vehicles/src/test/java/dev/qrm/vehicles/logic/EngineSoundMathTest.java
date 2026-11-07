package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EngineSoundMathTest {

    @Test
    void alMinimoIlTonoEUnoEIlVolumeEBasso() {
        for (VehicleDef def : VehicleDef.ALL) {
            Powertrain pt = def.powertrain();
            assertEquals(1.0, EngineSoundMath.pitch(pt.idleRpm(), pt), 1e-9);
            assertTrue(EngineSoundMath.volume(pt.idleRpm(), pt) < 0.7);
        }
    }

    @Test
    void alLimitatoreIlTonoNonSuperaIlMassimoDelGioco() {
        for (VehicleDef def : VehicleDef.ALL) {
            Powertrain pt = def.powertrain();
            assertEquals(EngineSoundMath.MAX_PITCH, EngineSoundMath.pitch(pt.redlineRpm(), pt), 1e-9);
            assertEquals(EngineSoundMath.MAX_PITCH, EngineSoundMath.pitch(pt.redlineRpm() * 3, pt), 1e-9);
            assertEquals(1.0, EngineSoundMath.volume(pt.redlineRpm(), pt), 1e-9);
        }
    }

    @Test
    void tonoEVolumeCrescono() {
        Powertrain pt = VehicleDef.CHARGER_BLACK.powertrain();
        double last = 0;
        for (double rpm = pt.idleRpm(); rpm <= pt.redlineRpm(); rpm += 250) {
            double p = EngineSoundMath.pitch(rpm, pt);
            assertTrue(p >= last);
            last = p;
        }
        assertTrue(EngineSoundMath.volume(5000, pt) > EngineSoundMath.volume(1500, pt));
    }

    @Test
    void ilMotoreSiSenteSoloEntroIlRaggio() {
        double r = EngineSoundMath.HEARING_RANGE;
        assertEquals(1.0, EngineSoundMath.distanceGain(0, r), 1e-9);
        assertTrue(EngineSoundMath.distanceGain(r / 2, r) < 0.3);
        assertEquals(0.0, EngineSoundMath.distanceGain(r, r), 1e-9);
        assertEquals(0.0, EngineSoundMath.distanceGain(300, r), 1e-9);
        assertEquals(0.0, EngineSoundMath.distanceGain(Double.NaN, r), 1e-9);
        double last = 1;
        for (double d = 0; d <= r; d += 2) {
            double g = EngineSoundMath.distanceGain(d, r);
            assertTrue(g <= last);
            last = g;
        }
    }

    @Test
    void giriNegativiONonNumericiNonRompono() {
        Powertrain pt = VehicleDef.JEEP.powertrain();
        assertEquals(1.0, EngineSoundMath.pitch(-50, pt), 1e-9);
        assertEquals(1.0, EngineSoundMath.pitch(Double.NaN, pt), 1e-9);
    }
}
