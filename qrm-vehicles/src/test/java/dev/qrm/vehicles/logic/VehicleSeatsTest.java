package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class VehicleSeatsTest {

    @Test
    void iSedegliSiLeggonoDaiLocatorDelModello() {
        var seats = VehicleSeats.fromLocators(Map.of("driver_hip", new float[] {-1, 2, 3}, "passenger_hip",
                new float[] {4, 5, 6}));
        assertTrue(seats.isPresent());
        assertArrayEquals(new float[] {-1, 2, 3}, seats.get().get(0), 0f);
        assertArrayEquals(new float[] {4, 5, 6}, seats.get().get(1), 0f);
    }

    @Test
    void senzaTuttiEDueILocatorSiUsanoLeCostanti() {
        assertTrue(VehicleSeats.fromLocators(Map.of("driver_hip", new float[] {0, 0, 0})).isEmpty());
    }

    @Test
    void ilModelloVeroDaiMedesimiSediliDelleCostanti() {
        assertEquals(2, VehicleSeats.of(VehicleDef.CHARGER_BLACK).size());
        assertArrayEquals(VehicleSeats.CHARGER_BLACK.get(0), VehicleSeats.of(VehicleDef.CHARGER_BLACK).get(0), 1e-4f);
        assertArrayEquals(VehicleSeats.CHARGER_BLACK.get(1), VehicleSeats.of(VehicleDef.CHARGER_BLACK).get(1), 1e-4f);
        assertArrayEquals(VehicleSeats.JEEP.get(0), VehicleSeats.of(VehicleDef.JEEP).get(0), 1e-4f);
    }
}
