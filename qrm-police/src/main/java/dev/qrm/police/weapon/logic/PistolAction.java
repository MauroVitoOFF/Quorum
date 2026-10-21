package dev.qrm.police.weapon.logic;

import java.util.List;
import java.util.Map;

/**
 * Azioni temporizzate della pistola: clip, durata in tick e istanti di commit.
 * I valori delle clip già esportate coincidono con {@code police_pistol.json}
 * (verificato da {@code PistolTimingsTest}); gli altri sono provvisori fino alla Parte F.
 */
public enum PistolAction {
    DRAW("draw", 16, true),
    RELOAD_TACTICAL("reload_tactical", 50, true,
            Map.entry(16, CommitPoint.MAG_OUT), Map.entry(32, CommitPoint.MAG_IN)),
    RELOAD_EMPTY("reload_empty", 55, true,
            Map.entry(16, CommitPoint.MAG_OUT), Map.entry(32, CommitPoint.MAG_IN), Map.entry(42, CommitPoint.SLIDE)),
    // Provvisori: clip non ancora esportate.
    SLIDE_RELEASE("slide_release", 8, true, Map.entry(4, CommitPoint.SLIDE)),
    SLIDE_RACK("slide_rack", 10, true, Map.entry(6, CommitPoint.SLIDE)),
    AMMO_CHECK("ammo_check", 20, false, Map.entry(12, CommitPoint.CHECK)),
    INSPECT("inspect", 84, false),
    INSPECT_MAG("inspect_mag", 20, false, Map.entry(12, CommitPoint.INSPECT_MAG));

    private final String clip;
    private final int durationTicks;
    private final boolean blocksFire;
    private final List<Map.Entry<Integer, CommitPoint>> commits;

    @SafeVarargs
    PistolAction(String clip, int durationTicks, boolean blocksFire, Map.Entry<Integer, CommitPoint>... commits) {
        this.clip = clip;
        this.durationTicks = durationTicks;
        this.blocksFire = blocksFire;
        this.commits = List.of(commits);
    }

    /** Nome della clip nel file di animazione. */
    public String clip() {
        return clip;
    }

    public int durationTicks() {
        return durationTicks;
    }

    /** Coppie (tick, punto di commit) in ordine crescente di tick. */
    public List<Map.Entry<Integer, CommitPoint>> commits() {
        return commits;
    }

    /** Vero se durante l'azione non si può sparare (estrazione, ricariche, carrello). */
    public boolean blocksFire() {
        return blocksFire;
    }
}
