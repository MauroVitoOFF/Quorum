package dev.qrm.api.point;

import dev.qrm.api.id.CharacterId;
import java.util.List;
import java.util.Optional;

/** Punti di interazione. Non controlla chi chiama: l'adattatore di piattaforma impone i permessi staff. */
public interface PointService {
    /**
     * @param minGradeId grado/rank minimo, o null per tutti i membri
     * @param name nome facoltativo (null = nessuno)
     * @param face faccia dell'icona nel mondo, o null per tutte e quattro le laterali
     */
    PointResult<InteractionPoint> create(String typeId, PointOwner owner, PointPosition position, String minGradeId,
            String name, PointFace face);

    /** Come {@link #create(String, PointOwner, PointPosition, String, String, PointFace)} con l'icona su tutte le facce. */
    default PointResult<InteractionPoint> create(String typeId, PointOwner owner, PointPosition position,
            String minGradeId, String name) {
        return create(typeId, owner, position, minGradeId, name, null);
    }

    Optional<InteractionPoint> find(int id);

    Optional<InteractionPoint> findAt(PointPosition position);

    /** Ordinati per id. */
    List<InteractionPoint> all();

    /** Ordinati per id. */
    List<InteractionPoint> ownedBy(PointOwner owner);

    /** @param gradeId null per rimuovere il minimo */
    PointResult<InteractionPoint> setMinGrade(int id, String gradeId);

    PointResult<InteractionPoint> setPublicVisible(int id, boolean publicVisible);

    /** @param name null per rimuovere il nome */
    PointResult<InteractionPoint> setName(int id, String name);

    /** @param face null per mostrare l'icona su tutte e quattro le facce laterali */
    PointResult<InteractionPoint> setFace(int id, PointFace face);

    /** Sposta il punto: id e dati collegati restano. */
    PointResult<InteractionPoint> move(int id, PointPosition position);

    /** @return false se il punto non esiste */
    boolean remove(int id);

    /** Cause di inattività valutabili senza il mondo; vuoto se il Core non trova problemi. */
    Optional<InactiveReason> inactiveReason(InteractionPoint point);

    /**
     * Regola di accesso: personaggio ACTIVE, punto attivo secondo il Core, appartenenza e livello al proprietario,
     * servizio se il tipo lo richiede. Non verifica il mondo né che il personaggio sia quello attivo del giocatore.
     */
    boolean canUse(CharacterId character, InteractionPoint point);
}
