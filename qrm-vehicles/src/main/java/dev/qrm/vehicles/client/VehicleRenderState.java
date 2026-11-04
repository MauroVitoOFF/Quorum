package dev.qrm.vehicles.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Ciò che serve a disegnare un veicolo in un fotogramma, già interpolato. */
public final class VehicleRenderState extends EntityRenderState {
    /** Imbardata del muso in gradi (come l'entità: 0 guarda verso +Z). */
    public float yaw;
    public float wheelDeg;
    public float steerDeg;
    public float pitchDeg;
    public float rollDeg;
    public boolean lightsOn;
}
