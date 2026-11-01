package dev.qrm.police.weapon.client;

import dev.qrm.police.weapon.logic.anim.AnimEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registro minimo degli ascoltatori degli eventi delle clip del giocatore locale (lampo, bossolo, commit,
 * contraccolpo...). {@link PistolClientTick} passa a tutti ogni evento svuotato da {@link WeaponAnimator}, nell'ordine
 * in cui è stato emesso. I suoni non passano di qui: li suona già l'animatore (R26).
 *
 * <p>Un ascoltatore che lancia un'eccezione non ferma gli altri: l'errore si scrive nel log una volta sola.
 */
public final class PistolClientEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("qrm_police");
    private static final List<Consumer<AnimEvent>> LISTENERS = new CopyOnWriteArrayList<>();
    private static boolean logged;

    private PistolClientEvents() {}

    public static void addListener(Consumer<AnimEvent> listener) {
        LISTENERS.add(listener);
    }

    static void fire(AnimEvent event) {
        for (Consumer<AnimEvent> l : LISTENERS) {
            try {
                l.accept(event);
            } catch (RuntimeException e) {
                if (!logged) {
                    logged = true;
                    LOGGER.error("Ascoltatore degli eventi della pistola fallito su {} (errori successivi non scritti)",
                            event, e);
                }
            }
        }
    }
}
