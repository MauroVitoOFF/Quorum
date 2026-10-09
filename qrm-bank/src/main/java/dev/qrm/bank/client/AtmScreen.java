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
import dev.qrm.client.api.widget.ThemedButton;
import dev.qrm.client.api.widget.ThemedField;
import dev.qrm.client.api.widget.ThemedToggle;
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

/** Schermata dell'ATM: tre schede (conto, contanti, bonifico). Tutte le regole stanno sul server. */
final class AtmScreen extends QrmScreen {
    private enum Tab { ACCOUNT, CASH, TRANSFER }

    private static final int PANEL_W = 270;
    private static final int PANEL_H = 200;
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
            if (tab == Tab.CASH) cashAmount = "";
            if (tab == Tab.TRANSFER) {
                transferAmount = "";
                noteText = "";
            }
        }
        rebuildWidgets();
    }

    @Override
    protected void init() {
        int x = (this.width - PANEL_W) / 2;
        int y = (this.height - PANEL_H) / 2;
        int tabW = (PANEL_W - 24 - 8) / 3;
        int i = 0;
        for (Tab t : Tab.values()) {
            Tab target = t;
            addRenderableWidget(new ThemedToggle(x + 12 + i * (tabW + 4), y + 24, tabW, 16,
                    Component.translatable("qrm_bank.tab." + t.name().toLowerCase(Locale.ROOT)),
                    () -> tab == target, () -> select(target), "hub.tab", "hub.tab.selected"));
            i++;
        }
        switch (tab) {
            case CASH -> initCash(x, y);
            case TRANSFER -> initTransfer(x, y);
            case ACCOUNT -> {}
        }
        addRenderableWidget(new ThemedButton(x + PANEL_W - 72, y + PANEL_H - 26, 60, 18,
                Component.translatable("qrm_bank.atm.close"), this::onClose, ThemedButton.Style.DANGER));
    }

    private void select(Tab target) {
        if (tab == target) return;
        tab = target;
        currencyIndex = 0;
        lastResult = null;
        rebuildWidgets();
    }

    private List<String> allCurrencyCodes() {
        return state.currencies().stream().map(AtmStatePayload.CurrencyInfo::code).toList();
    }

    private void initCash(int x, int y) {
        if (state.cashCurrencies().isEmpty()) return;
        addCurrencyButton(x, y + 48, state.cashCurrencies());
        addAmountField(x, y + 72, cashAmount, v -> cashAmount = v);
        addRenderableWidget(new ThemedButton(x + 12, y + 98, 100, 18, Component.translatable("qrm_bank.atm.withdraw"),
                this::withdraw, ThemedButton.Style.CONFIRM));
        addRenderableWidget(new ThemedButton(x + 120, y + 98, 100, 18, Component.translatable("qrm_bank.atm.deposit"),
                this::deposit, ThemedButton.Style.CONFIRM));
    }

    private void initTransfer(int x, int y) {
        ThemedField recipient = new ThemedField(this.font, x + 12, y + 48, PANEL_W - 24, 18,
                Component.translatable("qrm_bank.atm.recipient"));
        recipient.setMaxLength(129);
        recipient.setValue(recipientText);
        recipient.setResponder(s -> recipientText = s);
        recipient.setHint(Component.translatable("qrm_bank.atm.recipient"));
        addRenderableWidget(recipient);
        addCurrencyButton(x, y + 72, allCurrencyCodes());
        addAmountField(x, y + 96, transferAmount, v -> transferAmount = v);
        ThemedField note = new ThemedField(this.font, x + 12, y + 120, PANEL_W - 24, 18,
                Component.translatable("qrm_bank.atm.note"));
        note.setMaxLength(TransferNote.MAX_LENGTH);
        note.setValue(noteText);
        note.setResponder(v -> noteText = v);
        note.setHint(Component.translatable("qrm_bank.atm.note"));
        addRenderableWidget(note);
        addRenderableWidget(new ThemedButton(x + 12, y + 146, 100, 18, Component.translatable("qrm_bank.atm.send"),
                this::send, ThemedButton.Style.CONFIRM));
    }

    private void addCurrencyButton(int x, int y, List<String> codes) {
        if (codes.isEmpty()) return;
        String current = codes.get(currencyIndex % codes.size());
        addRenderableWidget(new ThemedButton(x + 12, y, PANEL_W - 24, 18,
                Component.translatable("qrm_bank.atm.currency", current), () -> {
                    currencyIndex = (currencyIndex + 1) % codes.size();
                    rebuildWidgets();
                }));
    }

    private void addAmountField(int x, int y, String value, Consumer<String> onChange) {
        ThemedField amount = new ThemedField(this.font, x + 12, y, PANEL_W - 24, 18,
                Component.translatable("qrm_bank.atm.amount"));
        amount.setMaxLength(24);
        amount.setValue(value);
        amount.setResponder(onChange);
        amount.setHint(Component.translatable("qrm_bank.atm.amount"));
        addRenderableWidget(amount);
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

    /** Importo digitato in unità minime, oppure -1 (e messaggio d'errore locale) se non valido. */
    private long parseAmount(String code, String amountText) {
        Currency currency = currencyOf(code);
        try {
            if (currency == null) throw new IllegalArgumentException("unknown currency");
            long units = currency.parseAmount(amountText);
            if (units <= 0) throw new IllegalArgumentException("zero");
            return units;
        } catch (IllegalArgumentException e) {
            lastResult = BankResult.INVALID_AMOUNT;
            return -1;
        }
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

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = (this.width - PANEL_W) / 2;
        int y = (this.height - PANEL_H) / 2;
        ThemeRenderer.draw(g, "character.idcard", x, y, PANEL_W, PANEL_H);
        g.text(this.font, this.title, x + 12, y + 10, ThemeRenderer.color("text"));
        if (tab == Tab.ACCOUNT) {
            renderAccount(g, x + 12, y + 48, PANEL_W - 24);
        } else if (tab == Tab.CASH && state.cashCurrencies().isEmpty()) {
            g.text(this.font, Component.translatable("qrm_bank.atm.no_cash"), x + 12, y + 52,
                    ThemeRenderer.color("qrm_bank.muted"));
        } else if (tab == Tab.CASH) {
            renderHeld(g, x + 12, y + 126);
        }
        if (lastResult != null) {
            boolean ok = lastResult == BankResult.OK;
            Component message = Component.translatable("qrm_bank.result." + lastResult.name().toLowerCase(Locale.ROOT));
            g.text(this.font, TextFit.fit(this.font, message.getString(), PANEL_W - 90), x + 12, y + PANEL_H - 22,
                    ThemeRenderer.color(ok ? "qrm_bank.positive" : "qrm_bank.negative"));
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void renderAccount(GuiGraphicsExtractor g, int x, int y, int w) {
        int muted = ThemeRenderer.color("qrm_bank.muted");
        int text = ThemeRenderer.color("text");
        g.text(this.font, Component.translatable("qrm_bank.atm.balances"), x, y, muted);
        y += 11;
        if (state.balances().isEmpty()) {
            g.text(this.font, Component.translatable("qrm_bank.atm.no_balance"), x, y, text);
            y += 10;
        }
        for (AtmStatePayload.BalanceEntry b : state.balances()) {
            g.text(this.font, format(b.currency(), b.amount()), x, y, text);
            y += 10;
        }
        y += 4;
        g.text(this.font, Component.translatable("qrm_bank.atm.history"), x, y, muted);
        y += 11;
        if (state.history().isEmpty()) {
            g.text(this.font, Component.translatable("qrm_bank.atm.no_history"), x, y, text);
            return;
        }
        int bottom = (this.height + PANEL_H) / 2 - 30;
        for (AtmStatePayload.HistoryEntry e : state.history()) {
            if (y > bottom) break;
            String reasonKey = "qrm_bank.reason." + e.reason().replace('.', '_');
            String reason = Language.getInstance().has(reasonKey) ? Language.getInstance().getOrDefault(reasonKey) : e.reason();
            if (!e.counterparty().isEmpty()) {
                reason = Component.translatable(e.signedAmount() > 0 ? "qrm_bank.history.from" : "qrm_bank.history.to",
                        reason, e.counterparty()).getString();
            }
            String line = TIME.format(Instant.ofEpochSecond(e.epochSecond())) + " " + reason;
            g.text(this.font, TextFit.fit(this.font, line, w - 70), x, y, text);
            String amount = (e.signedAmount() > 0 ? "+" : "") + format(e.currency(), e.signedAmount());
            g.text(this.font, amount, x + w - this.font.width(amount), y,
                    ThemeRenderer.color(e.signedAmount() > 0 ? "qrm_bank.positive" : "qrm_bank.negative"));
            y += 10;
            if (!e.note().isEmpty() && y <= bottom) {
                g.text(this.font, TextFit.fit(this.font, "“" + e.note() + "”", w - 8), x + 8, y,
                        ThemeRenderer.color("qrm_bank.muted"));
                y += 10;
            }
        }
    }

    private void renderHeld(GuiGraphicsExtractor g, int x, int y) {
        g.text(this.font, Component.translatable("qrm_bank.atm.held"), x, y, ThemeRenderer.color("qrm_bank.muted"));
        y += 11;
        for (AtmStatePayload.BalanceEntry b : state.held()) {
            g.text(this.font, format(b.currency(), b.amount()), x, y, ThemeRenderer.color("text"));
            y += 10;
        }
    }
}
