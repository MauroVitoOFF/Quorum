package dev.qrm.police.logic;

import java.util.ArrayList;
import java.util.List;

/** Una chiamata d'emergenza: dati iniziali, stato, un solo agente incaricato e cronologia. */
public final class DispatchCall {
    public final String id;
    public final CallType type;
    public final String description;
    public final String dimension;
    public final int x, y, z;
    public final long time;
    public final String caller;
    /** L'id del giocatore che ha chiamato, se noto. */
    public String callerId = "";
    public int priority;
    public StatoChiamata state = StatoChiamata.PENDING;
    public String assignee = "";
    public String assigneeNo = "";
    public String assigneeId = "";
    public long assignedAt;
    /** La chiamata da cui nasce una richiesta di supporto, se l'agente ne aveva una. */
    public String parent = "";
    public String outcome = "";
    public long closedAt;
    public final List<EventoChiamata> events = new ArrayList<>();

    public DispatchCall(String id, CallType type, String description, String dimension, int x, int y, int z, long time,
            String caller) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.time = time;
        this.caller = caller;
        this.priority = type.defaultPriority();
    }

    public boolean active() {
        return !state.terminal();
    }

    public boolean assigned() {
        return !assignee.isEmpty();
    }
}
