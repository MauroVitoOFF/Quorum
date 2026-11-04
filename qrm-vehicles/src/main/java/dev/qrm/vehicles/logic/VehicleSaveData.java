package dev.qrm.vehicles.logic;

import java.util.OptionalDouble;

/** Ciò che si salva di un veicolo: carburante e strada percorsa, già corretti rispetto ai limiti del modello. */
public record VehicleSaveData(double fuel, double odometerBlocks) {

    /**
     * Costruisce i dati da quanto letto dal salvataggio: un valore mancante, non numerico o fuori limite non rompe
     * nulla (serbatoio pieno e zero chilometri per i dati assenti).
     */
    public static VehicleSaveData of(VehicleDef def, OptionalDouble fuel, OptionalDouble blocks) {
        double f = fuel.isPresent() && !Double.isNaN(fuel.getAsDouble())
                ? Math.max(0, Math.min(def.fuelCapacity(), fuel.getAsDouble())) : def.fuelCapacity();
        double b = blocks.isPresent() && !Double.isNaN(blocks.getAsDouble()) ? Math.max(0, blocks.getAsDouble()) : 0;
        return new VehicleSaveData(f, b);
    }
}
