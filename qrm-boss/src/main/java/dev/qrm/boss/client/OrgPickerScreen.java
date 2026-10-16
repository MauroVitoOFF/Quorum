package dev.qrm.boss.client;

import dev.qrm.boss.logic.BossReply;
import dev.qrm.boss.logic.BossRequest;
import dev.qrm.boss.net.BossActionPayload;
import dev.qrm.client.api.screen.QrmScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Selettore dell'azienda, quando il Character ha {@code boss.menu} in più di una. */
final class OrgPickerScreen extends QrmScreen {
    private static final int ROW_H = 20;
    private static final int HEAD_H = 32;
    private final BossReply.OrgList list;
    private int x0, y0, fw, fh;

    OrgPickerScreen(BossReply.OrgList list) {
        super(Component.translatable("qrm_boss.picker.title"));
        this.list = list;
    }

    @Override
    protected void init() {
        fw = Math.min(240, this.width - 20);
        int rows = Math.min(list.orgs().size(), Math.max(1, (this.height - 80) / ROW_H));
        fh = HEAD_H + 14 + rows * ROW_H;
        x0 = (this.width - fw) / 2;
        y0 = (this.height - fh) / 2;
        for (int i = 0; i < rows; i++) {
            var org = list.orgs().get(i);
            addRenderableWidget(new BossRow(x0 + 10, y0 + HEAD_H + 4 + i * ROW_H, fw - 20, ROW_H, org.label(), "", org.id(), null, false,
                    false, () -> ClientPacketDistributor.sendToServer(
                            new BossActionPayload(BossRequest.refresh(org.id(), list.character())))));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        BossStyle.box(g, x0, y0, fw, fh, BossStyle.LINE, BossStyle.BG);
        BossStyle.panel(g, x0 + 10, y0 + 8, fw - 20, 20);
        BossStyle.text(g, this.font, Component.translatable("qrm_boss.picker.title").getString(), x0 + 18,
                y0 + 8 + (20 - BossStyle.TEXT_H) / 2 + 1, BossStyle.color("qrm_boss.title"));
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }
}
