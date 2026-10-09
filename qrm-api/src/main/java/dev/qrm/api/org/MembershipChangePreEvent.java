package dev.qrm.api.org;

import dev.qrm.api.event.CancellableEvent;
import dev.qrm.api.id.CharacterId;

/** Prima di aggiungere, cambiare rank o rimuovere un membro. Gli handler possono solo annullare. */
public final class MembershipChangePreEvent extends CancellableEvent {
    private final String orgId;
    private final CharacterId character;
    private final String from;
    private final String to;

    /** @param from rank attuale (null se non è membro); {@code to} null = uscita */
    public MembershipChangePreEvent(String orgId, CharacterId character, String from, String to) {
        this.orgId = orgId;
        this.character = character;
        this.from = from;
        this.to = to;
    }

    public String orgId() { return orgId; }

    public CharacterId character() { return character; }

    public String from() { return from; }

    public String to() { return to; }
}
