package dev.qrm.police.weapon.client;

import dev.qrm.police.weapon.logic.anim.AnimEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Smistatore di fine tick del client (R28): svuota gli eventi di {@link WeaponAnimator}, manda i {@code camera_kick}
 * a {@link PistolCamera} e passa ogni evento agli ascoltatori di {@link PistolClientEvents}. Va registrato dopo
 * {@link WeaponAnimator#onClientTick}, così vede gli eventi del tick appena avanzato.
 */
public final class PistolClientTick {
    private PistolClientTick() {}

    /** Da registrare sul bus di NeoForge, lato client. */
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        WeaponAnimator animator = WeaponAnimator.local();
        PistolCamera.tick();
        for (AnimEvent e : animator.drainEvents()) {
            if ("camera_kick".equals(e.type())) kick(e);
            PistolClientEvents.fire(e);
        }
    }

    private static void kick(AnimEvent e) {
        float pitch = arg(e, 0);
        float yaw = arg(e, 1);
        PistolCamera.kick(pitch, yaw);
    }

    private static float arg(AnimEvent e, int i) {
        if (i >= e.args().size()) return 0f;
        try {
            return Float.parseFloat(e.args().get(i));
        } catch (NumberFormatException ex) {
            return 0f;
        }
    }
}
