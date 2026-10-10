package dev.qrm.neoforge.client;

import dev.qrm.neoforge.net.CharacterRosterPayload;
import dev.qrm.neoforge.net.PlayerStatus;
import dev.qrm.neoforge.net.PointEntry;

/** Stato client minimo: se il server parla QRM e l'ultimo roster ricevuto. */
public final class ClientState {
    private static volatile boolean serverHasQrm;
    private static volatile CharacterRosterPayload roster;
    private static volatile PlayerStatus status = PlayerStatus.EMPTY;
    private static volatile java.util.List<PointEntry> points = java.util.List.of();

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

    public static java.util.List<PointEntry> points() {
        return points;
    }

    public static void onPoints(java.util.List<PointEntry> newPoints) {
        points = newPoints;
    }

    public static boolean hasActiveCharacter() {
        CharacterRosterPayload r = roster;
        return r != null && r.activeId().isPresent();
    }

    public static void reset() {
        serverHasQrm = false;
        roster = null;
        status = PlayerStatus.EMPTY;
        points = java.util.List.of();
        PointWorldRenderer.clearCache();
        ClientStaffAccess.reset();
        PanelData.reset();
        StaffTarget.reset();
    }
}
