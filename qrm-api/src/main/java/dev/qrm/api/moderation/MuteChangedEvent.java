package dev.qrm.api.moderation;

import dev.qrm.api.event.Event;
import java.util.UUID;

/**
 * Pubblicato quando il mute di un giocatore cambia (nuovo mute, sostituzione o rimozione). Serve a chi applica il mute (QRM
 * aggiorna così la sua cache dei giocatori online) e a chi vuole reagire. Un mute che scade da solo non lo pubblica.
 */
public record MuteChangedEvent(UUID player) implements Event {}
