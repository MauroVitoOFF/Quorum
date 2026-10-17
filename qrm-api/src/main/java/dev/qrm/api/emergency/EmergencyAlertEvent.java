package dev.qrm.api.emergency;

import dev.qrm.api.event.Event;
import java.util.UUID;

/**
 * Una richiesta di soccorso partita da un giocatore (per esempio il tasto di allerta del medical). I moduli di
 * dispatch, come la polizia, la trasformano in una chiamata.
 *
 * @param dimension id della dimensione, es. {@code minecraft:overworld}
 */
public record EmergencyAlertEvent(
        UUID reporterId, String reporterName, String dimension, int x, int y, int z, Kind kind, String description)
        implements Event {

    public enum Kind { MEDICAL, POLICE, SHOOTING }
}
