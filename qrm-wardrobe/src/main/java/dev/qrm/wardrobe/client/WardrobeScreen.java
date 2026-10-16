package dev.qrm.wardrobe.client;

import dev.qrm.client.api.screen.QrmScreen;
import dev.qrm.client.api.theme.ThemeRenderer;
import dev.qrm.client.api.widget.ThemedButton;
import dev.qrm.wardrobe.logic.WardrobeWire;
import dev.qrm.wardrobe.net.WardrobePayloads;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** L'armadietto: la lista delle divise a sinistra, il manichino grigio con la divisa evidenziata a destra. */
final class WardrobeScreen extends QrmScreen {
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 190;
    private static final int ROW_H = 20;
    private static final int LIST_W = 130;
    private static final int VISIBLE_ROWS = 6;

    private record Choice(String id, Component label) {}

    private final List<Choice> choices = new ArrayList<>();
    private String current;
    private int selected;
    private int scroll;

    WardrobeScreen(WardrobeWire.Open open) {
        super(Component.translatable("qrm_wardrobe.screen.title"));
        load(open);
    }

    void refresh(WardrobeWire.Open open) {
        load(open);
        rebuildWidgets();
    }

    private void load(WardrobeWire.Open open) {
        String previouslySelected = choices.isEmpty() ? open.current() : choices.get(selected).id();
        choices.clear();
        choices.add(new Choice("", Component.translatable("qrm_wardrobe.civil")));
        for (String id : open.outfitIds()) {
            ClientOutfits.entry(id).ifPresent(e -> choices.add(new Choice(id, Component.literal(e.label()))));
        }
        current = open.current();
        selected = 0;
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).id().equals(previouslySelected)) selected = i;
        }
        scroll = Math.max(0, Math.min(scroll, choices.size() - VISIBLE_ROWS));
    }

    private int left() {
        return (this.width - PANEL_W) / 2;
    }

    private int top() {
        return (this.height - PANEL_H) / 2;
    }

    @Override
    protected void init() {
        int x = left() + 12;
        int y = top() + 30;
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = scroll + i;
            if (index >= choices.size()) break;
            Choice c = choices.get(index);
            Component label = c.id().equals(current) ? Component.literal("• ").append(c.label()) : c.label();
            ThemedButton.Style style = index == selected ? ThemedButton.Style.CONFIRM : ThemedButton.Style.DEFAULT;
            addRenderableWidget(new ThemedButton(x, y + i * ROW_H, LIST_W, ROW_H - 2, label, () -> {
                selected = index;
                rebuildWidgets();
            }, style));
        }
        ThemedButton confirm = addRenderableWidget(new ThemedButton(x, top() + PANEL_H - 30, LIST_W, 20,
                Component.translatable("qrm_wardrobe.confirm"), () -> ClientPacketDistributor.sendToServer(
                        new WardrobePayloads.Choose(WardrobeWire.encodeChoose(choices.get(selected).id())))));
        confirm.active = !choices.get(selected).id().equals(current);
        addRenderableWidget(new ThemedButton(left() + PANEL_W - 12 - 90, top() + PANEL_H - 30, 90, 20,
                Component.translatable("qrm_wardrobe.close"), this::onClose));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, choices.size() - VISIBLE_ROWS);
        int next = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        g.centeredText(this.font, this.title, this.width / 2, top() + 10, ThemeRenderer.color("text"));
        int bx = left() + 12 + LIST_W + 18;
        int by = top() + 30;
        int bw = PANEL_W - 12 - (12 + LIST_W + 18);
        int bh = PANEL_H - 30 - 40;
        MannequinSkin.playerFor(choices.get(selected).id()).ifPresent(dummy ->
                InventoryScreen.extractEntityInInventoryFollowsMouse(g, bx, by, bx + bw, by + bh, 45, 0.0625f,
                        bx + bw / 2f, by + bh / 3f, dummy));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        ThemeRenderer.draw(g, "panel", left(), top(), PANEL_W, PANEL_H);
    }

    @Override
    public void removed() {
        MannequinSkin.clear();
    }
}
