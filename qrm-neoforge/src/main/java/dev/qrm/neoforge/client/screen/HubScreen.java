package dev.qrm.neoforge.client.screen;

import dev.qrm.neoforge.client.ClientState;
import dev.qrm.neoforge.client.KeyBindings;
import dev.qrm.client.api.screen.QrmScreen;
import dev.qrm.client.api.widget.TextFit;
import dev.qrm.client.api.theme.QrmTheme;
import dev.qrm.client.api.theme.ThemeRenderer;
import dev.qrm.client.api.widget.ThemedButton;
import dev.qrm.client.api.widget.ThemedToggle;
import dev.qrm.neoforge.net.PlayerStatus;
import dev.qrm.neoforge.net.SetDutyPayload;
import dev.qrm.neoforge.net.StatusRequestPayload;
import dev.qrm.neoforge.net.UiConfig;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Hub del personaggio, in sola lettura: schede Conti, Lavoro e Organizzazioni. */
public final class HubScreen extends QrmScreen {
    private enum Tab {
        ACCOUNTS(UiConfig.TAB_ACCOUNTS, "qrm.hub.tab.accounts"), JOB(UiConfig.TAB_JOB, "qrm.hub.tab.job"),
        ORGS(UiConfig.TAB_ORGS, "qrm.hub.tab.orgs");

        private final String id;
        private final String key;

        Tab(String id, String key) {
            this.id = id;
            this.key = key;
        }
    }

    /** Schede abilitate dal server, nell'ordine di configurazione; i nomi sconosciuti sono ignorati. */
    private static List<Tab> allowedTabs() {
        List<Tab> out = new ArrayList<>();
        for (String name : ClientState.status().ui().hubTabs()) {
            for (Tab t : Tab.values()) {
                if (t.id.equals(name) && !out.contains(t)) out.add(t);
            }
        }
        return out;
    }

    /** {@code dotColor} 0 = nessun quadratino (i colori del tema sono sempre opachi, quindi mai 0). */
    private record Row(String label, String value, int valueColor, int dotColor) {}

    private static final int VISIBLE_ROWS = 8;
    private static final int TAB_H = 16;
    private static final int TAB_MIN_W = 40;
    private static final int DOT = 5;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Tab tab;
    private int scroll;
    private ThemedButton dutyEnter;
    private ThemedButton dutyLeave;

    private HubScreen() {
        super(Component.translatable("qrm.hub.title"));
        List<Tab> tabs = allowedTabs();
        this.tab = tabs.isEmpty() ? Tab.ACCOUNTS : tabs.get(0);
    }

    /** Apre l'hub con lo stato in cache e ne chiede uno aggiornato; senza personaggio attivo mostra un messaggio. */
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!ClientState.status().hasCharacter()) {
            mc.player.sendOverlayMessage(Component.translatable("qrm.character.reminder",
                    KeyBindings.CHARACTERS.getTranslatedKeyMessage()));
            return;
        }
        if (allowedTabs().isEmpty()) {
            mc.player.sendOverlayMessage(Component.translatable("qrm.hub.unavailable"));
            return;
        }
        ClientPacketDistributor.sendToServer(new StatusRequestPayload());
        mc.setScreenAndShow(new HubScreen());
    }

    /** Chiamato quando arriva uno snapshot: se il personaggio non c'è più la schermata si chiude. */
    public void onStatus() {
        if (!ClientState.status().hasCharacter()) onClose();
    }

    // ---- layout --------------------------------------------------------------------------------------------

    private static int pad() {
        return Math.max(0, (int) Math.round(QrmTheme.current().metric("padding")));
    }

    private int lineH() {
        return this.font.lineHeight;
    }

    private int rowH() {
        return lineH() + 4;
    }

    private int cardW() {
        int wanted = Math.max(1, (int) Math.round(QrmTheme.current().metric("idCardWidth")));
        return Math.max(1, Math.min(wanted, this.width - 2 * pad()));
    }

    private int cardH() {
        return pad() + VISIBLE_ROWS * rowH() + lineH() + pad();
    }

    private int blockH() {
        return lineH() + pad() + TAB_H + 2 + cardH() + pad() + lineH();
    }

    private int top() {
        return Math.max(pad(), (this.height - blockH()) / 2);
    }

    private int tabsY() {
        return top() + lineH() + pad();
    }

    private int cardX() {
        return (this.width - cardW()) / 2;
    }

    private int cardY() {
        return tabsY() + TAB_H + 2;
    }

    // ---- widget --------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        Tab[] tabs = allowedTabs().toArray(new Tab[0]);
        int[] widths = new int[tabs.length];
        int total = 0;
        int gap = Math.max(1, pad() / 2);
        for (int i = 0; i < tabs.length; i++) {
            widths[i] = Math.max(TAB_MIN_W, this.font.width(Component.translatable(tabs[i].key)) + 2 * pad());
            total += widths[i] + (i > 0 ? gap : 0);
        }
        int x = (this.width - total) / 2;
        for (int i = 0; i < tabs.length; i++) {
            Tab t = tabs[i];
            addRenderableWidget(new ThemedToggle(x, tabsY(), widths[i], TAB_H, Component.translatable(t.key),
                    () -> this.tab == t, () -> select(t), "hub.tab", "hub.tab.selected"));
            x += widths[i] + gap;
        }
        int buttonH = Math.max(1, QrmTheme.current().texture("button.normal").height());
        int bx = cardX() + pad();
        int by = cardY() + pad() + 5 * rowH();
        int bw = Math.max(1, cardW() - 2 * pad());
        dutyEnter = addRenderableWidget(new ThemedButton(bx, by, bw, buttonH,
                Component.translatable("qrm.hub.duty.enter"), () -> sendDuty(true), ThemedButton.Style.CONFIRM));
        dutyLeave = addRenderableWidget(new ThemedButton(bx, by, bw, buttonH,
                Component.translatable("qrm.hub.duty.leave"), () -> sendDuty(false), ThemedButton.Style.DANGER));
        updateDutyButtons();
    }

    private void select(Tab t) {
        this.tab = t;
        this.scroll = 0;
    }

    private void shift(int delta) {
        List<Tab> tabs = allowedTabs();
        if (tabs.isEmpty()) return;
        int index = Math.max(0, tabs.indexOf(tab));
        select(tabs.get(Math.floorMod(index + delta, tabs.size())));
    }

    private void sendDuty(boolean on) {
        ClientPacketDistributor.sendToServer(new SetDutyPayload(on));
    }

    /** Il pulsante compare solo nella scheda Lavoro, se il server lo consente e il lavoro è noto. */
    private void updateDutyButtons() {
        if (dutyEnter == null || dutyLeave == null) return;
        PlayerStatus s = ClientState.status();
        boolean canToggle = tab == Tab.JOB && s.ui().selfDuty() && s.job().map(PlayerStatus.JobInfo::known).orElse(false);
        boolean onDuty = s.job().map(PlayerStatus.JobInfo::onDuty).orElse(false);
        dutyEnter.visible = canToggle && !onDuty;
        dutyLeave.visible = canToggle && onDuty;
    }

    // ---- contenuto -----------------------------------------------------------------------------------------

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private static String upper(String key) {
        return tr(key).toUpperCase(Locale.ROOT);
    }

    private List<Row> rows() {
        PlayerStatus s = ClientState.status();
        int text = ThemeRenderer.color("text");
        int muted = ThemeRenderer.color("muted");
        List<Row> out = new ArrayList<>();
        switch (tab) {
            case ACCOUNTS -> {
                for (PlayerStatus.BalanceEntry b : s.balances()) {
                    String label = b.currency().equals(s.primaryCurrency())
                            ? b.currency() + " (" + tr("qrm.hub.primary") + ")" : b.currency();
                    out.add(new Row(label, s.format(b), text, 0));
                }
            }
            case JOB -> s.job().ifPresent(j -> {
                out.add(new Row(tr("qrm.hub.job.label"), j.jobLabel(), text, 0));
                out.add(new Row(tr("qrm.hub.job.grade"), j.gradeLabel(), text, 0));
                out.add(new Row(tr("qrm.hub.job.duty"), tr(j.onDuty() ? "qrm.hub.duty.on" : "qrm.hub.duty.off"), text,
                        ThemeRenderer.color(j.onDuty() ? "statusOn" : "statusOff")));
                if (j.sinceEpochSecond() > 0) {
                    out.add(new Row(tr("qrm.hub.job.since"), Instant.ofEpochSecond(j.sinceEpochSecond())
                            .atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMAT), text, 0));
                }
            });
            case ORGS -> {
                for (PlayerStatus.OrgEntry o : s.orgs()) {
                    out.add(new Row(o.label(), o.viaJob() ? tr("qrm.hub.via_job") : o.rankLabel(),
                            o.viaJob() ? muted : text, 0));
                }
            }
        }
        return out;
    }

    private String emptyText() {
        return tr(switch (tab) {
            case ACCOUNTS -> "qrm.hub.empty.accounts";
            case JOB -> "qrm.hub.empty.job";
            case ORGS -> "qrm.hub.empty.orgs";
        });
    }

    // ---- input ---------------------------------------------------------------------------------------------

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (KeyBindings.HUB.matches(event)) {
            onClose();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_LEFT) {
            shift(-1);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_RIGHT) {
            shift(1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scroll -= (int) Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }


    // ---- disegno -------------------------------------------------------------------------------------------


    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int pad = pad();
        int cx = this.width / 2;
        g.centeredText(this.font, upper("qrm.hub.title"), cx, top(), ThemeRenderer.color("text"));

        int cardX = cardX();
        int cardY = cardY();
        ThemeRenderer.draw(g, "character.idcard", cardX, cardY, cardW(), cardH());

        List<Row> rows = rows();
        int maxScroll = Math.max(0, rows.size() - VISIBLE_ROWS);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        int innerX = cardX + pad;
        int innerW = Math.max(1, cardW() - 2 * pad);
        int y = cardY + pad;
        if (rows.isEmpty()) {
            g.centeredText(this.font, TextFit.fit(this.font, emptyText(), innerW), cx, y, ThemeRenderer.color("muted"));
        } else {
            for (int i = 0; i < VISIBLE_ROWS && scroll + i < rows.size(); i++) {
                drawRow(g, rows.get(scroll + i), innerX, innerW, y);
                y += rowH();
            }
            if (maxScroll > 0) {
                String position = (scroll + 1) + "-" + Math.min(rows.size(), scroll + VISIBLE_ROWS) + "/" + rows.size();
                g.text(this.font, position, innerX + innerW - this.font.width(position),
                        cardY + pad + VISIBLE_ROWS * rowH(), ThemeRenderer.color("muted"));
            }
        }
        g.centeredText(this.font, tr("qrm.hub.hint"), cx, cardY + cardH() + pad, ThemeRenderer.color("muted"));
        updateDutyButtons();
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void drawRow(GuiGraphicsExtractor g, Row r, int innerX, int innerW, int y) {
        String value = TextFit.fit(this.font, r.value(), innerW * 2 / 3);
        int valueW = this.font.width(value);
        int dotSpace = r.dotColor() != 0 ? DOT + 4 : 0;
        String label = TextFit.fit(this.font, r.label(), Math.max(1, innerW - valueW - dotSpace - pad()));
        g.text(this.font, label, innerX, y, ThemeRenderer.color("accent2"));
        int valueX = innerX + innerW - valueW;
        g.text(this.font, value, valueX, y, r.valueColor());
        if (r.dotColor() != 0) {
            int dotY = y + (lineH() - DOT) / 2;
            g.fill(valueX - DOT - 4, dotY, valueX - 4, dotY + DOT, r.dotColor());
        }
    }
}
