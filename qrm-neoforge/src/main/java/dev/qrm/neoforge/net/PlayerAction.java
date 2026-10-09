package dev.qrm.neoforge.net;

import dev.qrm.api.staff.StaffNodes;
import java.util.Locale;

/** Azioni della scheda giocatore del pannello staff. Il nodo è {@code staff.player.<id>}. */
public enum PlayerAction {
    TELEPORT_TO,
    BRING,
    FREEZE,
    HEAL,
    GIVE_ITEM,
    SET_GAMEMODE,
    KICK,
    BAN,
    VIEW_CHARACTER,
    VIEW_PERMISSIONS,
    HISTORY,
    MUTE,
    UNMUTE,
    WARN,
    NOTE,
    WARNINGS,
    NOTES;

    /** Identificatore minuscolo ({@code teleport_to}), lo stesso usato dal registro lato client. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Nodo staff che il server ricontrolla prima di eseguire l'azione. */
    public String node() {
        return this == HISTORY ? StaffNodes.LOGS : StaffNodes.PLAYER_PREFIX + id();
    }

    /** Azioni di sola lettura: non cambiano nulla e quindi non finiscono nel registro staff. */
    public boolean readOnly() {
        return this == VIEW_CHARACTER || this == VIEW_PERMISSIONS || this == HISTORY || this == WARNINGS
                || this == NOTES;
    }

    public static PlayerAction fromOrdinal(int ordinal) {
        PlayerAction[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : VIEW_CHARACTER;
    }
}
