package dev.qrm.police.weapon.logic.anim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Pianifica, a tick di client, i suoni delle clip di azioni di altre entità (regola R26). Logica pura: non sa nulla
 * di Minecraft. Ogni chiave (l'id dell'entità) ha al massimo una clip attiva; il chiamante avanza con {@link #tick}
 * e suona gli id restituiti alla posizione che l'entità ha in quel momento.
 *
 * <p>Il suono {@code pistol.fire} è escluso: lo trasmette già il server a tutti tranne chi spara.
 */
public final class RemoteSoundScheduler {
    private static final double TICKS_PER_SECOND = 20.0;
    private static final String SKIPPED = "pistol.fire";

    /** Un suono scaduto: a chi appartiene e quale. */
    public record Due(int key, String soundId) {}

    private record Pending(int tick, String soundId) {}

    private static final class Run {
        final int key;
        final List<Pending> pending;
        int elapsed;

        Run(int key, List<Pending> pending) {
            this.key = key;
            this.pending = pending;
        }
    }

    private final List<Run> runs = new ArrayList<>();

    /** Avvia la clip per la chiave, sostituendo quella in corso. Una clip assente non pianifica nulla. */
    public void start(int key, AnimClip clip) {
        cancel(key);
        if (clip == null) return;
        List<Pending> pending = new ArrayList<>();
        for (AnimEvent e : clip.events()) {
            if (!"sound".equals(e.type()) || e.args().isEmpty()) continue;
            String id = e.args().get(0);
            if (SKIPPED.equals(id)) continue;
            pending.add(new Pending((int) Math.round(Math.max(0, e.t()) * TICKS_PER_SECOND), id));
        }
        if (pending.isEmpty()) return;
        pending.sort(Comparator.comparingInt(Pending::tick));
        runs.add(new Run(key, pending));
    }

    /** Ferma la clip della chiave (azione finita o annullata, entità rimossa). */
    public void cancel(int key) {
        runs.removeIf(r -> r.key == key);
    }

    /** Ferma tutto (uscita dal mondo, cambio di livello). */
    public void clear() {
        runs.clear();
    }

    public boolean isActive(int key) {
        for (Run r : runs) if (r.key == key) return true;
        return false;
    }

    /** Avanza di un tick e restituisce i suoni scaduti, in ordine di tempo; le clip esaurite si eliminano. */
    public List<Due> tick() {
        List<Due> due = new ArrayList<>();
        for (Iterator<Run> it = runs.iterator(); it.hasNext(); ) {
            Run run = it.next();
            run.elapsed++;
            while (!run.pending.isEmpty() && run.pending.get(0).tick() <= run.elapsed) {
                due.add(new Due(run.key, run.pending.remove(0).soundId()));
            }
            if (run.pending.isEmpty()) it.remove();
        }
        return due;
    }
}
