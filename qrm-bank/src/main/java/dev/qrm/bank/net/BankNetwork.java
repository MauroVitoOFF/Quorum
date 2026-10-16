package dev.qrm.bank.net;

import dev.qrm.bank.client.BankClientBridge;
import dev.qrm.bank.server.AtmActions;
import dev.qrm.bank.server.StaffBankServer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Canale della banca, con versione di protocollo propria (indipendente da quella di QRM). */
public final class BankNetwork {
    public static final String PROTOCOL_VERSION = "3";

    private BankNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        // La lambda carica BankClientBridge solo quando il pacchetto arriva davvero: mai sul server dedicato.
        registrar.playToClient(AtmStatePayload.TYPE, AtmStatePayload.CODEC,
                (payload, context) -> BankClientBridge.onState(payload));
        registrar.playToClient(StaffBankStatePayload.TYPE, StaffBankStatePayload.CODEC,
                (payload, context) -> BankClientBridge.onStaffState(payload));
        registrar.playToServer(StaffBankActionPayload.TYPE, StaffBankActionPayload.CODEC, StaffBankServer::onAction);
        registrar.playToServer(WithdrawPayload.TYPE, WithdrawPayload.CODEC, AtmActions::onWithdraw);
        registrar.playToServer(DepositPayload.TYPE, DepositPayload.CODEC, AtmActions::onDeposit);
        registrar.playToServer(TransferPayload.TYPE, TransferPayload.CODEC, AtmActions::onTransfer);
        registrar.playToServer(SelectAccountPayload.TYPE, SelectAccountPayload.CODEC, AtmActions::onSelectAccount);
        registrar.playToServer(HistoryRequestPayload.TYPE, HistoryRequestPayload.CODEC, AtmActions::onHistory);
    }
}
