package dev.qrm.vehicles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** La visuale a bordo del giocatore locale: quando vale e di quanto si sposta. Solo lato client. */
public final class VehicleView {
    private VehicleView() {}

    /** Il giocatore locale guarda dall'interno (è il passeggero e la telecamera è in prima persona). */
    public static boolean isInside(Entity passenger) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == passenger && mc.options.getCameraType().isFirstPerson();
    }

    /**
     * Scostamento della visuale {destra, su, avanti} in unità del modello, o null se non si applica a questo
     * passeggero. Installato in {@code QrmVehicleEntity.VIEW_OFFSET}.
     */
    public static float[] offsetFor(Entity passenger) {
        if (!isInside(passenger)) return null;
        return new float[] {VehicleClientConfig.VIEW_RIGHT.get(), VehicleClientConfig.VIEW_UP.get(),
            VehicleClientConfig.VIEW_FORWARD.get()};
    }
}
