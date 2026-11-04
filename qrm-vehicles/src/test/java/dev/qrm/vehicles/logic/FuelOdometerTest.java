package dev.qrm.vehicles.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FuelOdometerTest {

    private static final double EPS = 1e-9;

    @Test
    void consumoProporzionaleAllaStrada() {
        FuelTank tank = new FuelTank(60, 60);
        tank.consume(500, 0.08);
        assertEquals(59.6, tank.level(), EPS);
    }

    @Test
    void nonScendeSottoZero() {
        FuelTank tank = new FuelTank(60, 0.1);
        tank.consume(100000, 0.08);
        assertEquals(0, tank.level(), EPS);
        assertTrue(tank.isEmpty());
    }

    @Test
    void aggiungiRispettaLaCapacita() {
        FuelTank tank = new FuelTank(60, 55);
        assertEquals(5, tank.add(10), EPS);
        assertEquals(60, tank.level(), EPS);
        assertEquals(0, tank.room(), EPS);
        assertEquals(0, tank.add(10), EPS);
    }

    @Test
    void ilLivelloInizialeELimitato() {
        assertEquals(60, new FuelTank(60, 100).level(), EPS);
        assertEquals(0, new FuelTank(60, -5).level(), EPS);
    }

    @Test
    void contachilometriAccumulaESiConvertePerKm() {
        Odometer odo = new Odometer(0);
        odo.add(1000);
        odo.add(1500);
        assertEquals(2500, odo.blocks(), EPS);
        assertEquals(2.5, odo.km(), EPS);
        assertEquals(1234.5, Odometer.fromKm(1234.5).km(), 1e-6);
    }

    @Test
    void valoriNegativiONanSonoIgnorati() {
        FuelTank tank = new FuelTank(60, 30);
        tank.consume(-100, 0.08);
        tank.consume(Double.NaN, 0.08);
        tank.consume(100, Double.NaN);
        assertEquals(30, tank.level(), EPS);
        assertEquals(0, tank.add(-5), EPS);
        assertEquals(0, tank.add(Double.NaN), EPS);
        assertEquals(30, tank.level(), EPS);
        Odometer odo = new Odometer(10);
        odo.add(-5);
        odo.add(Double.NaN);
        assertEquals(10, odo.blocks(), EPS);
        assertFalse(Double.isNaN(new Odometer(Double.NaN).blocks()));
    }
}
