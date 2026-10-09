package dev.qrm.neoforge.net;

import dev.qrm.neoforge.client.ClientBridge;
import dev.qrm.neoforge.server.GuiServer;
import dev.qrm.neoforge.server.PageServer;
import dev.qrm.neoforge.server.PanelServer;
import dev.qrm.neoforge.server.PlayerActionServer;
import dev.qrm.neoforge.server.StatusSync;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registrazione del canale QRM (opzionale: un client senza il mod non riceve nulla e non viene respinto). */
public final class QrmNetwork {
    public static final String PROTOCOL_VERSION = "11";

    private QrmNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        // Le lambda caricano ClientBridge solo quando il pacchetto arriva davvero: mai sul server dedicato.
        registrar.playToClient(CharacterRosterPayload.TYPE, CharacterRosterPayload.CODEC,
                (payload, context) -> ClientBridge.onRoster(payload));
        registrar.playToClient(CharacterResultPayload.TYPE, CharacterResultPayload.CODEC,
                (payload, context) -> ClientBridge.onResult(payload));
        registrar.playToClient(PlayerStatusPayload.TYPE, PlayerStatusPayload.CODEC,
                (payload, context) -> ClientBridge.onStatus(payload));
        registrar.playToClient(PanelSnapshotPayload.TYPE, PanelSnapshotPayload.CODEC,
                (payload, context) -> ClientBridge.onPanelSnapshot(payload));
        registrar.playToClient(StaffAccessPayload.TYPE, StaffAccessPayload.CODEC,
                (payload, context) -> ClientBridge.onStaffAccess(payload));
        registrar.playToClient(PlayerActionResultPayload.TYPE, PlayerActionResultPayload.CODEC,
                (payload, context) -> ClientBridge.onPlayerActionResult(payload));
        registrar.playToServer(PlayerActionPayload.TYPE, PlayerActionPayload.CODEC, PlayerActionServer::onAction);
        registrar.playToClient(PagePayload.TYPE, PagePayload.CODEC,
                (payload, context) -> ClientBridge.onPage(payload));
        registrar.playToServer(PageRequestPayload.TYPE, PageRequestPayload.CODEC, PageServer::onRequest);
        registrar.playToServer(PanelRequestPayload.TYPE, PanelRequestPayload.CODEC, PanelServer::onRequest);
        registrar.playToServer(RosterRequestPayload.TYPE, RosterRequestPayload.CODEC, GuiServer::onRosterRequest);
        registrar.playToServer(SelectCharacterPayload.TYPE, SelectCharacterPayload.CODEC, GuiServer::onSelect);
        registrar.playToServer(CreateCharacterPayload.TYPE, CreateCharacterPayload.CODEC, GuiServer::onCreate);
        registrar.playToServer(ArchiveCharacterPayload.TYPE, ArchiveCharacterPayload.CODEC, GuiServer::onArchive);
        registrar.playToServer(StatusRequestPayload.TYPE, StatusRequestPayload.CODEC, StatusSync::onRequest);
        registrar.playToServer(SetDutyPayload.TYPE, SetDutyPayload.CODEC, StatusSync::onSetDuty);
    }
}
