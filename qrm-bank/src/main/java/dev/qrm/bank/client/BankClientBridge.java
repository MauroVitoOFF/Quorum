package dev.qrm.bank.client;

import dev.qrm.bank.net.AtmStatePayload;
import dev.qrm.bank.net.StaffBankStatePayload;
import net.minecraft.client.Minecraft;

/** Ponte dal canale di rete alla UI: caricata solo quando arriva davvero un pacchetto (mai sul server dedicato). */
public final class BankClientBridge {
    private BankClientBridge() {}

    public static void onStaffState(StaffBankStatePayload payload) {
        if (Minecraft.getInstance().gui.screen() instanceof StaffBankScreen screen) screen.onState(payload);
    }

    public static void onState(AtmStatePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (payload.open()) {
            mc.setScreenAndShow(new AtmScreen(payload));
        } else if (mc.gui.screen() instanceof AtmScreen screen) {
            screen.update(payload);
        }
    }
}
