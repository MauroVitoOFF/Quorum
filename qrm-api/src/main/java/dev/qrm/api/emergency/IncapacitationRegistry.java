package dev.qrm.api.emergency;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Registro dello stato "a terra" dei giocatori. Chi gestisce lo stato (per esempio qrm-medical) registra un
 * controllo; chi deve saperlo (per esempio un'arma) interroga il registro senza dipendere da quel modulo.
 */
public interface IncapacitationRegistry {
    /**
     * Registra il controllo di un modulo. Un secondo controllo con lo stesso {@code moduleId} lancia
     * {@link IllegalArgumentException}.
     */
    void register(String moduleId, Predicate<UUID> check);

    /** Vero se almeno uno dei controlli registrati considera a terra il giocatore. */
    boolean isIncapacitated(UUID playerId);
}
