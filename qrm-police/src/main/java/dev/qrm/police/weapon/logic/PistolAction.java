package dev.qrm.police.weapon.logic;

import java.util.List;
import java.util.Map;

/**
 * Azioni temporizzate della pistola. Il nome dell'azione è quello della clip; durata in tick, istanti di commit e se
 * blocca lo sparo si leggono da {@link WeaponDef} ({@code weapons/police_pistol.json}), che ricava durata e commit
 * dalla clip stessa: non c'è più una seconda tabella da tenere allineata.
 */
public enum PistolAction {
    DRAW("draw"),
    RELOAD_TACTICAL("reload_tactical"),
    RELOAD_EMPTY("reload_empty"),
    SLIDE_RELEASE("slide_release"),
    SLIDE_RACK("slide_rack"),
    AMMO_CHECK("ammo_check"),
    INSPECT("inspect");

    private final String clip;

    PistolAction(String clip) {
        this.clip = clip;
    }

    /** Nome della clip nel file di animazione (e dell'azione nella definizione dell'arma). */
    public String clip() {
        return clip;
    }

    private WeaponDef.Action def() {
        return WeaponDef.pistol().action(clip)
                .orElseThrow(() -> new IllegalStateException("azione non definita: " + clip));
    }

    public int durationTicks() {
        return def().ticks();
    }

    /** Coppie (tick, punto di commit) in ordine crescente di tick. */
    public List<Map.Entry<Integer, CommitPoint>> commits() {
        return def().commits().stream().map(c -> Map.entry(c.tick(), c.point())).toList();
    }

    /** Vero se durante l'azione non si può sparare (estrazione, ricariche, carrello). */
    public boolean blocksFire() {
        return def().blocksFire();
    }
}
