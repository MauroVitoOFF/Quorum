package dev.qrm.boss.client;

import dev.qrm.boss.logic.BossReply;
import dev.qrm.boss.logic.BossRequest;
import dev.qrm.boss.net.BossActionPayload;
import dev.qrm.client.api.screen.QrmScreen;
import dev.qrm.client.api.theme.ThemeRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Selettore dell'azienda, quando il Character ha {@code boss.menu} in più di una. */
final class OrgPickerScreen extends QrmScreen {
    private static final int ROW_H = 18;
    private final BossReply.OrgList list;
    private int x0, y0, fw, fh;

    OrgPickerScreen(BossReply.OrgList list) {
        super(Component.translatable("qrm_boss.picker.title"));
        this.list = list;
    }

    @Override
    protected void init() {
        fw = Math.min(220, this.width - 20);
        int rows = Math.min(list.orgs().size(), Math.max(1, (this.height - 80) / ROW_H));
        fh = 40 + rows * ROW_H;
        x0 = (this.width - fw) / 2;
        y0 = (this.height - fh) / 2;
        for (int i = 0; i < rows; i++) {
            var org = list.orgs().get(i);
            addRenderableWidget(new BossRow(x0 + 12, y0 + 28 + i * ROW_H, fw - 24, ROW_H, org.label(), org.id(), false,
                    () -> ClientPacketDistributor.sendToServer(
                            new BossActionPayload(BossRequest.refresh(org.id(), list.character())))));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        ThemeRenderer.draw(g, "qrm_boss.frame", x0, y0, fw, fh);
        String title = Component.translatable("qrm_boss.picker.title").getString();
        g.text(this.font, title, (this.width - this.font.width(title)) / 2, y0 + 12,
                ThemeRenderer.color("qrm_boss.ink"), false);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }
}
