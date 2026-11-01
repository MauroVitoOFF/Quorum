package dev.qrm.police.weapon.logic;

import dev.qrm.police.weapon.logic.anim.AnimClip;
import dev.qrm.police.weapon.logic.anim.AnimEvent;
import dev.qrm.police.weapon.logic.anim.RefillSequence;

/**
 * Regole e tempi del riempimento a mano di un caricatore con proiettili singoli (logica pura). La capienza vera è quella della
 * definizione dell'arma ({@code magazine.capacity} in {@code weapons/<arma>.json}): un caricatore con una capienza
 * maggiore salvata nei dati non può comunque superarla.
 */
public final class MagazineLoader {
    private MagazineLoader() {}

    /** Tick dopo il clic al primo colpo inserito: la preparazione e poi il "clack" del primo giro. */
    public static int firstInsertTicks() {
        WeaponDef d = magazineDef();
        return ticks(d.clips().get(RefillSequence.IN)) + clackTick(d.clips().get(RefillSequence.LOOP));
    }

    /** Tick tra un colpo e il successivo: un giro del ciclo. */
    public static int insertIntervalTicks() {
        return ticks(magazineDef().clips().get(RefillSequence.LOOP));
    }

    private static WeaponDef magazineDef() {
        return WeaponDef.byId("police_magazine").orElseThrow(() -> new IllegalStateException("manca police_magazine"));
    }

    private static int ticks(AnimClip clip) {
        return (int) Math.ceil(clip.length() * 20 - 1e-6);
    }

    /** Tick, dall'inizio del giro, del suono di inserimento del colpo. */
    private static int clackTick(AnimClip loop) {
        for (AnimEvent e : loop.events()) {
            if ("sound".equals(e.type()) && e.args().contains("pistol.bullet_load")) {
                return (int) Math.round(e.t() * 20);
            }
        }
        return 0;
    }

    /**
     * Quanti proiettili spostare dallo stack al caricatore.
     *
     * @param rounds colpi già nel caricatore
     * @param capacity capienza salvata nel caricatore
     * @param weaponCapacity capienza massima della definizione dell'arma
     * @param available proiettili nello stack in mano
     * @param single vero con il clic destro: se ne sposta uno solo
     * @return un numero tra 0 e il minimo di spazio libero e proiettili disponibili
     */
    public static int loadable(int rounds, int capacity, int weaponCapacity, int available, boolean single) {
        int space = Math.max(0, Math.min(capacity, weaponCapacity) - Math.max(0, rounds));
        int wanted = single ? 1 : available;
        return Math.max(0, Math.min(wanted, Math.min(space, available)));
    }
}
