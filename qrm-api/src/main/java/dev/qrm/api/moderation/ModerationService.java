package dev.qrm.api.moderation;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Strumenti di moderazione persistenti, legati all'<b>account</b> del giocatore (UUID): mute, avvertimenti e note dello staff.
 * Avvertimenti e note non si modificano né si cancellano. Tutti i limiti di lista sono portati a 1..200.
 */
public interface ModerationService {
    /**
     * Silenzia il giocatore, sostituendo un eventuale mute precedente.
     *
     * @param duration vuota = permanente
     */
    void mute(UUID player, String playerName, Optional<UUID> staffId, String staffName, String reason,
            Optional<Duration> duration);

    /** @return true se il giocatore era silenziato (mute attivo) */
    boolean unmute(UUID player);

    /** Il mute attivo del giocatore; un mute scaduto non c'è più. */
    Optional<Mute> activeMute(UUID player);

    /** I mute attivi (anche di giocatori offline), dal più recente. */
    List<Mute> activeMutes(int limit);

    /** Registra un avvertimento e restituisce il numero di avvertimenti del giocatore dopo questo. */
    int warn(UUID player, String playerName, Optional<UUID> staffId, String staffName, String reason);

    /** Gli avvertimenti del giocatore, dal più recente. */
    List<Warning> warningsOf(UUID player, int limit);

    int warningCount(UUID player);

    /** Gli ultimi avvertimenti di tutti i giocatori, dal più recente. */
    List<Warning> recentWarnings(int limit);

    void addNote(UUID player, String playerName, Optional<UUID> staffId, String staffName, String text);

    /** Le note sul giocatore, dalla più recente. */
    List<StaffNote> notesOf(UUID player, int limit);

    int noteCount(UUID player);
}
