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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Schermata dell'ATM: tre schede (conto, contanti, bonifico), nello stile di uno sportello d'epoca con il cassiere
 * a blocchi sul bordo. Poco testo e molte icone: i nomi compaiono come suggerimento al passaggio. Tutte le regole
 * stanno sul server; qui si aiuta solo chi digita (percentuali rapide, pulsanti attivi solo con un importo valido).
 */
final class AtmScreen extends QrmScreen {
    private enum Tab { ACCOUNT, CASH, TRANSFER }

    private static final int PANEL_W = 270;
    private static final int PANEL_H = 200;
    private static final int BORDER = 12;
    private static final int FIELD_H = 20;
    private static final int BUTTON_H = 20;
    private static final int PRESET_W = 56;
    private static final int PRESET_H = 14;
    private static final int SEND_W = 120;
    private static final int HINT_COLOR = 0x8FB9A2;
    private static final int GAP = 4;
    private static final int STEP = FIELD_H + GAP;
    private static final int PLATE_PAD = 8;
    private static final int FOOTER_H = 22;
    private static final int CLOSE_W = 26;
    private static final int LEDGER_ROW = 20;
    private static final int HELD_ROW = 20;
    private static final long HAPPY_MS = 1800;
    private static final String[] PRESET_IDS = {"25", "50", "75", "max"};
    private static final int[] PERCENTS = {25, 50, 75, 100};
    private static final String ICON = "qrm_bank.ui.icon_";
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());

    private AtmStatePayload state;
    private Tab tab = Tab.ACCOUNT;
    private int currencyIndex;
    // Ogni scheda ha il proprio importo: quello del prelievo non deve comparire nel bonifico (e viceversa).
    private String cashAmount = "";
    private String transferAmount = "";
    private String recipientText = "";
    private String noteText = "";
    private BankResult lastResult;
    private int scroll;
    private long happyUntil;

    private AtmField amountField;
    private AtmButton withdrawButton;
    private AtmButton depositButton;
    private AtmButton sendButton;

    private int x0, y0, cx, cw, top, footerY;

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
            happyUntil = System.currentTimeMillis() + HAPPY_MS;
            if (tab == Tab.CASH) cashAmount = "";
            if (tab == Tab.TRANSFER) {
                transferAmount = "";
                noteText = "";
            }
        }
        rebuildWidgets();
    }

    // ---- layout ---------------------------------------------------------------------------------------

    @Override
    protected void init() {
        x0 = Math.max(30, (this.width - PANEL_W) / 2);
        y0 = Math.max(AtmMascot.HEIGHT, (this.height - PANEL_H) / 2);
        cx = x0 + BORDER;
        cw = PANEL_W - 2 * BORDER;
        top = y0 + BORDER + 2;
        footerY = y0 + PANEL_H - 8 - 4 - FOOTER_H;
        amountField = null;
        withdrawButton = null;
        depositButton = null;
        sendButton = null;
        int ty = y0 + 14;
        for (Tab t : Tab.values()) {
            Tab target = t;
            String name = t.name().toLowerCase(Locale.ROOT);
            addRenderableWidget(AtmButton.tab(x0 - 26, ty, Component.translatable("qrm_bank.tab." + name),
                    ICON + name, tab == target, () -> select(target)));
            ty += 22;
        }
        switch (tab) {
            case CASH -> initCash();
            case TRANSFER -> initTransfer();
            case ACCOUNT -> {}
        }
        addRenderableWidget(AtmButton.icon(cx + cw - CLOSE_W, footerY + (FOOTER_H - 18) / 2, CLOSE_W, 18,
                Component.translatable("qrm_bank.atm.close"), ICON + "power", AtmButton.Style.DANGER, this::onClose));
        refreshActions();
    }

    private void select(Tab target) {
        if (tab == target) return;
        tab = target;
        currencyIndex = 0;
        lastResult = null;
        scroll = 0;
        rebuildWidgets();
    }

    private List<String> allCurrencyCodes() {
        return state.currencies().stream().map(AtmStatePayload.CurrencyInfo::code).toList();
    }

    private void initCash() {
        if (state.cashCurrencies().isEmpty()) return;
        String code = selectedCurrency(state.cashCurrencies());
        addCurrencyButton(top, state.cashCurrencies());
        addAmountField(top + STEP, cashAmount, v -> cashAmount = v);
        addPresets(top + 2 * STEP, code, v -> cashAmount = v);
        int by = top + 2 * STEP + PRESET_H + GAP;
        int bw = (cw - GAP) / 2;
        withdrawButton = addRenderableWidget(AtmButton.button(cx, by, bw, BUTTON_H, Component.translatable("qrm_bank.atm.withdraw"),
                ICON + "withdraw", AtmButton.Style.DEFAULT, this::withdraw));
        depositButton = addRenderableWidget(AtmButton.button(cx + bw + GAP, by, cw - bw - GAP, BUTTON_H,
                Component.translatable("qrm_bank.atm.deposit"), ICON + "deposit", AtmButton.Style.CONFIRM, this::deposit));
    }

    private void initTransfer() {
        AtmField recipient = new AtmField(this.font, cx, top, cw, FIELD_H, Component.translatable("qrm_bank.atm.recipient"),
                ICON + "person");
        recipient.setMaxLength(129);
        recipient.setValue(recipientText);
        recipient.setResponder(s -> {
            recipientText = s;
            refreshActions();
        });
        addRenderableWidget(recipient);
        String code = selectedCurrency(allCurrencyCodes());
        addCurrencyButton(top + STEP, allCurrencyCodes());
        addAmountField(top + 2 * STEP, transferAmount, v -> transferAmount = v);
        addPresets(top + 3 * STEP, code, v -> transferAmount = v);
        int ny = top + 3 * STEP + PRESET_H + GAP;
        AtmField note = new AtmField(this.font, cx, ny, cw, FIELD_H, Component.translatable("qrm_bank.atm.note"), null);
        note.setMaxLength(TransferNote.MAX_LENGTH);
        note.setValue(noteText);
        note.setResponder(v -> noteText = v);
        note.setHint(hint("qrm_bank.atm.note"));
        addRenderableWidget(note);
        sendButton = addRenderableWidget(AtmButton.graphic(cx, ny + STEP + 2, SEND_W, BUTTON_H,
                Component.translatable("qrm_bank.atm.send"), "send", this::send));
    }

    private static Component hint(String key) {
        return Component.translatable(key).withStyle(style -> style.withColor(HINT_COLOR));
    }

    /** Il pulsante della valuta: moneta, codice e frecce che ciclano. Non serve altro testo. */
    private void addCurrencyButton(int y, List<String> codes) {
        if (codes.isEmpty()) return;
        String current = codes.get(currencyIndex % codes.size());
        AtmButton b = AtmButton.button(cx, y, cw, FIELD_H, Component.literal(current), null,
                AtmButton.Style.DEFAULT, () -> {
                    currencyIndex = (currencyIndex + 1) % codes.size();
                    rebuildWidgets();
                }).trailing(ICON + "cycle");
        b.active = codes.size() > 1;
        addRenderableWidget(b);
    }

    private void addAmountField(int y, String value, Consumer<String> onChange) {
        amountField = new AtmField(this.font, cx, y, cw, FIELD_H, Component.translatable("qrm_bank.atm.amount"), null);
        amountField.setHint(hint("qrm_bank.atm.amount"));
        amountField.setMaxLength(24);
        amountField.setValue(value);
        amountField.setResponder(v -> {
            onChange.accept(v);
            refreshActions();
        });
        addRenderableWidget(amountField);
    }

    /** Quattro scorciatoie (25%, 50%, 75%, tutto) sul saldo del conto: tasti con il numero disegnato nella texture. */
    private void addPresets(int y, String code, Consumer<String> setAmount) {
        Currency currency = currencyOf(code);
        if (currency == null) return;
        long balance = balanceOf(code);
        for (int i = 0; i < PERCENTS.length; i++) {
            int percent = PERCENTS[i];
            int x = cx + i * (cw - PRESET_W) / (PERCENTS.length - 1);
            AtmButton b = addRenderableWidget(AtmButton.graphic(x, y, PRESET_W, PRESET_H,
                    Component.literal(percent + "%"), "preset_" + PRESET_IDS[i], () -> {
                        long units = percent == 100 ? balance : balance * percent / 100;
                        setAmount.accept(BigDecimal.valueOf(units, currency.decimals()).toPlainString());
                        rebuildWidgets();
                    }));
            b.active = balance > 0;
        }
    }

    // ---- stato e regole locali (il server ricontrolla tutto) ----------------------------------------------

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
        if (tab == Tab.CASH && withdrawButton != null) {
            String code = selectedCurrency(state.cashCurrencies());
            long units = quietParse(code, cashAmount);
            withdrawButton.active = units > 0 && units <= balanceOf(code);
            if (depositButton != null) depositButton.active = heldOf(code) > 0;
            markAmount(cashAmount, units, balanceOf(code));
        } else if (tab == Tab.TRANSFER && sendButton != null) {
            String code = selectedCurrency(allCurrencyCodes());
            long units = quietParse(code, transferAmount);
            sendButton.active = units > 0 && units <= balanceOf(code) && !recipientText.isBlank();
            markAmount(transferAmount, units, balanceOf(code));
        }
    }

    private void markAmount(String text, long units, long balance) {
        if (amountField == null) return;
        boolean bad = !text.isBlank() && (units < 0 || units > balance);
        amountField.setTextColor(ThemeRenderer.color(bad ? "qrm_bank.ledger_neg" : "qrm_bank.ink"));
    }

    private long parseAmount(String code, String amountText) {
        long units = quietParse(code, amountText);
        if (units < 0) lastResult = BankResult.INVALID_AMOUNT;
        return units;
    }

    private void withdraw() {
        String code = selectedCurrency(state.cashCurrencies());
        long units = parseAmount(code, cashAmount);
        if (units > 0) ClientPacketDistributor.sendToServer(new WithdrawPayload(code, units));
    }

    private void deposit() {
        String code = selectedCurrency(state.cashCurrencies());
        if (!code.isEmpty()) ClientPacketDistributor.sendToServer(new DepositPayload(code));
    }

    private void send() {
        String code = selectedCurrency(allCurrencyCodes());
        long units = parseAmount(code, transferAmount);
        if (units > 0) {
            ClientPacketDistributor.sendToServer(
                    new TransferPayload(recipientText.strip(), code, units, TransferNote.clean(noteText)));
        }
    }

    private String format(String code, long amount) {
        Currency c = currencyOf(code);
        return c == null ? amount + " " + code : c.format(amount);
    }

    // ---- registro: geometria e scorrimento -----------------------------------------------------------------

    private int plateH() {
        int shown = Math.max(1, Math.min(3, state.balances().size()));
        return Math.max(26, 2 * PLATE_PAD + shown * (this.font.lineHeight + 2) - 2);
    }

    private int ledgerTop() {
        return top + plateH() + 6;
    }

    private int ledgerBottom() {
        return footerY - 4;
    }

    private int ledgerCapacity() {
        return Math.max(1, (ledgerBottom() - ledgerTop() - 8) / LEDGER_ROW);
    }

    private boolean ledgerScrolls() {
        return state.history().size() > ledgerCapacity();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (tab == Tab.ACCOUNT && scrollY != 0 && ledgerScrolls()) {
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
        if (tab == Tab.ACCOUNT) {
            renderAccount(g, mouseX, mouseY);
        } else if (tab == Tab.CASH && state.cashCurrencies().isEmpty()) {
            lcd(g, cx, top, cw, ICON + "error", Component.translatable("qrm_bank.atm.no_cash").getString(),
                    ThemeRenderer.color("qrm_bank.negative"));
        } else if (tab == Tab.CASH) {
            renderHeld(g, top + 2 * STEP + PRESET_H + GAP + 18 + 6);
        }
        renderFooter(g);
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        int plateW = Math.max(80, this.font.width(this.title) + 40);
        int py = y0 - 8;
        ThemeRenderer.draw(g, "qrm_bank.ui.title_plate", x0 + (PANEL_W - plateW) / 2, py, plateW, 16);
        String title = this.title.getString();
        g.text(this.font, title, x0 + (PANEL_W - this.font.width(title)) / 2, py + (16 - this.font.lineHeight) / 2 + 1,
                ThemeRenderer.color("qrm_bank.plate_ink"), false);
        AtmMascot.draw(g, x0 + PANEL_W - AtmMascot.WIDTH - 4, y0 - AtmMascot.HEIGHT + 2,
                System.currentTimeMillis() < happyUntil);

        for (var child : this.children()) {
            if (child instanceof AtmButton b && b.wantsTooltip() && b.hovered()) {
                g.setTooltipForNextFrame(this.font, b.getMessage(), mouseX, mouseY);
            } else if (child instanceof AtmField f && f.isMouseOver(mouseX, mouseY) && !f.isFocused()
                    && f.getValue().isEmpty()) {
                g.setTooltipForNextFrame(this.font, f.label(), mouseX, mouseY);
            }
        }
    }

    /** Testo centrato in x, senza ombra. */
    private void centered(GuiGraphicsExtractor g, String text, int centerX, int y, int color) {
        g.text(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
    }

    /** Targa a vetro scuro con un'icona e un testo corto, per esiti e saldi. */
    private void lcd(GuiGraphicsExtractor g, int x, int y, int w, String icon, String text, int color) {
        ThemeRenderer.draw(g, "qrm_bank.ui.lcd_plate", x, y, w, FOOTER_H);
        ThemeRenderer.draw(g, icon, x + 6, y + (FOOTER_H - 16) / 2, 16, 16);
        g.text(this.font, TextFit.fit(this.font, text, w - 36), x + 28, y + (FOOTER_H - this.font.lineHeight) / 2 + 1,
                color, false);
    }

    /** In basso: l'esito dell'ultima operazione, altrimenti il saldo nella valuta scelta; a destra la X per chiudere. */
    private void renderFooter(GuiGraphicsExtractor g) {
        int w = cw - CLOSE_W - GAP;
        if (lastResult != null) {
            boolean ok = lastResult == BankResult.OK;
            Component message = Component.translatable("qrm_bank.result." + lastResult.name().toLowerCase(Locale.ROOT));
            lcd(g, cx, footerY, w, ICON + (ok ? "ok" : "error"), message.getString(),
                    ThemeRenderer.color(ok ? "qrm_bank.positive" : "qrm_bank.negative"));
        } else if (tab != Tab.ACCOUNT) {
            String code = selectedCurrency(tab == Tab.CASH ? state.cashCurrencies() : allCurrencyCodes());
            if (!code.isEmpty()) {
                lcd(g, cx, footerY, w, ICON + "coin", format(code, balanceOf(code)), ThemeRenderer.color("qrm_bank.gold"));
                return;
            }
            ThemeRenderer.draw(g, "qrm_bank.ui.lcd_plate", cx, footerY, w, FOOTER_H);
        } else {
            ThemeRenderer.draw(g, "qrm_bank.ui.lcd_plate", cx, footerY, w, FOOTER_H);
        }
    }

    /** Icona della riga del registro secondo il motivo e il segno. */
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

    private void renderAccount(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int ink = ThemeRenderer.color("qrm_bank.ink");
        int inkSoft = ThemeRenderer.color("qrm_bank.ink_soft");
        int lineH = this.font.lineHeight + 2;
        int shown = Math.max(1, Math.min(3, state.balances().size()));
        int plateH = plateH();
        ThemeRenderer.draw(g, "qrm_bank.ui.balance_plate", cx, top, cw, plateH);
        ThemeRenderer.draw(g, ICON + "coin", cx + 12, top + (plateH - 16) / 2, 16, 16);
        if (state.balances().isEmpty()) {
            centered(g, "0", cx + cw / 2, top + (plateH - this.font.lineHeight) / 2 + 1, ThemeRenderer.color("qrm_bank.gold"));
        } else {
            int by = top + (plateH - (shown * lineH - 2)) / 2 + 1;
            for (int i = 0; i < shown; i++) {
                AtmStatePayload.BalanceEntry b = state.balances().get(i);
                centered(g, format(b.currency(), b.amount()), cx + cw / 2, by + i * lineH,
                        ThemeRenderer.color("qrm_bank.gold"));
            }
        }

        int ledgerTop = ledgerTop();
        int ledgerBottom = ledgerBottom();
        ThemeRenderer.draw(g, "qrm_bank.ui.inset", cx, ledgerTop, cw, ledgerBottom - ledgerTop);
        if (state.history().isEmpty()) {
            ThemeRenderer.draw(g, ICON + "account", cx + (cw - 16) / 2, ledgerTop + (ledgerBottom - ledgerTop - 16) / 2, 16, 16);
            return;
        }
        boolean scrolls = ledgerScrolls();
        int x = cx + 8;
        int w = cw - 16 - (scrolls ? 7 : 0);
        int y = ledgerTop + 4;
        int rows = ledgerCapacity();
        for (int r = 1; r <= rows; r++) { // rigatura da registro
            g.fill(cx + 6, y + r * LEDGER_ROW - 1, cx + 6 + w + 4, y + r * LEDGER_ROW, 0x22FFFFFF);
        }
        int first = Math.min(scroll, state.history().size() - 1);
        for (int i = first; i < state.history().size() && i < first + rows; i++) {
            AtmStatePayload.HistoryEntry e = state.history().get(i);
            String when = TIME.format(Instant.ofEpochSecond(e.epochSecond()));
            ThemeRenderer.draw(g, ledgerIcon(e), x + 1, y + (LEDGER_ROW - 16) / 2, 16, 16);
            String amount = (e.signedAmount() > 0 ? "+" : "") + format(e.currency(), e.signedAmount());
            int amountW = this.font.width(amount);
            g.text(this.font, amount, x + w - amountW, y + (LEDGER_ROW - this.font.lineHeight) / 2, ThemeRenderer.color(
                    e.signedAmount() > 0 ? "qrm_bank.ledger_pos" : "qrm_bank.ledger_neg"), false);
            int tw = w - 28 - amountW - 6;
            String first1 = e.counterparty().isEmpty() ? when : e.counterparty();
            String second = !e.note().isEmpty() ? "“" + e.note() + "”" : e.counterparty().isEmpty() ? "" : when;
            if (second.isEmpty()) {
                g.text(this.font, TextFit.fit(this.font, first1, tw), x + 28, y + (LEDGER_ROW - this.font.lineHeight) / 2,
                        ink, false);
            } else {
                g.text(this.font, TextFit.fit(this.font, first1, tw), x + 28, y + 1, ink, false);
                g.text(this.font, TextFit.fit(this.font, second, tw), x + 28, y + 10, inkSoft, false);
            }
            if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + LEDGER_ROW) {
                String tip = reasonName(e.reason()) + (e.counterparty().isEmpty() ? "" : " · " + e.counterparty())
                        + "\n" + when + (e.note().isEmpty() ? "" : "\n“" + e.note() + "”");
                g.setTooltipForNextFrame(this.font, Component.literal(tip), mouseX, mouseY);
            }
            y += LEDGER_ROW;
        }
        if (scrolls) drawScrollbar(g, ledgerTop, ledgerBottom);
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int ledgerTop, int ledgerBottom) {
        int trackX = cx + cw - 8;
        int trackTop = ledgerTop + 5;
        int trackH = ledgerBottom - ledgerTop - 10;
        int size = state.history().size();
        int capacity = ledgerCapacity();
        int thumbH = Math.max(8, trackH * capacity / size);
        int range = size - capacity;
        int thumbY = trackTop + (range <= 0 ? 0 : (trackH - thumbH) * Math.min(scroll, range) / range);
        g.fill(trackX, trackTop, trackX + 3, trackTop + trackH, 0x33FFFFFF);
        g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xFFB88420);
    }

    /** Contanti in mano: un riquadro di carta con una riga (banconote e importo) per valuta. */
    private void renderHeld(GuiGraphicsExtractor g, int y) {
        int bottom = footerY - 4;
        if (bottom - y < 14) return;
        ThemeRenderer.draw(g, "qrm_bank.ui.inset", cx, y, cw, bottom - y);
        int ly = y + 4;
        if (state.held().isEmpty()) {
            ThemeRenderer.draw(g, ICON + "cash", cx + (cw - 16) / 2, y + (bottom - y - 16) / 2, 16, 16);
            return;
        }
        for (AtmStatePayload.BalanceEntry b : state.held()) {
            if (ly + HELD_ROW > bottom - 2) break;
            ThemeRenderer.draw(g, ICON + "cash", cx + 10, ly + (HELD_ROW - 16) / 2, 16, 16);
            g.text(this.font, format(b.currency(), b.amount()), cx + 34, ly + (HELD_ROW - this.font.lineHeight) / 2,
                    ThemeRenderer.color("qrm_bank.ink"), false);
            ly += HELD_ROW;
        }
    }
}
