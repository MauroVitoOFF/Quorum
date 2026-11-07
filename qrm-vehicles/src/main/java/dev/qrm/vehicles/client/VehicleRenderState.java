package dev.qrm.vehicles.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Ciò che serve a disegnare un veicolo in un fotogramma, già interpolato. */
public final class VehicleRenderState extends EntityRenderState {
    /** Imbardata del muso in gradi (come l'entità: 0 guarda verso +Z). */
    public float yaw;
    public float wheelDeg;
    public float steerDeg;
    public float pitchDeg;
    public float heavePx;
    public float rollDeg;
    public boolean lightsOn;
    /** Frazioni di apertura 0..1 di portiere, cofano, freno a mano e interruttore dei fari ({@code VehicleParts}). */
    public float[] parts = new float[dev.qrm.vehicles.logic.VehicleParts.CHANNELS];
    /** Il giocatore locale è dentro in prima persona: i vetri non si disegnano (a meno che l'opzione li voglia). */
    public boolean hideGlass;
    /** Id del modello di veicolo ({@code VehicleDef#id()}): sceglie geo e texture. */
    public String vehicleId = "";
}
