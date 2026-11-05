package dev.qrm.vehicles.logic;

import java.util.List;

/**
 * Posizione dei sedili di ogni modello, nelle unità del modello (16 = 1 blocco) e già nello spazio di gioco. Devono
 * coincidere con i locator del file del modello: lo verificano {@code JeepModelTest} e {@code ChargerModelTest}.
 * L'indice 0 è il guidatore.
 */
public final class VehicleSeats {

    private VehicleSeats() {}

    public static final List<float[]> JEEP = List.of(new float[] {-8f, 8.4f, 3.75f}, new float[] {8f, 8.4f, 3.75f});

    /** Sedili bassi della Charger (cuscino y 9,6): l'anca sta 4,2 unità sotto la superficie perché il tetto è a y 27,8. */
    public static final List<float[]> CHARGER_BLACK = List.of(new float[] {-7.2f, 5.4f, 1.8f}, new float[] {7.2f, 5.4f, 1.8f});

    public static List<float[]> of(VehicleDef def) {
        if (def.id().equals(VehicleDef.CHARGER_BLACK.id())) return CHARGER_BLACK;
        return JEEP;
    }
}
