package dev.qrm.police.weapon.client;

import dev.qrm.police.weapon.logic.input.KickDecay;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Effetti della pistola sulla visuale del giocatore locale: contraccolpo e FOV in mira.
 *
 * <p>Contraccolpo: l'evento {@code camera_kick <pitch> <yaw>} sposta solo la visuale (in
 * {@link ViewportEvent.ComputeCameraAngles}), mai la rotazione vera del giocatore, quindi la mira non cambia; rientra
 * a zero in {@link KickDecay#RETURN_TICKS} tick. Convenzione: {@code pitch} positivo alza la canna (in Minecraft un
 * pitch minore guarda più in alto), {@code yaw} positivo gira verso destra.
 *
 * <p>FOV: ×{@link #AIM_FOV} in mira. Il passaggio è già graduale perché il FOV vanilla insegue il valore calcolato a
 * metà strada per tick (circa 4 tick, come {@code AIM_TICKS}).
 */
public final class PistolCamera {
    /** Moltiplicatore del FOV in mira (specifica). */
    public static final float AIM_FOV = 0.85f;

    private static final KickDecay KICK = new KickDecay();

    private PistolCamera() {}

    /** Aggiunge un contraccolpo in gradi. */
    public static void kick(float pitch, float yaw) {
        KICK.add(pitch, yaw);
    }

    static void tick() {
        KICK.tick();
    }

    public static void reset() {
        KICK.reset();
    }

    /** Da registrare sul bus di NeoForge, lato client. */
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float pt = (float) event.getPartialTick();
        float p = KICK.pitch(pt);
        float y = KICK.yaw(pt);
        if (p == 0f && y == 0f) return;
        event.setPitch(event.getPitch() - p);
        event.setYaw(event.getYaw() + y);
    }

    /** Da registrare sul bus di NeoForge, lato client. */
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (event.getPlayer() != Minecraft.getInstance().player) return;
        if (!PistolInput.isLocalAiming()) return;
        event.setNewFovModifier(event.getNewFovModifier() * AIM_FOV);
    }
}
