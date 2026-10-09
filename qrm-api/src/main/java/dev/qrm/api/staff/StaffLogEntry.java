package dev.qrm.api.staff;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Una voce del registro delle azioni staff.
 *
 * @param id numero progressivo (cresce a ogni voce)
 * @param staffId chi ha agito; vuoto per la console
 * @param staffName nome al momento dell'azione (i nomi cambiano, la voce no)
 * @param action identificatore dell'azione, es. {@code panel.ban}, {@code bank.take}, {@code cmd.job.set}
 * @param targetId il giocatore su cui si è agito, se c'è
 * @param targetName nome del bersaglio al momento dell'azione, se noto
 * @param detail oggetto e quantità, durata e motivo, importo, testo del comando... (al massimo 256 caratteri)
 */
public record StaffLogEntry(long id, Instant at, Optional<UUID> staffId, String staffName, String action,
        Optional<UUID> targetId, Optional<String> targetName, String detail, Outcome outcome) {
    public enum Outcome { OK, DENIED }
}
