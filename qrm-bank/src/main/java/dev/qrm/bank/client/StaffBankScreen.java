package dev.qrm.bank.client;

import dev.qrm.bank.logic.StaffBankLogic;
import dev.qrm.bank.net.StaffBankAction;
import dev.qrm.bank.net.StaffBankActionPayload;
import dev.qrm.bank.net.StaffBankResult;
import dev.qrm.bank.net.StaffBankStatePayload;
import dev.qrm.client.api.screen.QrmScreen;
import dev.qrm.client.api.staff.StaffEntries;
import dev.qrm.client.api.theme.QrmTheme;
import dev.qrm.client.api.theme.ThemeRenderer;
import dev.qrm.client.api.widget.IconSlot;
import dev.qrm.client.api.widget.TextFit;
import dev.qrm.client.api.widget.ThemedButton;
import dev.qrm.client.api.widget.ThemedField;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Schermata staff della banca, aperta dalla tessera "Banca" del menu staff di QRM: saldi e ultimi movimenti del bersaglio,
 * dai/togli denaro, congela/scongela, dai un ATM e audit. Il server ricontrolla il permesso a ogni azione.
 */
final class StaffBankScreen extends QrmScreen {
    private static final int FRAME_MAX_W = 300;
    private static final int FRAME_MAX_H = 200;
    private static final int FRAME_BORDER = 12;
    private static final int PLATE_W = 120;
    private static final int PLATE_H = 20;
    private static final int BUTTON_H = 20;
    private static final int GAP = 4;
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());

    private final StaffEntries.Context context;
    private final Optional<UUID> target;
    private StaffBankStatePayload state;
    private Component status = Component.empty();
    private int statusColor;
    private String armed;
    private String amountText = "";
    private String selCurrency;
    private int balanceScroll;
    private int historyScroll;

    private ThemedField amountField;
    private ThemedButton currencyButton;
    private ThemedButton grant;
    private ThemedButton take;
    private ThemedButton freeze;
    private ThemedButton unfreeze;
    private ThemedButton giveAtm;
    private IconSlot[] slots = new IconSlot[0];

    private int x0, y0, fw, fh, cx, cw, top, bottom, statusY;

    StaffBankScreen(StaffEntries.Context context) {
        super(Component.translatable("qrm_bank.staff.tile"));
        this.context = context;
        this.target = context.target();
        send(StaffBankAction.REFRESH);
    }

    private void send(StaffBankAction action) {
        send(action, "", "");
    }

    private void send(StaffBankAction action, String amount, String currency) {
        ClientPacketDistributor.sendToServer(new StaffBankActionPayload(action, target, amount, currency));
    }

    /** Invia un'azione; le distruttive chiedono una seconda pressione sullo stesso pulsante. */
    private void act(StaffBankAction action, String amount, String currency, boolean danger) {
        String key = action + "|" + amount + "|" + currency;
        if (danger && !key.equals(armed)) {
            armed = key;
            status = Component.translatable("qrm_bank.staff.confirm");
            statusColor = ThemeRenderer.color("accent2");
            return;
        }
        armed = null;
        send(action, amount, currency);
    }

    private String currency() {
        if (state == null) return "";
        if (selCurrency != null && state.currencies().contains(selCurrency)) return selCurrency;
        return state.primaryCurrency();
    }

    private void nextCurrency() {
        if (state == null || state.currencies().isEmpty()) return;
        List<String> all = state.currencies();
        selCurrency = all.get((all.indexOf(currency()) + 1) % all.size());
        armed = null;
        if (currencyButton != null) currencyButton.setMessage(Component.literal(selCurrency));
    }

    void onState(StaffBankStatePayload payload) {
        this.state = payload;
        armed = null;
        if (currencyButton != null) currencyButton.setMessage(Component.literal(currency()));
        StaffBankResult result = payload.result();
        if (payload.action() == StaffBankAction.REFRESH && result == StaffBankResult.OK) return;
        this.status = describe(payload);
        boolean good = result == StaffBankResult.OK || result == StaffBankResult.ATM_GIVEN
                || result == StaffBankResult.AUDIT_CLEAN;
        this.statusColor = ThemeRenderer.color(good ? "statusOn" : "error");
        if (good && (payload.action() == StaffBankAction.GRANT || payload.action() == StaffBankAction.TAKE)) {
            amountText = "";
            if (amountField != null) amountField.setValue("");
        }
    }

    private static Component describe(StaffBankStatePayload p) {
        if (p.result() == StaffBankResult.AUDIT_ISSUES) {
            return Component.translatable("qrm_bank.staff.audit.issues", p.auditIssues());
        }
        if (p.result() == StaffBankResult.CASH_EXCESS && !p.excess().isEmpty()) {
            StaffBankLogic.Excess e = p.excess().get(0);
            return Component.translatable("qrm_bank.staff.audit.excess", e.held(), e.vault());
        }
        return Component.translatable("qrm_bank.staff.result." + p.result().name().toLowerCase(Locale.ROOT));
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private static String reason(String code) {
        String key = "qrm_bank.reason." + code.replace('.', '_');
        return Language.getInstance().has(key) ? Language.getInstance().getOrDefault(key) : code;
    }

    private int pad() {
        return Math.max(0, (int) Math.round(QrmTheme.current().metric("padding")));
    }

    private int lineH() {
        return this.font.lineHeight;
    }

    private int rowH() {
        return lineH() + 4;
    }

    // ---- layout e widget -----------------------------------------------------------------------------------

    @Override
    protected void init() {
        int pad = pad();
        fw = Math.min(FRAME_MAX_W, this.width - 2 * pad);
        fh = Math.max(120, Math.min(FRAME_MAX_H, this.height - 4 * pad));
        x0 = (this.width - fw) / 2;
        y0 = Math.max(PLATE_H / 2, (this.height - fh) / 2);
        cx = x0 + FRAME_BORDER;
        cw = fw - 2 * FRAME_BORDER;
        top = y0 + FRAME_BORDER + 4;
        statusY = y0 + fh - FRAME_BORDER - lineH() - 2;
        bottom = statusY - 3;

        int rowB = bottom - BUTTON_H;
        int rowA = rowB - GAP - BUTTON_H;

        // riga A: importo, valuta, Dai, Togli
        int amountW = (int) (cw * 0.28);
        int curW = (int) (cw * 0.16);
        int btnW = (cw - amountW - curW - 3 * GAP) / 2;
        amountField = addRenderableWidget(new ThemedField(this.font, cx, rowA, amountW, BUTTON_H,
                Component.translatable("qrm_bank.staff.amount")));
        amountField.setMaxLength(StaffBankActionPayload.MAX_AMOUNT);
        amountField.setHint(Component.translatable("qrm_bank.staff.amount"));
        amountField.setValue(amountText);
        amountField.setResponder(t -> amountText = t);
        currencyButton = addRenderableWidget(new ThemedButton(cx + amountW + GAP, rowA, curW, BUTTON_H,
                Component.literal(currency()), this::nextCurrency));
        int bx = cx + amountW + curW + 2 * GAP;
        grant = addRenderableWidget(new ThemedButton(bx, rowA, btnW, BUTTON_H, Component.translatable("qrm_bank.staff.grant"),
                () -> act(StaffBankAction.GRANT, amountText.strip(), currency(), false), ThemedButton.Style.CONFIRM));
        take = addRenderableWidget(new ThemedButton(bx + btnW + GAP, rowA, cw - amountW - curW - 3 * GAP - btnW, BUTTON_H,
                Component.translatable("qrm_bank.staff.take"),
                () -> act(StaffBankAction.TAKE, amountText.strip(), currency(), true), ThemedButton.Style.DANGER));

        // riga B: Congela, Scongela, Dai ATM, Audit
        int w4 = (cw - 3 * GAP) / 4;
        freeze = addRenderableWidget(new ThemedButton(cx, rowB, w4, BUTTON_H, Component.translatable("qrm_bank.staff.freeze"),
                () -> act(StaffBankAction.FREEZE, "", "", true), ThemedButton.Style.DANGER));
        unfreeze = addRenderableWidget(new ThemedButton(cx + w4 + GAP, rowB, w4, BUTTON_H,
                Component.translatable("qrm_bank.staff.unfreeze"), () -> send(StaffBankAction.UNFREEZE),
                ThemedButton.Style.CONFIRM));
        giveAtm = addRenderableWidget(new ThemedButton(cx + 2 * (w4 + GAP), rowB, w4, BUTTON_H,
                Component.translatable("qrm_bank.staff.give_atm"), () -> send(StaffBankAction.GIVE_ATM)));
        addRenderableWidget(new ThemedButton(cx + 3 * (w4 + GAP), rowB, cw - 3 * (w4 + GAP), BUTTON_H,
                Component.translatable("qrm_bank.staff.run_audit"), () -> send(StaffBankAction.AUDIT)));

        int gap = 4;
        int sx = x0 + (fw - (3 * IconSlot.SIZE + 2 * gap)) / 2;
        int sy = y0 + fh - FRAME_BORDER;
        IconSlot back = addRenderableWidget(new IconSlot(sx, sy, Component.translatable("qrm_bank.staff.slot.back"),
                "staff.icon.back", context.back()));
        IconSlot refresh = addRenderableWidget(new IconSlot(sx + IconSlot.SIZE + gap, sy,
                Component.translatable("qrm_bank.staff.slot.refresh"), "staff.icon.refresh",
                () -> send(StaffBankAction.REFRESH)));
        IconSlot close = addRenderableWidget(new IconSlot(sx + 2 * (IconSlot.SIZE + gap), sy,
                Component.translatable("qrm_bank.staff.slot.close"), "staff.icon.close", this::onClose));
        slots = new IconSlot[] {back, refresh, close};
    }

    private boolean hasCharacter() {
        return state != null && state.hasCharacter();
    }

    private boolean frozen() {
        return hasCharacter() && state.frozen();
    }

    private void updateButtons() {
        boolean can = hasCharacter();
        boolean money = can && !amountText.isBlank() && !currency().isEmpty();
        grant.active = money;
        take.active = money;
        currencyButton.active = state != null && state.currencies().size() > 1;
        freeze.active = can && !frozen();
        unfreeze.active = frozen();
        giveAtm.active = can;
    }

    private int listTop() {
        return top + 3 * (lineH() + 2) - 2;
    }

    private int rows() {
        return Math.max(1, (bottom - 2 * BUTTON_H - 2 * GAP - listTop()) / rowH());
    }

    private int leftW() {
        return (int) (cw * 0.38);
    }

    private int rightX() {
        return cx + leftW() + GAP;
    }

    private int rightW() {
        return cw - leftW() - GAP;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (scrollY == 0 || state == null) return super.mouseScrolled(mx, my, scrollX, scrollY);
        int delta = -(int) Math.signum(scrollY);
        if (mx >= rightX()) historyScroll = clamp(historyScroll + delta, state.history().size());
        else balanceScroll = clamp(balanceScroll + delta, state.balances().size());
        return true;
    }

    private int clamp(int value, int total) {
        return Math.max(0, Math.min(value, Math.max(0, total - rows())));
    }

    // ---- disegno -------------------------------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        ThemeRenderer.draw(g, "staff.frame", x0, y0, fw, fh);
        updateButtons();

        // intestazione: titolo e bersaglio
        String targetText = Component.translatable("qrm_bank.staff.target",
                context.targetName().isEmpty() ? "-" : context.targetName()).getString();
        int tw = Math.min(this.font.width(targetText), cw / 2);
        g.text(this.font, TextFit.fit(this.font, tr("qrm_bank.staff.tile"), cw - tw - 8), cx, top,
                ThemeRenderer.color("accent2"));
        String fitted = TextFit.fit(this.font, targetText, tw);
        g.text(this.font, fitted, cx + cw - this.font.width(fitted), top, ThemeRenderer.color("text"));

        // stato del conto
        int y = top + lineH() + 2;
        if (state == null) {
            g.text(this.font, tr("qrm_bank.staff.loading"), cx, y, ThemeRenderer.color("muted"));
        } else if (!state.hasCharacter()) {
            g.text(this.font, tr("qrm_bank.staff.account.none"), cx, y, ThemeRenderer.color("muted"));
        } else {
            g.text(this.font, tr(state.frozen() ? "qrm_bank.staff.account.frozen" : "qrm_bank.staff.account.active"), cx, y,
                    ThemeRenderer.color(state.frozen() ? "error" : "statusOn"));
        }

        // colonne: saldi (sinistra) e ultimi movimenti (destra)
        int headY = top + 2 * (lineH() + 2);
        g.text(this.font, tr("qrm_bank.staff.balances"), cx + 2, headY, ThemeRenderer.color("muted"));
        g.text(this.font, tr("qrm_bank.staff.history"), rightX() + 2, headY, ThemeRenderer.color("muted"));
        int ly = listTop();
        int rows = rows();
        g.fill(cx, ly, cx + leftW(), ly + rows * rowH(), ThemeRenderer.color("scrim"));
        g.fill(rightX(), ly, rightX() + rightW(), ly + rows * rowH(), ThemeRenderer.color("scrim"));
        drawBalances(g, ly, rows);
        drawHistory(g, ly, rows);

        g.centeredText(this.font, TextFit.fit(this.font, status.getString(), cw), this.width / 2, statusY, statusColor);
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        int px = (this.width - PLATE_W) / 2;
        int py = y0 - PLATE_H / 2;
        ThemeRenderer.draw(g, "staff.plate", px, py, PLATE_W, PLATE_H);
        String title = tr("qrm_bank.staff.tile").toUpperCase(Locale.ROOT);
        g.centeredText(this.font, title, this.width / 2 + 1, py + (PLATE_H - lineH()) / 2 + 1, 0xFF10161F);
        g.centeredText(this.font, title, this.width / 2, py + (PLATE_H - lineH()) / 2, ThemeRenderer.color("accent2"));
        for (IconSlot s : slots) {
            if (s.hovered()) g.setTooltipForNextFrame(this.font, s.getMessage(), mouseX, mouseY);
        }
    }

    private void drawBalances(GuiGraphicsExtractor g, int ly, int rows) {
        List<StaffBankLogic.Line> balances = state == null ? List.of() : state.balances();
        if (balances.isEmpty()) {
            if (hasCharacter()) {
                g.text(this.font, TextFit.fit(this.font, tr("qrm_bank.staff.balances.empty"), leftW() - 8), cx + 4, ly + 3,
                        ThemeRenderer.color("muted"));
            }
            return;
        }
        balanceScroll = clamp(balanceScroll, balances.size());
        for (int i = 0; i < rows && balanceScroll + i < balances.size(); i++) {
            StaffBankLogic.Line line = balances.get(balanceScroll + i);
            int ry = ly + i * rowH();
            String value = TextFit.fit(this.font, line.text(), leftW() * 2 / 3);
            g.text(this.font, line.currency(), cx + 4, ry + 3, ThemeRenderer.color("accent2"));
            g.text(this.font, value, cx + leftW() - 4 - this.font.width(value), ry + 3, ThemeRenderer.color("text"));
        }
    }

    private void drawHistory(GuiGraphicsExtractor g, int ly, int rows) {
        List<StaffBankLogic.Move> moves = state == null ? List.of() : state.history();
        if (moves.isEmpty()) {
            if (hasCharacter()) {
                g.text(this.font, TextFit.fit(this.font, tr("qrm_bank.staff.history.empty"), rightW() - 8), rightX() + 4,
                        ly + 3, ThemeRenderer.color("muted"));
            }
            return;
        }
        historyScroll = clamp(historyScroll, moves.size());
        for (int i = 0; i < rows && historyScroll + i < moves.size(); i++) {
            StaffBankLogic.Move m = moves.get(historyScroll + i);
            int ry = ly + i * rowH();
            String amount = TextFit.fit(this.font, m.amount(), rightW() / 2 - 8);
            String text = TIME.format(Instant.ofEpochSecond(m.epochSecond())) + " " + reason(m.reason());
            String left = TextFit.fit(this.font, text, rightW() - 12 - this.font.width(amount));
            g.text(this.font, left, rightX() + 4, ry + 3, ThemeRenderer.color("text"));
            g.text(this.font, amount, rightX() + rightW() - 4 - this.font.width(amount), ry + 3,
                    ThemeRenderer.color(m.amount().startsWith("+") ? "qrm_bank.positive" : "qrm_bank.negative"));
        }
    }
}
