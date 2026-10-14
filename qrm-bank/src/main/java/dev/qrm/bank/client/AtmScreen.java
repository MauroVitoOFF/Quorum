package dev.qrm.bank.client;

import dev.qrm.api.economy.Currency;
import dev.qrm.bank.logic.BankResult;
import dev.qrm.bank.logic.TransferNote;
import dev.qrm.bank.net.AtmStatePayload;
import dev.qrm.bank.net.DepositPayload;
import dev.qrm.bank.net.TransferPayload;
import dev.qrm.bank.net.WithdrawPayload;
import dev.qrm.client.api.screen.QrmScreen;
import dev.qrm.client.api.theme.ThemeRenderer;
import dev.qrm.client.api.widget.TextFit;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Schermata dell'ATM: una sola finestra a tre colonne. A sinistra il titolare con conto, saldo e contanti; al centro
 * prelievo e deposito rapidi e il bonifico; a destra lo storico. Tutte le regole stanno sul server; qui si aiuta
 * solo chi digita (pulsanti attivi solo con un importo valido e disponibile).
 */
final class AtmScreen extends QrmScreen {
    private static final int PANEL_W = 428;
    private static final int PANEL_H = 254;
    private static final int MARGIN = 10;
    private static final int GAP = 6;
    private static final int PAD = 8;
    private static final int TITLE_H = 22;
    private static final int BODY_H = 184;
    private static final int LEFT_W = 120;
    private static final int CENTER_W = 156;
    private static final int RIGHT_W = 120;
    private static final int ACCOUNT_H = 136;
    private static final int HEADER_H = 16;
    private static final int QUICK_H = 16;
    private static final int QUICK_GAP = 4;
    private static final int DEPOSIT_Y = 43;
    private static final int TRANSFER_Y = 86;
    private static final int FIELD_H = 18;
    private static final int STATUS_H = 18;
    private static final int LEDGER_ROW = 20;
    private static final int HINT_COLOR = 0x8FB9A2;
    private static final long[] WITHDRAW_QUICK = {50, 100, 200, 500};
    private static final long[] DEPOSIT_QUICK = {50, 100, 500};
    private static final String ICON = "qrm_bank.ui.icon_";
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());

    private AtmStatePayload state;
    private int currencyIndex;
    private boolean depositCustom;
    // Ogni campo ha il proprio importo: quello del deposito non deve comparire nel bonifico (e viceversa).
    private String depositAmount = "";
    private String transferAmount = "";
    private String recipientText = "";
    private String noteText = "";
    private BankResult lastResult;
    private int scroll;

    private AtmField depositField;
    private AtmField transferField;
    private AtmButton depositOk;
    private AtmButton sendButton;

    private int x0, y0, bx, by;

    AtmScreen(AtmStatePayload state) {
        super(Component.translatable("qrm_bank.atm.title"));
        this.state = state;
    }

    /** Nuovo stato dal server (dopo un'azione o una richiesta di aggiornamento). */
    void update(AtmStatePayload next) {
        this.state = next;
        this.lastResult = next.result();
        if (next.result() == BankResult.TOO_FAR || next.result() == BankResult.NO_PERMISSION) {
            onClose();
            return;
        }
        if (next.result() == BankResult.OK) {
            depositAmount = "";
            depositCustom = false;
            transferAmount = "";
            noteText = "";
        }
        rebuildWidgets();
    }

    // ---- layout ---------------------------------------------------------------------------------------

    private int leftX() {
        return bx;
    }

    private int centerX() {
        return bx + LEFT_W + GAP;
    }

    private int rightX() {
        return centerX() + CENTER_W + GAP;
    }

    @Override
    protected void init() {
        x0 = Math.max(0, (this.width - PANEL_W) / 2);
        y0 = Math.max(0, (this.height - PANEL_H) / 2);
        bx = x0 + MARGIN;
        by = y0 + 8 + TITLE_H + GAP;
        depositField = null;
        transferField = null;
        depositOk = null;
        sendButton = null;

        addRenderableWidget(AtmButton.icon(x0 + PANEL_W - MARGIN - 26, y0 + 9, 22, 20,
                Component.translatable("qrm_bank.atm.close"), ICON + "power", AtmButton.Style.DANGER, this::onClose));
        initAccount();
        initQuick();
        initTransfer();
        refreshActions();
    }

    private void initAccount() {
        List<String> codes = allCurrencyCodes();
        if (codes.size() < 2) return;
        addRenderableWidget(AtmButton.button(leftX() + PAD, by + ACCOUNT_H - PAD - 18, LEFT_W - 2 * PAD, 18, Component.literal(selectedCurrency(codes)),
                null, AtmButton.Style.DEFAULT, () -> {
                    currencyIndex = (currencyIndex + 1) % codes.size();
                    rebuildWidgets();
                }).trailing(ICON + "cycle"));
    }

    private void initQuick() {
        int x = centerX();
        String code = selectedCurrency(allCurrencyCodes());
        Currency currency = currencyOf(code);
        boolean cash = currency != null && state.cashCurrencies().contains(code);
        int bw = (CENTER_W - 3 * QUICK_GAP) / 4;
        int wy = by + HEADER_H + 3;
        for (int i = 0; i < WITHDRAW_QUICK.length; i++) {
            long units = currency == null ? 0 : WITHDRAW_QUICK[i] * pow10(currency.decimals());
            AtmButton b = addRenderableWidget(AtmButton.button(x + i * (bw + QUICK_GAP), wy, bw, QUICK_H,
                    Component.literal(String.valueOf(WITHDRAW_QUICK[i])), null, AtmButton.Style.DEFAULT,
                    () -> ClientPacketDistributor.sendToServer(new WithdrawPayload(code, units))));
            b.active = cash && units > 0 && units <= balanceOf(code);
        }
        int dy = by + DEPOSIT_Y + HEADER_H + 3;
        if (depositCustom) {
            initCustomDeposit(x, dy, code, cash);
            return;
        }
        for (int i = 0; i < DEPOSIT_QUICK.length; i++) {
            long units = currency == null ? 0 : DEPOSIT_QUICK[i] * pow10(currency.decimals());
            AtmButton b = addRenderableWidget(AtmButton.button(x + i * (bw + QUICK_GAP), dy, bw, QUICK_H,
                    Component.literal(String.valueOf(DEPOSIT_QUICK[i])), null, AtmButton.Style.DEFAULT,
                    () -> ClientPacketDistributor.sendToServer(new DepositPayload(code, units))));
            b.active = cash && units > 0 && units <= heldOf(code);
        }
        AtmButton more = addRenderableWidget(AtmButton.button(x + 3 * (bw + QUICK_GAP), dy, bw, QUICK_H,
                Component.literal("…"), null, AtmButton.Style.DEFAULT, () -> {
                    depositCustom = true;
                    rebuildWidgets();
                }));
        more.active = cash;
    }

    /** Importo libero per il deposito: campo, conferma e un tasto per tornare alle cifre rapide. */
    private void initCustomDeposit(int x, int y, String code, boolean cash) {
        int okW = 26;
        int backW = 14;
        depositField = new AtmField(this.font, x, y, CENTER_W - okW - backW - 2 * QUICK_GAP, QUICK_H,
                Component.translatable("qrm_bank.atm.amount"), null);
        depositField.setHint(hint("qrm_bank.atm.amount"));
        depositField.setMaxLength(24);
        depositField.setValue(depositAmount);
        depositField.setResponder(v -> {
            depositAmount = v;
            refreshActions();
        });
        addRenderableWidget(depositField);
        depositOk = addRenderableWidget(AtmButton.icon(x + CENTER_W - okW - backW - QUICK_GAP, y, okW, QUICK_H,
                Component.translatable("qrm_bank.atm.deposit"), ICON + "deposit", AtmButton.Style.CONFIRM, () -> {
                    long units = quietParse(code, depositAmount);
                    if (units > 0) ClientPacketDistributor.sendToServer(new DepositPayload(code, units));
                }));
        depositOk.active = cash;
        addRenderableWidget(AtmButton.button(x + CENTER_W - backW, y, backW, QUICK_H, Component.literal("x"), null,
                AtmButton.Style.DEFAULT, () -> {
                    depositCustom = false;
                    depositAmount = "";
                    rebuildWidgets();
                }));
    }

    private void initTransfer() {
        int x = centerX() + PAD;
        int w = CENTER_W - 2 * PAD;
        int y = by + TRANSFER_Y + 22;
        AtmField recipient = new AtmField(this.font, x, y, w, FIELD_H, Component.translatable("qrm_bank.atm.recipient"),
                null);
        recipient.setMaxLength(129);
        recipient.setHint(hint("qrm_bank.atm.recipient_hint"));
        recipient.setValue(recipientText);
        recipient.setResponder(s -> {
            recipientText = s;
            refreshActions();
        });
        addRenderableWidget(recipient);
        AtmField note = new AtmField(this.font, x, y + FIELD_H + 4, w, FIELD_H, Component.translatable("qrm_bank.atm.note"),
                null);
        note.setMaxLength(TransferNote.MAX_LENGTH);
        note.setHint(hint("qrm_bank.atm.note_hint"));
        note.setValue(noteText);
        note.setResponder(v -> noteText = v);
        addRenderableWidget(note);
        int ry = y + 2 * (FIELD_H + 4);
        int amountW = 68;
        transferField = new AtmField(this.font, x, ry, amountW, FIELD_H, Component.translatable("qrm_bank.atm.amount"), null);
        transferField.setHint(hint("qrm_bank.atm.amount"));
        transferField.setMaxLength(24);
        transferField.setValue(transferAmount);
        transferField.setResponder(v -> {
            transferAmount = v;
            refreshActions();
        });
        addRenderableWidget(transferField);
        sendButton = addRenderableWidget(AtmButton.button(x + amountW + QUICK_GAP, ry, w - amountW - QUICK_GAP, FIELD_H,
                Component.translatable("qrm_bank.atm.send"), ICON + "send", AtmButton.Style.CONFIRM, this::send));
    }

    private static Component hint(String key) {
        return Component.translatable(key).withStyle(style -> style.withColor(HINT_COLOR));
    }

    private static long pow10(int decimals) {
        long v = 1;
        for (int i = 0; i < decimals; i++) v *= 10;
        return v;
    }

    // ---- stato e regole locali (il server ricontrolla tutto) ----------------------------------------------

    private List<String> allCurrencyCodes() {
        return state.currencies().stream().map(AtmStatePayload.CurrencyInfo::code).toList();
    }

    private String selectedCurrency(List<String> codes) {
        return codes.isEmpty() ? "" : codes.get(currencyIndex % codes.size());
    }

    private Currency currencyOf(String code) {
        for (AtmStatePayload.CurrencyInfo c : state.currencies()) {
            if (c.code().equals(code)) {
                try {
                    return new Currency(c.code(), c.symbol(), c.decimals());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private long balanceOf(String code) {
        long sum = 0;
        for (AtmStatePayload.BalanceEntry b : state.balances()) if (b.currency().equals(code)) sum += b.amount();
        return sum;
    }

    private long heldOf(String code) {
        long sum = 0;
        for (AtmStatePayload.BalanceEntry b : state.held()) if (b.currency().equals(code)) sum += b.amount();
        return sum;
    }

    /** Importo digitato in unità minime, oppure -1 se vuoto o non valido (senza toccare il messaggio d'esito). */
    private long quietParse(String code, String amountText) {
        Currency currency = currencyOf(code);
        if (currency == null || amountText.isBlank()) return -1;
        try {
            long units = currency.parseAmount(amountText);
            return units > 0 ? units : -1;
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    /** Attiva i pulsanti solo con un importo valido e colora di rosso l'importo che non va. */
    private void refreshActions() {
        String code = selectedCurrency(allCurrencyCodes());
        if (depositField != null) {
            long units = quietParse(code, depositAmount);
            if (depositOk != null) depositOk.active = state.cashCurrencies().contains(code) && units > 0 && units <= heldOf(code);
            mark(depositField, depositAmount, units, heldOf(code));
        }
        if (transferField != null) {
            long units = quietParse(code, transferAmount);
            if (sendButton != null) {
                sendButton.active = units > 0 && units <= balanceOf(code) && !recipientText.isBlank();
            }
            mark(transferField, transferAmount, units, balanceOf(code));
        }
    }

    private static void mark(AtmField field, String text, long units, long available) {
        boolean bad = !text.isBlank() && (units < 0 || units > available);
        field.setTextColor(ThemeRenderer.color(bad ? "qrm_bank.ledger_neg" : "qrm_bank.ink"));
    }

    private void send() {
        String code = selectedCurrency(allCurrencyCodes());
        long units = quietParse(code, transferAmount);
        if (units > 0) {
            ClientPacketDistributor.sendToServer(
                    new TransferPayload(recipientText.strip(), code, units, TransferNote.clean(noteText)));
        } else {
            lastResult = BankResult.INVALID_AMOUNT;
        }
    }

    private String format(String code, long amount) {
        Currency c = currencyOf(code);
        return c == null ? amount + " " + code : c.format(amount);
    }

    // ---- storico: geometria e scorrimento ------------------------------------------------------------------

    private int ledgerTop() {
        return by + HEADER_H + 6;
    }

    private int ledgerBottom() {
        return by + BODY_H - PAD;
    }

    private int ledgerCapacity() {
        return Math.max(1, (ledgerBottom() - ledgerTop() - 8) / LEDGER_ROW);
    }

    private boolean ledgerScrolls() {
        return state.history().size() > ledgerCapacity();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (scrollY != 0 && ledgerScrolls()) {
            int max = state.history().size() - ledgerCapacity();
            scroll = Math.max(0, Math.min(scroll - (int) Math.signum(scrollY), max));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    // ---- disegno -------------------------------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        ThemeRenderer.draw(g, "qrm_bank.ui.frame", x0, y0, PANEL_W, PANEL_H);
        ThemeRenderer.draw(g, "qrm_bank.ui.title_bar", x0 + MARGIN, y0 + 8, PANEL_W - 2 * MARGIN, TITLE_H);
        String title = this.title.getString();
        g.text(this.font, title, x0 + (PANEL_W - this.font.width(title)) / 2, y0 + 8 + (TITLE_H - this.font.lineHeight) / 2 + 1,
                ThemeRenderer.color("qrm_bank.title"), false);
        renderAccount(g, mouseX, mouseY);
        renderCash(g);
        renderQuickHeaders(g);
        renderTransferPanel(g);
        renderHistory(g, mouseX, mouseY);
        renderStatus(g);
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        for (var child : this.children()) {
            if (child instanceof AtmButton b && b.wantsTooltip() && b.hovered()) {
                g.setTooltipForNextFrame(this.font, b.getMessage(), mouseX, mouseY);
            } else if (child instanceof AtmField f && f.isMouseOver(mouseX, mouseY) && !f.isFocused()
                    && f.getValue().isEmpty()) {
                g.setTooltipForNextFrame(this.font, f.label(), mouseX, mouseY);
            }
        }
    }

    private void label(GuiGraphicsExtractor g, String text, int x, int y) {
        g.text(this.font, text, x, y, ThemeRenderer.color("qrm_bank.label"), false);
    }

    /** Riga «ETICHETTA ........ valore» dentro un pannello. */
    private void row(GuiGraphicsExtractor g, int x, int w, int y, String name, String value, int valueColor) {
        label(g, name, x, y);
        int room = w - this.font.width(name) - 6;
        String fit = TextFit.fit(this.font, value, room);
        g.text(this.font, fit, x + w - this.font.width(fit), y, valueColor, false);
    }

    private void renderAccount(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = leftX();
        int inner = LEFT_W - 2 * PAD;
        ThemeRenderer.draw(g, "qrm_bank.ui.panel", x, by, LEFT_W, ACCOUNT_H);
        ThemeRenderer.draw(g, "qrm_bank.ui.inset", x + PAD, by + PAD, 32, 32);
        var player = this.minecraft == null ? null : this.minecraft.player;
        if (player != null) {
            PlayerFaceExtractor.extractRenderState(g, player.getSkin(), x + PAD + 2, by + PAD + 2, 28);
        } else {
            ThemeRenderer.draw(g, ICON + "person", x + PAD + 4, by + PAD + 4, 24, 24);
        }
        String name = state.holder().isEmpty() ? "?" : state.holder();
        int space = name.indexOf(' ');
        String first = space < 0 ? name : name.substring(0, space);
        String rest = space < 0 ? "" : name.substring(space + 1);
        int nx = x + PAD + 32 + 6;
        int badgeRoom = state.vip() ? 18 : 0;
        int nw = LEFT_W - PAD - (nx - x) - badgeRoom;
        g.text(this.font, TextFit.fit(this.font, first, nw), nx, by + PAD + 7, ThemeRenderer.color("qrm_bank.ink"), false);
        if (!rest.isEmpty()) {
            g.text(this.font, TextFit.fit(this.font, rest, nw), nx, by + PAD + 18,
                    ThemeRenderer.color("qrm_bank.ink_soft"), false);
        }
        if (state.vip()) {
            int bx2 = x + LEFT_W - PAD - 16;
            ThemeRenderer.draw(g, ICON + "vip", bx2, by + PAD + 8, 16, 16);
            if (mouseX >= bx2 && mouseX < bx2 + 16 && mouseY >= by + PAD + 8 && mouseY < by + PAD + 24) {
                g.setTooltipForNextFrame(this.font, Component.translatable("qrm_bank.atm.vip"), mouseX, mouseY);
            }
        }
        g.fill(x + PAD, by + 46, x + LEFT_W - PAD, by + 47, 0x55000000);
        if (!state.jobLabel().isEmpty()) {
            ThemeRenderer.draw(g, ICON + "job", x + PAD, by + 51, 16, 16);
            g.text(this.font, TextFit.fit(this.font, state.jobLabel(), inner - 22), x + PAD + 20, by + 55,
                    ThemeRenderer.color("qrm_bank.cream"), false);
        }
        String code = selectedCurrency(allCurrencyCodes());
        int gold = ThemeRenderer.color("qrm_bank.gold");
        row(g, x + PAD, inner, by + 72, Component.translatable("qrm_bank.atm.account_id").getString(),
                state.accountCode(), gold);
        label(g, Component.translatable("qrm_bank.atm.balance_label").getString(), x + PAD, by + 85);
        String balance = code.isEmpty() ? "0" : format(code, balanceOf(code));
        g.text(this.font, TextFit.fit(this.font, balance, inner), x + PAD, by + 96, gold, false);
    }

    private void renderCash(GuiGraphicsExtractor g) {
        int x = leftX();
        int y = by + ACCOUNT_H + GAP;
        int h = BODY_H - ACCOUNT_H - GAP;
        ThemeRenderer.draw(g, "qrm_bank.ui.panel", x, y, LEFT_W, h);
        label(g, Component.translatable("qrm_bank.atm.cash_label").getString(), x + PAD, y + PAD);
        ThemeRenderer.draw(g, ICON + "cash", x + PAD, y + 22, 16, 16);
        String code = selectedCurrency(allCurrencyCodes());
        boolean cash = state.cashCurrencies().contains(code);
        String value = cash ? format(code, heldOf(code)) : "-";
        g.text(this.font, TextFit.fit(this.font, value, LEFT_W - 2 * PAD - 22), x + PAD + 22, y + 26,
                ThemeRenderer.color(cash ? "qrm_bank.gold" : "qrm_bank.ink_soft"), false);
    }

    private void renderQuickHeaders(GuiGraphicsExtractor g) {
        header(g, by, ICON + "withdraw", "qrm_bank.atm.quick_withdraw");
        header(g, by + DEPOSIT_Y, ICON + "deposit", "qrm_bank.atm.quick_deposit");
    }

    private void header(GuiGraphicsExtractor g, int y, String icon, String key) {
        int x = centerX();
        ThemeRenderer.draw(g, "qrm_bank.ui.button_normal", x, y, CENTER_W, HEADER_H);
        ThemeRenderer.draw(g, icon, x + 4, y, 16, 16);
        String text = Component.translatable(key).getString();
        g.text(this.font, text, x + 24, y + (HEADER_H - this.font.lineHeight) / 2 + 1,
                ThemeRenderer.color("qrm_bank.cream"), false);
    }

    private void renderTransferPanel(GuiGraphicsExtractor g) {
        int x = centerX();
        int y = by + TRANSFER_Y;
        ThemeRenderer.draw(g, "qrm_bank.ui.panel", x, y, CENTER_W, BODY_H - TRANSFER_Y);
        String text = Component.translatable("qrm_bank.atm.transfer").getString();
        g.text(this.font, text, x + (CENTER_W - this.font.width(text)) / 2, y + 8, ThemeRenderer.color("qrm_bank.title"),
                false);
    }

    private void renderStatus(GuiGraphicsExtractor g) {
        int y = by + BODY_H + GAP;
        int w = PANEL_W - 2 * MARGIN;
        ThemeRenderer.draw(g, "qrm_bank.ui.lcd_plate", bx, y, w, STATUS_H);
        String icon;
        String text;
        int color;
        if (lastResult != null) {
            boolean ok = lastResult == BankResult.OK;
            icon = ICON + (ok ? "ok" : "error");
            text = Component.translatable("qrm_bank.result." + lastResult.name().toLowerCase(Locale.ROOT)).getString();
            color = ThemeRenderer.color(ok ? "qrm_bank.positive" : "qrm_bank.negative");
        } else {
            icon = ICON + "coin";
            text = Component.translatable("qrm_bank.atm.ready").getString();
            color = ThemeRenderer.color("qrm_bank.lcd");
        }
        ThemeRenderer.draw(g, icon, bx + 4, y, 16, 16);
        g.text(this.font, TextFit.fit(this.font, text, w - 30), bx + 24, y + (STATUS_H - this.font.lineHeight) / 2 + 1,
                color, false);
    }

    /** Icona della riga dello storico secondo il motivo e il segno. */
    private static String ledgerIcon(AtmStatePayload.HistoryEntry e) {
        return ICON + switch (e.reason()) {
            case "bank.withdraw" -> "withdraw";
            case "bank.deposit" -> "deposit";
            case "bank.deposit_revert" -> "cycle";
            case "admin.grant", "admin.take" -> "admin";
            case "bank.transfer", "player.pay" -> e.signedAmount() > 0 ? "in" : "out";
            default -> "coin";
        };
    }

    private static String reasonName(String reason) {
        String key = "qrm_bank.reason." + reason.replace('.', '_');
        return Language.getInstance().has(key) ? Language.getInstance().getOrDefault(key) : reason;
    }

    private void renderHistory(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int px = rightX();
        ThemeRenderer.draw(g, "qrm_bank.ui.panel", px, by, RIGHT_W, BODY_H);
        String text = Component.translatable("qrm_bank.atm.history").getString();
        g.text(this.font, text, px + (RIGHT_W - this.font.width(text)) / 2, by + 8, ThemeRenderer.color("qrm_bank.title"),
                false);
        int ledgerTop = ledgerTop();
        int ledgerBottom = ledgerBottom();
        int cx = px + PAD;
        int cw = RIGHT_W - 2 * PAD;
        ThemeRenderer.draw(g, "qrm_bank.ui.inset", cx, ledgerTop, cw, ledgerBottom - ledgerTop);
        if (state.history().isEmpty()) {
            ThemeRenderer.draw(g, ICON + "account", cx + (cw - 16) / 2, ledgerTop + (ledgerBottom - ledgerTop - 16) / 2, 16, 16);
            return;
        }
        int ink = ThemeRenderer.color("qrm_bank.ink");
        int inkSoft = ThemeRenderer.color("qrm_bank.ink_soft");
        boolean scrolls = ledgerScrolls();
        int x = cx + 4;
        int w = cw - 8 - (scrolls ? 6 : 0);
        int y = ledgerTop + 4;
        int rows = ledgerCapacity();
        for (int r = 1; r <= rows; r++) { // rigatura da registro
            g.fill(cx + 3, y + r * LEDGER_ROW - 1, cx + cw - 3, y + r * LEDGER_ROW, 0x18FFFFFF);
        }
        int first = Math.min(scroll, state.history().size() - 1);
        for (int i = first; i < state.history().size() && i < first + rows; i++) {
            AtmStatePayload.HistoryEntry e = state.history().get(i);
            String when = TIME.format(Instant.ofEpochSecond(e.epochSecond()));
            String amount = (e.signedAmount() > 0 ? "+" : "") + format(e.currency(), e.signedAmount());
            g.text(this.font, TextFit.fit(this.font, amount, w), x, y + 1, ThemeRenderer.color(
                    e.signedAmount() > 0 ? "qrm_bank.ledger_pos" : "qrm_bank.ledger_neg"), false);
            String who = e.counterparty().isEmpty() ? reasonName(e.reason()) : e.counterparty();
            g.text(this.font, TextFit.fit(this.font, who, w), x, y + 10, e.counterparty().isEmpty() ? inkSoft : ink, false);
            if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + LEDGER_ROW) {
                String tip = reasonName(e.reason()) + (e.counterparty().isEmpty() ? "" : " · " + e.counterparty())
                        + "\n" + when + (e.note().isEmpty() ? "" : "\n“" + e.note() + "”");
                g.setTooltipForNextFrame(this.font, Component.literal(tip), mouseX, mouseY);
            }
            y += LEDGER_ROW;
        }
        if (scrolls) drawScrollbar(g, cx + cw - 7, ledgerTop, ledgerBottom);
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int trackX, int ledgerTop, int ledgerBottom) {
        int trackTop = ledgerTop + 4;
        int trackH = ledgerBottom - ledgerTop - 8;
        int size = state.history().size();
        int capacity = ledgerCapacity();
        int thumbH = Math.max(8, trackH * capacity / size);
        int range = size - capacity;
        int thumbY = trackTop + (range <= 0 ? 0 : (trackH - thumbH) * Math.min(scroll, range) / range);
        g.fill(trackX, trackTop, trackX + 3, trackTop + trackH, 0x33FFFFFF);
        g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xFFB88420);
    }
}
