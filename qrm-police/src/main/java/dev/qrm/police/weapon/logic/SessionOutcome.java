package dev.qrm.police.weapon.logic;

import java.util.Optional;

/**
 * Esito di un tick di una sessione d'azione.
 *
 * @param status       in corso, conclusa o annullata
 * @param state        stato dell'arma dopo l'ultimo commit applicato
 * @param revealedBand fascia di munizioni rivelata dal commit {@code CHECK}, presente dal tick in cui è stato
 *                     applicato in poi
 */
public record SessionOutcome(Status status, WeaponState state, Optional<AmmoBand> revealedBand) {
    public SessionOutcome {
        revealedBand = revealedBand == null ? Optional.empty() : revealedBand;
    }

    public enum Status {
        RUNNING,
        DONE,
        CANCELLED
    }
}
