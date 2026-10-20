package dev.qrm.police.weapon.logic;

import java.util.Optional;

/** Regole pure della pistola: sparo, carrello, cambio caricatore e suggerimenti. Nessuno stato statico. */
public final class PistolRules {
    private PistolRules() {}

    private static boolean magHasRounds(WeaponState s) {
        return s.magazine().map(m -> m.rounds() > 0).orElse(false);
    }

    /**
     * Spara se c'è un colpo in canna. Il colpo successivo viene caricato dal caricatore; se è vuoto il carrello
     * resta bloccato indietro. Senza caricatore il carrello resta chiuso e la camera vuota.
     */
    public static FireResult fire(WeaponState s) {
        if (!s.chambered()) {
            return new FireResult(false, s);
        }
        if (magHasRounds(s)) {
            MagazineState m = s.magazine().get();
            return new FireResult(true,
                    new WeaponState(s.id(), Optional.of(m.withRounds(m.rounds() - 1)), true, s.slideLocked()));
        }
        boolean lock = s.magazine().isPresent();
        return new FireResult(true, new WeaponState(s.id(), s.magazine(), false, lock));
    }

    /** Azione del carrello disponibile nello stato dato. */
    public static SlideAction slideActionFor(WeaponState s) {
        if (s.chambered()) {
            return SlideAction.NONE;
        }
        if (s.slideLocked()) {
            return SlideAction.RELEASE;
        }
        return magHasRounds(s) ? SlideAction.RACK : SlideAction.NONE;
    }

    /** Applica l'azione del carrello; se non ce n'è una lo stato resta invariato. */
    public static WeaponState applySlide(WeaponState s) {
        if (slideActionFor(s) == SlideAction.NONE) {
            return s;
        }
        if (magHasRounds(s)) {
            MagazineState m = s.magazine().get();
            return new WeaponState(s.id(), Optional.of(m.withRounds(m.rounds() - 1)), true, false);
        }
        return new WeaponState(s.id(), s.magazine(), false, false);
    }

    /** Suggerimento sul prossimo gesto; {@code hasLoadedSpare} indica un caricatore di scorta con colpi. */
    public static Hint hint(WeaponState s, boolean hasLoadedSpare) {
        if (s.chambered()) {
            return Hint.NONE;
        }
        if (magHasRounds(s)) {
            return s.slideLocked() ? Hint.RELEASE_SLIDE : Hint.RACK_SLIDE;
        }
        return hasLoadedSpare ? Hint.RELOAD : Hint.NO_SPARE;
    }

    /** Toglie il caricatore (se c'è) senza toccare camera e carrello. */
    public static WeaponState magOut(WeaponState s) {
        if (s.magazine().isEmpty()) {
            return s;
        }
        return new WeaponState(s.id(), Optional.empty(), s.chambered(), s.slideLocked());
    }

    /** Inserisce un caricatore senza toccare camera e carrello; errore se ce n'è già uno. */
    public static WeaponState magIn(WeaponState s, MagazineState m) {
        if (s.magazine().isPresent()) {
            throw new IllegalStateException("magazine already inserted");
        }
        return new WeaponState(s.id(), Optional.of(m), s.chambered(), s.slideLocked());
    }
}
