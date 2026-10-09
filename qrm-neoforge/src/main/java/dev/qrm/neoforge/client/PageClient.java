package dev.qrm.neoforge.client;

import dev.qrm.client.api.staff.StaffPanelModel;
import dev.qrm.neoforge.net.PageKind;
import dev.qrm.neoforge.net.PagePayload;
import dev.qrm.neoforge.net.PageRequestPayload;
import java.util.List;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Lato client delle pagine di righe del pannello staff (registro azioni, mute attivi, ultimi avvertimenti). */
public final class PageClient {
    private PageClient() {}

    public static void request(PageKind kind) {
        ClientPacketDistributor.sendToServer(new PageRequestPayload(kind));
    }

    public static void onPage(PagePayload payload) {
        List<StaffPanelModel.InfoLine> lines = payload.lines().stream()
                .map(l -> new StaffPanelModel.InfoLine(l.labelKey(), l.value())).toList();
        StaffPanel.showInfo(payload.titleKey(), lines);
    }
}
