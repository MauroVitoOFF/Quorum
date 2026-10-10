package dev.qrm.locker.client;

import dev.qrm.client.api.theme.ThemeRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;

/** Lo stesso menu del baule vanilla, con pannello e colori del tema QRM. Gli slot non cambiano. */
final class LockerScreen extends ContainerScreen {
    LockerScreen(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        extractBlurredBackground(g);
        g.fill(0, 0, this.width, this.height, ThemeRenderer.color("worldDim"));
        g.fill(0, 0, this.width, this.height, ThemeRenderer.color("worldFrost"));
        ThemeRenderer.draw(g, "panel", this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        int slotBg = ThemeRenderer.color("qrm_locker.slot");
        for (Slot slot : this.menu.slots) {
            g.fill(this.leftPos + slot.x - 1, this.topPos + slot.y - 1, this.leftPos + slot.x + 17,
                    this.topPos + slot.y + 17, slotBg);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, this.titleLabelX, this.titleLabelY, ThemeRenderer.color("text"));
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                ThemeRenderer.color("muted"));
    }
}
