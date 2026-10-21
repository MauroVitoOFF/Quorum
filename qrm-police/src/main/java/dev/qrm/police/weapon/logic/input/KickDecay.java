package dev.qrm.police.weapon.logic.input;

/**
 * Spostamento visivo della visuale per il contraccolpo: arriva subito per intero e rientra a zero in modo lineare in
 * {@link #RETURN_TICKS} tick dall'ultimo colpo. Colpi ravvicinati si sommano. Si legge con il {@code partialTick} per
 * un movimento fluido tra i tick. Gradi, senza convenzione di segno: la decide chi lo applica alla visuale.
 */
public final class KickDecay {
    /** Tick in cui il contraccolpo torna a zero (H6). */
    public static final int RETURN_TICKS = 6;

    private float pitch;
    private float yaw;
    private float prevPitch;
    private float prevYaw;
    private int remaining;

    /** Aggiunge un contraccolpo, visibile subito. */
    public void add(float dPitch, float dYaw) {
        pitch += dPitch;
        yaw += dYaw;
        prevPitch = pitch;
        prevYaw = yaw;
        remaining = RETURN_TICKS;
    }

    /** Avanza di un tick. */
    public void tick() {
        prevPitch = pitch;
        prevYaw = yaw;
        if (remaining <= 0) {
            pitch = 0f;
            yaw = 0f;
            return;
        }
        float keep = (remaining - 1) / (float) remaining;
        pitch *= keep;
        yaw *= keep;
        remaining--;
    }

    public float pitch(float partialTick) {
        return prevPitch + (pitch - prevPitch) * partialTick;
    }

    public float yaw(float partialTick) {
        return prevYaw + (yaw - prevYaw) * partialTick;
    }

    public void reset() {
        pitch = yaw = prevPitch = prevYaw = 0f;
        remaining = 0;
    }
}
