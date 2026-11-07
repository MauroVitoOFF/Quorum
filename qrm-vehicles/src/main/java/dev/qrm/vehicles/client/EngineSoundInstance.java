package dev.qrm.vehicles.client;

import dev.qrm.vehicles.logic.EngineSoundMath;
import dev.qrm.vehicles.mc.QrmVehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Il motore di un veicolo: un unico suono in ciclo che segue il veicolo. Il tono sale con i giri e il volume con
 * il carico; si ferma da solo quando il motore si spegne o il veicolo sparisce.
 */
final class EngineSoundInstance extends AbstractTickableSoundInstance {
    private final QrmVehicleEntity vehicle;
    private double pitchNow;
    private double volumeNow;

    EngineSoundInstance(QrmVehicleEntity vehicle, SoundEvent event) {
        super(event, SoundSource.NEUTRAL, vehicle.level().getRandom());
        this.vehicle = vehicle;
        this.looping = true;
        this.delay = 0;
        // L'attenuazione la fa hearing(): quella del gioco non agisce sui file stereo.
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
        var pt = vehicle.def().powertrain();
        this.pitchNow = EngineSoundMath.pitch(pt.idleRpm(), pt);
        this.volumeNow = EngineSoundMath.volume(pt.idleRpm(), pt);
        this.pitch = (float) pitchNow;
        this.volume = (float) (volumeNow * hearing());
    }

    @Override
    public void tick() {
        if (vehicle.isRemoved() || !vehicle.engineOn()) {
            stop();
            return;
        }
        x = vehicle.getX();
        y = vehicle.getY();
        z = vehicle.getZ();
        var pt = vehicle.def().powertrain();
        double rpm = Math.max(pt.idleRpm(), vehicle.rpm());
        // I giri arrivano a salti di 100 al tick: si livella per non sentire scalini.
        pitchNow += (EngineSoundMath.pitch(rpm, pt) - pitchNow) * EngineSoundMath.SMOOTHING;
        volumeNow += (EngineSoundMath.volume(rpm, pt) - volumeNow) * EngineSoundMath.SMOOTHING;
        pitch = (float) pitchNow;
        volume = (float) (volumeNow * hearing());
    }

    /** Quanto si sente dal giocatore: il gioco non attenua i suoni stereo, quindi la distanza la calcoliamo noi. */
    private double hearing() {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        return EngineSoundMath.distanceGain(player.distanceTo(vehicle), EngineSoundMath.HEARING_RANGE);
    }
}
