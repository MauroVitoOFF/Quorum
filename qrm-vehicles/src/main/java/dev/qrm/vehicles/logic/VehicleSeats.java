package dev.qrm.vehicles.logic;

import java.util.List;

/**
 * Posizione dei sedili di ogni modello, nelle unità del modello (16 = 1 blocco) e già nello spazio di gioco. Devono
 * coincidere con i locator del file del modello: lo verifica {@code JeepModelTest}. L'indice 0 è il guidatore.
 */
public final class VehicleSeats {

    private VehicleSeats() {}

    public static final List<float[]> JEEP = List.of(new float[] {-6.5f, 10f, 3f}, new float[] {6.5f, 10f, 3f});
}
