package dev.qrm.boss.client;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Riga di un elenco del menu boss, come quelle dello storico della banca: velo oro se selezionata, velo chiaro al
 * passaggio e una linea sottile in fondo. Sopra il nome, sotto il titolo (se c'è); a destra il rank.
 */
final class BossRow extends AbstractWidget {
    private final String left;
    private final String sub;
    private final String right;
    private final UUID player;
    private final boolean active;
    private final boolean selected;
    private final Runnable onPress;

    /** @param player se c'è, la testa del giocatore a sinistra; {@code active} la tiene a colori */
    BossRow(int x, int y, int w, int h, String left, String sub, String right, UUID player, boolean active,
            boolean selected, Runnable onPress) {
        super(x, y, w, h, Component.literal(left));
        this.left = left;
        this.sub = sub;
        this.right = right;
        this.player = player;
        this.active = active;
        this.selected = selected;
        this.onPress = onPress;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        if (selected) g.fill(x, y, x + w, y + h - 1, 0x55E2B24A);
        else if (isHoveredOrFocused()) g.fill(x, y, x + w, y + h - 1, 0x22FFFFFF);
        g.fill(x, y + h - 1, x + w, y + h, 0x18FFFFFF);

        int ink = BossStyle.color("qrm_boss.ink");
        int soft = BossStyle.color("qrm_boss.ink_soft");
        int rightW = right.isEmpty() ? 0 : Math.min(BossStyle.width(font, right), w / 3);
        if (!right.isEmpty()) {
            BossStyle.right(g, font, BossStyle.fit(font, right, rightW), x + w - 6,
                    y + (h - BossStyle.TEXT_H) / 2, BossStyle.color("qrm_boss.gold"));
        }
        int textX = x + 6;
        if (player != null) {
            BossStyle.face(g, player, active, x + 5, y + (h - 14) / 2, 14);
            textX = x + 26;
        }
        int room = w - rightW - 18 - (textX - x - 6);
        if (sub.isEmpty()) {
            BossStyle.text(g, font, BossStyle.fit(font, left, room), textX, y + (h - BossStyle.TEXT_H) / 2, ink);
        } else {
            BossStyle.text(g, font, BossStyle.fit(font, left, room), textX, y + 2, ink);
            BossStyle.text(g, font, BossStyle.fit(font, sub, room), textX, y + 11, soft);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
