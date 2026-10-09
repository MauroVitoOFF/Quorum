package dev.qrm.neoforge.client;

import dev.qrm.neoforge.net.CharacterRosterPayload;
import dev.qrm.neoforge.net.PlayerStatus;

/** Stato client minimo: se il server parla QRM e l'ultimo roster ricevuto. */
public final class ClientState {
    private static volatile boolean serverHasQrm;
    private static volatile CharacterRosterPayload roster;
    private static volatile PlayerStatus status = PlayerStatus.EMPTY;

    private ClientState() {}

    public static boolean serverHasQrm() {
        return serverHasQrm;
    }

    public static CharacterRosterPayload roster() {
        return roster;
    }

    public static void onRoster(CharacterRosterPayload payload) {
        serverHasQrm = true;
        roster = payload;
    }

    public static PlayerStatus status() {
        return status;
    }

    public static void onStatus(PlayerStatus newStatus) {
        status = newStatus;
    }

    public static boolean hasActiveCharacter() {
        CharacterRosterPayload r = roster;
        return r != null && r.activeId().isPresent();
    }

    public static void reset() {
        serverHasQrm = false;
        roster = null;
        status = PlayerStatus.EMPTY;
        ClientStaffAccess.reset();
        PanelData.reset();
        StaffTarget.reset();
    }
}
