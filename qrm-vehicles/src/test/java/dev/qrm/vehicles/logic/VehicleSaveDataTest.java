package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class VehicleSaveDataTest {

    private static final VehicleDef JEEP = VehicleDef.JEEP;
    private static final double EPS = 1e-9;

    @Test
    void iValoriSalvatiTornanoUguali() {
        VehicleSaveData d = VehicleSaveData.of(JEEP, OptionalDouble.of(31.5), OptionalDouble.of(1234.5));
        assertEquals(31.5, d.fuel(), EPS);
        assertEquals(1234.5, d.odometerBlocks(), EPS);
    }

    @Test
    void daiMancantiSiPartePienoEZeroKm() {
        VehicleSaveData d = VehicleSaveData.of(JEEP, OptionalDouble.empty(), OptionalDouble.empty());
        assertEquals(JEEP.fuelCapacity(), d.fuel(), EPS);
        assertEquals(0, d.odometerBlocks(), EPS);
    }

    @Test
    void valoriImpossibiliSonoCorretti() {
        assertEquals(JEEP.fuelCapacity(),
                VehicleSaveData.of(JEEP, OptionalDouble.of(999), OptionalDouble.empty()).fuel(), EPS);
        assertEquals(0, VehicleSaveData.of(JEEP, OptionalDouble.of(-3), OptionalDouble.empty()).fuel(), EPS);
        assertEquals(JEEP.fuelCapacity(),
                VehicleSaveData.of(JEEP, OptionalDouble.of(Double.NaN), OptionalDouble.empty()).fuel(), EPS);
        assertEquals(0, VehicleSaveData.of(JEEP, OptionalDouble.empty(), OptionalDouble.of(-5)).odometerBlocks(), EPS);
        assertEquals(0,
                VehicleSaveData.of(JEEP, OptionalDouble.empty(), OptionalDouble.of(Double.NaN)).odometerBlocks(), EPS);
    }
}
