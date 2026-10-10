package dev.qrm.neoforge.client;

import dev.qrm.neoforge.client.screen.CharacterScreen;
import dev.qrm.neoforge.client.screen.HubScreen;
import dev.qrm.neoforge.net.CharacterResultPayload;
import dev.qrm.neoforge.net.CharacterRosterPayload;
import dev.qrm.neoforge.net.PanelSnapshotPayload;
import dev.qrm.neoforge.net.PlayerStatusPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;

/** Ponte dai pacchetti S→C alla UI; caricato solo quando un pacchetto arriva (mai sul server dedicato). */
public final class ClientBridge {
    private ClientBridge() {}

    public static void onRoster(CharacterRosterPayload payload) {
        ClientState.onRoster(payload);
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof CharacterScreen screen) {
            screen.onRoster(payload);
        } else if (payload.openScreen() && (mc.gui.screen() == null || mc.gui.screen() instanceof LevelLoadingScreen)) {
            // mai sostituire un'altra schermata aperta (es. un contenitore): il menu andrebbe in desync
            mc.setScreenAndShow(new CharacterScreen(payload));
        }
    }

    public static void onStatus(PlayerStatusPayload payload) {
        ClientState.onStatus(payload.status());
        if (Minecraft.getInstance().gui.screen() instanceof HubScreen hub) hub.onStatus();
    }

    public static void onPoints(dev.qrm.neoforge.net.PointsPayload payload) {
        ClientState.onPoints(payload.points());
    }

    public static void onPanelSnapshot(PanelSnapshotPayload payload) {
        PanelData.set(payload.snapshot());
    }

    public static void onPage(dev.qrm.neoforge.net.PagePayload payload) {
        PageClient.onPage(payload);
    }

    public static void onPlayerActionResult(dev.qrm.neoforge.net.PlayerActionResultPayload payload) {
        PlayerActionClient.onResult(payload);
    }

    public static void onStaffAccess(dev.qrm.neoforge.net.StaffAccessPayload payload) {
        ClientStaffAccess.set(payload.all(), payload.patterns());
    }

    public static void onResult(CharacterResultPayload payload) {
        if (Minecraft.getInstance().gui.screen() instanceof CharacterScreen screen) screen.onResult(payload);
    }
}
