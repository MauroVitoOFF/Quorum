package dev.qrm.police.weapon.logic;

import java.util.Optional;
import java.util.UUID;

/** Stato di una singola arma: caricatore inserito, colpo in canna e otturatore bloccato indietro. */
public record WeaponState(UUID id, Optional<MagazineState> magazine, boolean chambered, boolean slideLocked) {
    public WeaponState {
        magazine = magazine == null ? Optional.empty() : magazine;
    }

    /** Arma scarica: nessun caricatore, nessun colpo in canna. */
    public static WeaponState empty(UUID id) {
        return new WeaponState(id, Optional.empty(), false, false);
    }
}
