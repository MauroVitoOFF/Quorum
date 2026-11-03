package dev.qrm.api.clock;

/**
 * Il tempo di gioco. Disponibile solo se è installato il Module Clock: senza, {@code QRM.get(ClockService.class)}
 * lancia {@code IllegalStateException}.
 */
public interface ClockService {
    /** Data, ora e stagione di gioco in questo momento. */
    GameTime now();
}
