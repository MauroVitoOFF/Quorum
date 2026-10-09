package dev.qrm.api.job;

import dev.qrm.api.event.CancellableEvent;
import dev.qrm.api.id.CharacterId;

/** Prima di assegnare, cambiare grade o licenziare. Gli handler possono solo annullare. */
public final class JobChangePreEvent extends CancellableEvent {
    private final CharacterId character;
    private final Assignment from;
    private final String toJobId;
    private final String toGradeId;

    /** @param from assegnazione attuale (null se disoccupato); {@code toJobId}/{@code toGradeId} null = licenziamento */
    public JobChangePreEvent(CharacterId character, Assignment from, String toJobId, String toGradeId) {
        this.character = character;
        this.from = from;
        this.toJobId = toJobId;
        this.toGradeId = toGradeId;
    }

    public CharacterId character() { return character; }

    public Assignment from() { return from; }

    public String toJobId() { return toJobId; }

    public String toGradeId() { return toGradeId; }
}
