package dev.qrm.boss.client;

import dev.qrm.boss.logic.BossReply;
import net.minecraft.client.Minecraft;

/** Ponte dal canale di rete alla UI: caricato solo quando arriva davvero un pacchetto (mai sul server dedicato). */
public final class BossClientBridge {
    private BossClientBridge() {}

    public static void onState(BossReply reply) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof BossScreen screen
                && (reply.snapshot().isEmpty() || screen.orgId().equals(reply.snapshot().get().orgId()))) {
            screen.onState(reply);
        } else if (reply.snapshot().isPresent()) {
            mc.setScreenAndShow(new BossScreen(reply));
        }
    }

    public static void onOrgList(BossReply.OrgList list) {
        Minecraft.getInstance().setScreenAndShow(new OrgPickerScreen(list));
    }
}
