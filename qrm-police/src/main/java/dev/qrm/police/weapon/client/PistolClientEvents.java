package dev.qrm.police.weapon.client;

import dev.qrm.police.weapon.logic.anim.AnimEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Registro minimo degli ascoltatori degli eventi delle clip del giocatore locale (lampo, bossolo, commit,
 * contraccolpo...). {@link PistolClientTick} passa a tutti ogni evento svuotato da {@link PistolAnimator}, nell'ordine
 * in cui è stato emesso. I suoni non passano di qui: li suona già l'animatore (R26).
 */
public final class PistolClientEvents {
    private static final List<Consumer<AnimEvent>> LISTENERS = new CopyOnWriteArrayList<>();

    private PistolClientEvents() {}

    public static void addListener(Consumer<AnimEvent> listener) {
        LISTENERS.add(listener);
    }

    static void fire(AnimEvent event) {
        for (Consumer<AnimEvent> l : LISTENERS) l.accept(event);
    }
}
