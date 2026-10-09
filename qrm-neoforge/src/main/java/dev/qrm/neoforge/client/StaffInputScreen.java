package dev.qrm.neoforge.client;

import dev.qrm.client.api.staff.StaffPlayerActions;
import dev.qrm.client.api.widget.TextFit;
import dev.qrm.client.api.widget.ThemedButton;
import dev.qrm.client.api.widget.ThemedField;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Finestrella di input del pannello staff (Give item, Kick, Ban...): cornice dorata, un campo per ogni {@link
 * StaffPlayerActions.Field}, Invio conferma, Esc annulla. Non mette in pausa il gioco. Chiude se stessa prima di chiamare
 * {@code onSubmit}, quindi si torna al gioco con il pannello ancora aperto.
 */
public final class StaffInputScreen extends Screen {
    private static final int WIDTH = 220;
    private static final int FRAME = 12;
    private static final int FIELD_H = 20;
    private static final int BUTTON_H = 20;
    private static final int GAP = 4;

    private final String subject;
    private final List<StaffPlayerActions.Field> fields;
    private final boolean danger;
    private final Consumer<List<String>> onSubmit;
    private final List<ThemedField> boxes = new ArrayList<>();
    private ThemedButton confirm;
    private int x0;
    private int y0;
    private int height;

    public StaffInputScreen(Component title, String subject, List<StaffPlayerActions.Field> fields, boolean danger,
            Consumer<List<String>> onSubmit) {
        super(title);
        this.subject = subject;
        this.fields = List.copyOf(fields);
        this.danger = danger;
        this.onSubmit = onSubmit;
    }

    @Override
    protected void init() {
        boxes.clear();
        int lh = this.font.lineHeight;
        int titleBlock = FRAME + lh + 4 + lh + 6;
        int perField = lh + 2 + FIELD_H + 6;
        height = titleBlock + fields.size() * perField + BUTTON_H + FRAME + 2;
        x0 = (this.width - WIDTH) / 2;
        y0 = Math.max(4, (this.height - height) / 2);
        int cx = x0 + FRAME;
        int cw = WIDTH - 2 * FRAME;
        int y = y0 + titleBlock;
        for (StaffPlayerActions.Field f : fields) {
            ThemedField box = addRenderableWidget(new ThemedField(this.font, cx, y + lh + 2, cw, FIELD_H,
                    Component.translatable(f.labelKey())));
            box.setMaxLength(f.maxLength());
            box.setHint(Component.translatable(f.hintKey()));
            box.setResponder(t -> refresh());
            boxes.add(box);
            y += perField;
        }
        int bw = (cw - GAP) / 2;
        confirm = addRenderableWidget(new ThemedButton(cx, y, bw, BUTTON_H, Component.translatable("qrm.staff.input.confirm"),
                this::submit, danger ? ThemedButton.Style.DANGER : ThemedButton.Style.CONFIRM));
        addRenderableWidget(new ThemedButton(cx + bw + GAP, y, bw, BUTTON_H, Component.translatable("qrm.staff.input.cancel"),
                this::onClose));
        refresh();
    }

    @Override
    protected void setInitialFocus() {
        if (!boxes.isEmpty()) setInitialFocus(boxes.get(0));
    }

    private void refresh() {
        if (confirm == null) return;
        boolean ok = true;
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).required() && boxes.get(i).getValue().isBlank()) ok = false;
        }
        confirm.active = ok;
    }

    private void submit() {
        if (!confirm.active) return;
        List<String> values = new ArrayList<>();
        for (ThemedField box : boxes) values.add(box.getValue().strip());
        onClose();
        onSubmit.accept(values);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Nessuno sfondo: dietro la finestrella si vede il mondo, senza scurimento né fasce. */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // intenzionalmente vuoto
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int lh = this.font.lineHeight;
        int cx = x0 + FRAME;
        int cw = WIDTH - 2 * FRAME;
        StaffPanelSkin.drawFrame(g, x0, y0, WIDTH, height);
        int gold = StaffPanelSkin.gold();
        String head = TextFit.fit(this.font, this.title.getString(), cw - 28);
        int hw = this.font.width(head);
        int hx = cx + (cw - hw) / 2;
        int hy = y0 + FRAME + 1;
        g.text(this.font, head, hx, hy, gold, true);
        StaffPanelSkin.diamond(g, hx - 8, hy + lh / 2, 2, gold);
        StaffPanelSkin.diamond(g, hx + hw + 8, hy + lh / 2, 2, gold);
        g.text(this.font, TextFit.fit(this.font, subject, cw), cx + (cw - Math.min(cw, this.font.width(subject))) / 2,
                hy + lh + 4, StaffPanelSkin.cream(), true);
        int perField = lh + 2 + FIELD_H + 6;
        int y = y0 + FRAME + lh + 4 + lh + 6;
        for (StaffPlayerActions.Field f : fields) {
            String label = Component.translatable(f.labelKey()).getString() + (f.required() ? " *" : "");
            g.text(this.font, label, cx + 2, y, gold, true);
            y += perField;
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }
}
