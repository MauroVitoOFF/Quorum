package dev.qrm.bank.client;

import dev.qrm.api.economy.Currency;
import dev.qrm.bank.logic.BankResult;
import dev.qrm.bank.logic.HistoryCategory;
import dev.qrm.bank.logic.ValueTween;
import dev.qrm.bank.logic.TransferNote;
import dev.qrm.bank.net.AtmStatePayload;
import dev.qrm.bank.net.DepositPayload;
import dev.qrm.bank.net.SelectAccountPayload;
import dev.qrm.bank.net.TransferPayload;
import dev.qrm.bank.net.WithdrawPayload;
import dev.qrm.client.api.screen.QrmScreen;
import dev.qrm.client.api.theme.ThemeRenderer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Schermata dell'ATM: una sola finestra a tre colonne. A sinistra il titolare con conto, saldo e contanti; al centro
 * prelievo e deposito rapidi e il bonifico; a destra lo storico. Tutte le regole stanno sul server; qui si aiuta
 * solo chi digita (pulsanti attivi solo con un importo valido e disponibile).
 */
final class AtmScreen extends QrmScreen {
    private static final int PANEL_W = 316;
    private static final int PANEL_H = 228;
    private static final int MARGIN = 10;
    private static final int GAP = 8;
    private static final int PAD = 8;
    private static final int TITLE_H = 20;
    private static final int BODY_H = 160;
    private static final int LEFT_W = 104;
    private static final int CENTER_W = PANEL_W - 2 * MARGIN - LEFT_W - GAP;
    private static final int HIST_W = PANEL_W - 2 * MARGIN;
    private static final int ACCOUNT_H = 112;
    private static final int CASH_H = 22;
    private static final int HEADER_H = 14;
    private static final int QUICK_H = 16;
    private static final int QUICK_GAP = 4;
    private static final int DEPOSIT_Y = 40;
    private static final int TRANSFER_Y = 78;
    private static final int FIELD_H = 16;
    private static final int STATUS_H = 14;
    private static final int LEDGER_ROW = 20;
    private static final int DETAIL_LINE = 10;
    private static final int HINT_COLOR = 0x8FB9A2;
    private static final long[] WITHDRAW_QUICK = {50, 100, 200, 500};
    private static final long[] DEPOSIT_QUICK = {50, 100, 500};
    private static final String ICON = "qrm_bank.ui.icon_";
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private AtmStatePayload state;
    private int currencyIndex;
    private boolean depositCustom;
    // Ogni campo ha il proprio importo: quello del deposito non deve comparire nel bonifico (e viceversa).
    private String depositAmount = "";
    private String transferAmount = "";
    private String recipientText = "";
    private String noteText = "";
    private BankResult lastResult;
    private boolean historyPage;
    private boolean choosing;
    private boolean switching;
    private int scroll;
    private int selected = -1;
    // Saldo e contanti "contano" fino al nuovo valore; l'esito dell'ultima azione ha la sua animazione breve.
    private final Map<String, ValueTween> balanceTweens = new HashMap<>();
    private final Map<String, ValueTween> cashTweens = new HashMap<>();
    private long resultAt;

    private AtmField depositField;
    private AtmField transferField;
    private AtmButton depositOk;
    private AtmButton sendButton;

    private int x0, y0, bx, by;

    AtmScreen(AtmStatePayload state) {
        super(Component.translatable("qrm_bank.atm.title"));
        this.state = state;
        // All'apertura, chi può usare anche un conto aziendale sceglie prima con quale operare.
        this.choosing = state.open() && !state.accounts().isEmpty();
    }

    /** Nuovo stato dal server (dopo un'azione o una richiesta di aggiornamento). */
    void update(AtmStatePayload next) {
        this.state = next;
        if (!switching) {
            this.lastResult = next.result();
            this.resultAt = System.currentTimeMillis();
        } else {
            depositAmount = "";
            depositCustom = false;
            transferAmount = "";
            noteText = "";
            scroll = 0;
            switching = false;
        }
        this.selected = -1;
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
        return bx;
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

        addRenderableWidget(AtmButton.icon(x0 + PANEL_W - MARGIN - 18, y0 + 11, 14, 14,
                Component.translatable("qrm_bank.atm.close"), ICON + "power", AtmButton.Style.DANGER, this::onClose));
        if (choosing) {
            initChoice();
            return;
        }
        if (historyPage) return;
        initAccount();
        initQuick();
        initTransfer();
        refreshActions();
    }

    private void initAccount() {
        List<String> codes = allCurrencyCodes();
        boolean multiCurrency = codes.size() >= 2;
        boolean multiAccount = !state.accounts().isEmpty();
        if (!multiCurrency && !multiAccount) return;
        int y = by + ACCOUNT_H + 4;
        int half = (LEFT_W - 4) / 2;
        if (multiCurrency) {
            int w = multiAccount ? half : LEFT_W;
            addRenderableWidget(AtmButton.button(leftX(), y, w, 14, Component.literal(selectedCurrency(codes)),
                    null, AtmButton.Style.DEFAULT, () -> {
                        currencyIndex = (currencyIndex + 1) % codes.size();
                        rebuildWidgets();
                    }).trailing(ICON + "cycle"));
        }
        if (multiAccount) {
            int w = multiCurrency ? half : LEFT_W;
            addRenderableWidget(AtmButton.button(leftX() + LEFT_W - w, y, w, 14,
                    Component.translatable("qrm_bank.atm.switch_account"), null, AtmButton.Style.DEFAULT, () -> {
                        choosing = true;
                        rebuildWidgets();
                    }).trailing(ICON + "cycle"));
        }
    }

    /** Rettangolo della scelta i di count: due affiancati se bastano, altrimenti una griglia. */
    private int[] choiceRect(int i, int count) {
        int cols = count <= 2 ? count : count <= 4 ? 2 : 3;
        int rows = (count + cols - 1) / cols;
        int gap = 8;
        int top = by + 22;
        int w = (HIST_W - 2 * PAD - (cols - 1) * gap) / cols;
        int h = rows == 1 ? BODY_H - 22 - PAD : (BODY_H - 22 - PAD - (rows - 1) * gap) / rows;
        int col = i % cols;
        int row = i / cols;
        int inRow = Math.min(cols, count - row * cols);
        int rowW = inRow * w + (inRow - 1) * gap;
        return new int[] {bx + (HIST_W - rowW) / 2 + col * (w + gap), top + row * (h + gap), w, h};
    }

    private int choiceCount() {
        return Math.min(1 + state.accounts().size(), 6);
    }

    /** Pagina di scelta: un rettangolo grande per il conto personale e uno per ogni organizzazione utilizzabile. */
    private void initChoice() {
        int count = choiceCount();
        for (int i = 0; i < count; i++) {
            String org = i == 0 ? "" : state.accounts().get(i - 1).orgId();
            int[] r = choiceRect(i, count);
            addRenderableWidget(AtmButton.button(r[0], r[1], r[2], r[3], Component.empty(), null,
                    state.activeOrg().equals(org) ? AtmButton.Style.CONFIRM : AtmButton.Style.DEFAULT, () -> {
                        ClientPacketDistributor.sendToServer(new SelectAccountPayload(org));
                        choosing = false;
                        switching = true;
                        historyPage = false;
                        rebuildWidgets();
                    }));
        }
    }

    private void initQuick() {
        int x = centerX();
        String code = selectedCurrency(allCurrencyCodes());
        Currency currency = currencyOf(code);
        boolean cash = currency != null && state.cashCurrencies().contains(code);
        int bw = (CENTER_W - 3 * QUICK_GAP) / 4;
        int wy = by + HEADER_H + 2;
        for (int i = 0; i < WITHDRAW_QUICK.length; i++) {
            long units = currency == null ? 0 : WITHDRAW_QUICK[i] * pow10(currency.decimals());
            AtmButton b = addRenderableWidget(AtmButton.button(x + i * (bw + QUICK_GAP), wy, bw, QUICK_H,
                    Component.literal(String.valueOf(WITHDRAW_QUICK[i])), null, AtmButton.Style.DEFAULT,
                    () -> ClientPacketDistributor.sendToServer(new WithdrawPayload(code, units))));
            b.active = cash && units > 0 && units <= balanceOf(code);
        }
        int dy = by + DEPOSIT_Y + HEADER_H + 2;
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
        int x = centerX();
        int w = CENTER_W;
        int y = by + TRANSFER_Y + 20;
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
        int amountW = 80;
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
        field.setTextColor(AtmStyle.color(bad ? "qrm_bank.ledger_neg" : "qrm_bank.ink"));
    }

    private void send() {
        String code = selectedCurrency(allCurrencyCodes());
        long units = quietParse(code, transferAmount);
        if (units > 0) {
            ClientPacketDistributor.sendToServer(
                    new TransferPayload(recipientText.strip(), code, units, TransferNote.clean(noteText)));
        } else {
            lastResult = BankResult.INVALID_AMOUNT;
            resultAt = System.currentTimeMillis();
        }
    }

    private String format(String code, long amount) {
        Currency c = currencyOf(code);
        return c == null ? amount + " " + code : c.format(amount);
    }

    // ---- storico: geometria e scorrimento ------------------------------------------------------------------

    private int ledgerTop() {
        return by + 22;
    }

    private int ledgerBottom() {
        return selectedEntry() == null ? by + BODY_H - PAD : detailTop() - 4;
    }

    private AtmStatePayload.HistoryEntry selectedEntry() {
        return selected >= 0 && selected < state.history().size() ? state.history().get(selected) : null;
    }

    private int detailWidth() {
        return HIST_W - 2 * PAD - 12;
    }

    private int detailHeight() {
        AtmStatePayload.HistoryEntry e = selectedEntry();
        return e == null ? 0 : 8 + DETAIL_LINE * detailLines(e).size();
    }

    private int detailTop() {
        return by + BODY_H - PAD - detailHeight();
    }

    /** Righe del dettaglio: categoria, data completa, controparte e causale (a capo, al massimo tre righe). */
    private List<DetailLine> detailLines(AtmStatePayload.HistoryEntry e) {
        List<DetailLine> lines = new ArrayList<>();
        HistoryCategory category = HistoryCategory.of(e.reason(), e.signedAmount());
        lines.add(new DetailLine(categoryName(category, e.reason()), "qrm_bank.title"));
        lines.add(new DetailLine(DATE.format(Instant.ofEpochSecond(e.epochSecond())), "qrm_bank.ink_soft"));
        if (!e.counterparty().isEmpty()) {
            String key = e.signedAmount() > 0 ? "qrm_bank.atm.detail_from" : "qrm_bank.atm.detail_to";
            lines.add(new DetailLine(AtmStyle.fit(this.font, Component.translatable(key, e.counterparty()).getString(),
                    detailWidth()), "qrm_bank.ink"));
        }
        if (!e.note().isEmpty()) {
            for (String part : wrap("“" + e.note() + "”", detailWidth(), 3)) {
                lines.add(new DetailLine(part, "qrm_bank.cream"));
            }
        }
        return lines;
    }

    private record DetailLine(String text, String color) {}

    /** Spezza il testo in righe di al massimo {@code width} pixel; se non basta l'ultima riga finisce con i puntini. */
    private List<String> wrap(String text, int width, int maxLines) {
        List<String> out = new ArrayList<>();
        String rest = text.strip();
        while (!rest.isEmpty() && out.size() < maxLines) {
            if (AtmStyle.width(this.font, rest) <= width) {
                out.add(rest);
                rest = "";
                break;
            }
            String head = this.font.plainSubstrByWidth(rest, (int) (width / AtmStyle.TS));
            int cut = head.lastIndexOf(' ');
            if (cut > 0 && head.length() < rest.length()) head = head.substring(0, cut);
            if (head.isEmpty()) head = rest.substring(0, 1);
            out.add(head);
            rest = rest.substring(head.length()).stripLeading();
        }
        if (!rest.isEmpty() && !out.isEmpty()) {
            String last = out.get(out.size() - 1);
            out.set(out.size() - 1, AtmStyle.fit(this.font, last + " " + rest, width));
        }
        return out;
    }

    private int ledgerCapacity() {
        return Math.max(1, (ledgerBottom() - ledgerTop() - 8) / LEDGER_ROW);
    }

    private boolean ledgerScrolls() {
        return state.history().size() > ledgerCapacity();
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int tab = tabAt(event.x(), event.y());
            if (tab >= 0) {
                boolean toHistory = tab == 1;
                if (toHistory != historyPage) {
                    historyPage = toHistory;
                    selected = -1;
                    scroll = 0;
                    rebuildWidgets();
                }
                return true;
            }
        }
        if (historyPage && event.button() == 0 && !state.history().isEmpty()) {
            int cw = HIST_W - 2 * PAD;
            int w = cw - 8 - (ledgerScrolls() ? 6 : 0);
            int row = rowAt(event.x(), event.y(), rightX() + PAD + 4, w);
            if (row >= 0) {
                selected = selected == row ? -1 : row;
                clampScroll();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Indice dello storico sotto il mouse, oppure -1. */
    private int rowAt(double mx, double my, int x, int w) {
        if (mx < x || mx >= x + w) return -1;
        int rowsTop = ledgerTop() + 4;
        if (my < rowsTop) return -1;
        int visible = (int) ((my - rowsTop) / LEDGER_ROW);
        if (visible >= ledgerCapacity()) return -1;
        int index = Math.min(scroll, Math.max(0, state.history().size() - ledgerCapacity())) + visible;
        return index < state.history().size() ? index : -1;
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, state.history().size() - ledgerCapacity())));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (historyPage && scrollY != 0 && ledgerScrolls()) {
            int max = state.history().size() - ledgerCapacity();
            scroll = Math.max(0, Math.min(scroll - (int) Math.signum(scrollY), max));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    // ---- schede HOME / HISTORY nella barra del titolo ------------------------------------------------------

    private String tabName(int i) {
        return i == 0 ? Component.translatable("qrm_bank.atm.home").getString()
                : Component.translatable("qrm_bank.atm.history").getString();
    }

    /** X e larghezza della scheda i: a sinistra delle tacche del segnale e del tasto di spegnimento. */
    private int[] tabBox(int i) {
        int hw = AtmStyle.width(this.font, tabName(1));
        int ow = AtmStyle.width(this.font, tabName(0));
        int start = x0 + (PANEL_W - (ow + 12 + hw)) / 2;
        return i == 0 ? new int[] {start, ow} : new int[] {start + ow + 12, hw};
    }

    private int tabAt(double mx, double my) {
        if (choosing || my < y0 + 8 || my >= y0 + 8 + TITLE_H) return -1;
        for (int i = 0; i < 2; i++) {
            int[] t = tabBox(i);
            if (mx >= t[0] - 3 && mx < t[0] + t[1] + 3) return i;
        }
        return -1;
    }

    private void renderTabs(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int hover = tabAt(mouseX, mouseY);
        int ty = y0 + 8 + (TITLE_H - AtmStyle.TEXT_H) / 2 - 1;
        for (int i = 0; i < 2; i++) {
            boolean active = (i == 1) == historyPage;
            int[] t = tabBox(i);
            int color = active ? AtmStyle.color("qrm_bank.gold")
                    : AtmStyle.color(hover == i ? "qrm_bank.cream" : "qrm_bank.ink_soft");
            AtmStyle.text(g, this.font, tabName(i), t[0], ty, color);
            g.fill(t[0], ty + AtmStyle.TEXT_H + 3, t[0] + t[1], ty + AtmStyle.TEXT_H + 4,
                    active ? AtmStyle.GOLD : AtmStyle.alpha(AtmStyle.BLUE, hover == i ? 0xCC : 0x77));
        }
    }

    // ---- disegno -------------------------------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        AtmStyle.box(g, x0, y0, PANEL_W, PANEL_H, AtmStyle.LINE, AtmStyle.BG);
        AtmStyle.panel(g, x0 + MARGIN, y0 + 8, PANEL_W - 2 * MARGIN, TITLE_H);
        String title = this.title.getString();
        AtmStyle.text(g, this.font, title, x0 + MARGIN + 18, y0 + 8 + (TITLE_H - AtmStyle.TEXT_H) / 2 + 1,
                AtmStyle.color("qrm_bank.title"));
        renderTitleDecor(g);
        if (!choosing) renderTabs(g, mouseX, mouseY);
        if (choosing) {
            renderChoice(g, mouseX, mouseY);
        } else if (historyPage) {
            renderHistory(g, mouseX, mouseY);
        } else {
            renderAccount(g, mouseX, mouseY);
            renderCash(g);
            renderQuickHeaders(g);
            renderTransferPanel(g);
        }
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

    /** Nessuna banconota, una banconota o un mazzetto, secondo quanto si ha in mano. */
    private String cashIcon(String code, long held) {
        Currency currency = currencyOf(code);
        if (currency == null || held <= 0) return "cash_none";
        return held >= 500 * pow10(currency.decimals()) ? "cash_stack" : "cash";
    }

    /** Valore da mostrare ora: scorre dal precedente al nuovo ogni volta che cambia. */
    private static long shown(Map<String, ValueTween> tweens, String code, long actual) {
        long now = System.currentTimeMillis();
        ValueTween tween = tweens.computeIfAbsent(code, k -> new ValueTween(actual));
        tween.retarget(actual, now);
        return tween.value(now);
    }

    /** Spia e tacche del segnale nella barra del titolo: fisse, solo per dare l'aria di uno sportello vero. */
    private void renderTitleDecor(GuiGraphicsExtractor g) {
        int cy = y0 + 8 + TITLE_H / 2;
        int lx = x0 + MARGIN + 7;
        g.fill(lx, cy - 2, lx + 4, cy + 2, 0xFF3CE07A);
        int sx = x0 + PANEL_W - MARGIN - 18 - 6 - 14;
        int base = cy + 4;
        for (int i = 0; i < 4; i++) {
            int h = 3 + 2 * i;
            g.fill(sx + i * 4, base - h, sx + i * 4 + 2, base, i < 3 ? 0xFF3C9570 : 0xFF2A3C48);
        }
    }

    private void renderChoice(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        String text = Component.translatable("qrm_bank.atm.choose_account").getString();
        AtmStyle.text(g, this.font, text, x0 + (PANEL_W - AtmStyle.width(this.font, text)) / 2, by + 6,
                AtmStyle.color("qrm_bank.title"));
        int count = choiceCount();
        for (int i = 0; i < count; i++) {
            int[] r = choiceRect(i, count);
            String label = i == 0 ? Component.translatable("qrm_bank.atm.personal_account").getString()
                    : state.accounts().get(i - 1).label();
            int pad = 8;
            int textY = r[1] + r[3] - pad - AtmStyle.TEXT_H;
            if (i == 0) {
                int bh = r[3] - 2 * pad - AtmStyle.TEXT_H - 6;
                int bw = Math.min(r[2] - 2 * pad, bh * 34 / 44);
                drawBust(g, r[0] + (r[2] - bw) / 2, r[1] + pad, bw, bh, Math.max(30, bh * 42 / 44), mouseX, mouseY);
            } else if (uniformOf(state.accounts().get(i - 1).orgId()).isPresent()) {
                int bh = r[3] - 2 * pad - AtmStyle.TEXT_H - 6;
                int bw = Math.min(r[2] - 2 * pad, bh * 34 / 44);
                drawUniformBust(g, state.accounts().get(i - 1).orgId(), r[0] + (r[2] - bw) / 2, r[1] + pad, bw, bh,
                        Math.max(30, bh * 42 / 44), mouseX, mouseY);
            } else {
                int bw = r[2] - 2 * pad;
                int bh = Math.min(r[3] - 2 * pad - AtmStyle.TEXT_H - 6, bw * 148 / 448);
                drawBanner(g, state.accounts().get(i - 1).orgId(), r[0] + pad, r[1] + pad + (r[3] - 2 * pad - AtmStyle.TEXT_H - 6 - bh) / 2,
                        bw, bh);
            }
            String fitted = AtmStyle.fit(this.font, label, r[2] - 2 * pad);
            AtmStyle.text(g, this.font, fitted, r[0] + (r[2] - AtmStyle.width(this.font, fitted)) / 2, textY,
                    AtmStyle.color("qrm_bank.cream"));
        }
    }

    private void label(GuiGraphicsExtractor g, String text, int x, int y) {
        AtmStyle.text(g, this.font, text, x, y, AtmStyle.color("qrm_bank.label"), false);
    }

    /** Quanto il busto è spostato in verticale: respiro continuo e saltello a ogni operazione riuscita. */
    private float bustLift(long now) {
        float lift = 0.5f + 0.03f * (float) Math.sin(now / 1000.0 * 3.2);
        long age = now - resultAt;
        if (lastResult == BankResult.OK && age < 520) {
            lift -= 0.16f * (float) Math.abs(Math.sin(age / 520f * Math.PI * 2));
        }
        return lift;
    }

    /** Mezzo busto vivo: respira, si guarda attorno, segue il mouse se gli si passa vicino e salta a ogni operazione riuscita. */
    private void drawBust(GuiGraphicsExtractor g, int bxL, int byT, int w, int h, int scale, int mouseX, int mouseY) {
        drawBust(g, bxL, byT, w, h, scale, mouseX, mouseY, this.minecraft == null ? null : this.minecraft.player);
    }

    private void drawBust(GuiGraphicsExtractor g, int bxL, int byT, int w, int h, int scale, int mouseX, int mouseY,
            net.minecraft.world.entity.LivingEntity player) {
        if (player == null) return;
        long now = System.currentTimeMillis();
        double t = now / 1000.0;
        int cx = bxL + w / 2;
        int cy = byT + h / 2;
        float lookX = (float) (Math.sin(t * 1.1) * 26 + Math.sin(t * 2.7) * 8);
        float lookY = (float) (Math.sin(t * 1.9) * 6);
        boolean near = mouseX >= bxL - 30 && mouseX < bxL + w + 60 && mouseY >= byT - 20 && mouseY < byT + h + 30;
        float tx = near ? Math.max(-40, Math.min(40, mouseX - cx)) : lookX;
        float ty = near ? Math.max(-20, Math.min(20, mouseY - cy)) : lookY;
        float lift = bustLift(now);
        long age = now - resultAt;
        if (lastResult == BankResult.OK && age < 520) {
            float k = age / 520f;
            tx += 30f * (float) Math.sin(k * Math.PI * 4) * (1 - k);
        }
        InventoryScreen.extractEntityInInventoryFollowsMouse(g, bxL, byT, bxL + w, byT + h, scale, lift, cx + tx,
                cy - 14 + ty, player);
    }

    private final Map<String, java.util.Optional<net.minecraft.client.player.RemotePlayer>> uniforms = new HashMap<>();

    /**
     * Personaggio con la divisa dell'organizzazione: una skin 64x64 in
     * {@code assets/qrm_bank/textures/gui/atm/skin/<idOrganizzazione>.png}. Vuoto se la skin non c'è.
     */
    private java.util.Optional<net.minecraft.client.player.RemotePlayer> uniformOf(String orgId) {
        return uniforms.computeIfAbsent(orgId, id -> {
            var mc = this.minecraft;
            if (mc == null || mc.level == null || mc.player == null || !id.matches("[a-z0-9_]{1,32}")) {
                return java.util.Optional.empty();
            }
            Identifier tex = Identifier.fromNamespaceAndPath("qrm_bank", "textures/gui/atm/skin/" + id + ".png");
            if (mc.getResourceManager().getResource(tex).isEmpty()) return java.util.Optional.empty();
            var base = mc.player.getSkin();
            var skin = new net.minecraft.world.entity.player.PlayerSkin(
                    new net.minecraft.core.ClientAsset.ResourceTexture(tex, tex), null, null, base.model(), true);
            var profile = new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(("qrm-uniform:" + id).getBytes()),
                    "qrm_" + id);
            var dummy = new net.minecraft.client.player.RemotePlayer(mc.level, profile) {
                @Override
                public net.minecraft.world.entity.player.PlayerSkin getSkin() {
                    return skin;
                }
            };
            // Fuori dal mondo l'entità non riceve un id: serve a chi la disegna (id negativo, mai in conflitto).
            dummy.setId(-1 - (id.hashCode() & 0xFFFFF));
            return java.util.Optional.of(dummy);
        });
    }

    /** Busto con la divisa e, sopra, la testa del giocatore: stessa faccia, corpo dell'organizzazione. */
    private void drawUniformBust(GuiGraphicsExtractor g, String orgId, int bxL, int byT, int w, int h, int scale,
            int mouseX, int mouseY) {
        var uniform = uniformOf(orgId);
        if (uniform.isEmpty()) {
            drawBust(g, bxL, byT, w, h, scale, mouseX, mouseY);
            return;
        }
        drawBust(g, bxL, byT, w, h, scale, mouseX, mouseY, uniform.get());
        // La linea di taglio segue il collo: sta al centro del riquadro e scende o sale insieme al busto.
        int neck = byT + h / 2 + Math.round((bustLift(System.currentTimeMillis()) - 0.506f) * scale);
        g.enableScissor(bxL - 20, byT - 20, bxL + w + 20, neck);
        drawBust(g, bxL, byT, w, h, scale, mouseX, mouseY);
        g.disableScissor();
    }

    private static final Identifier DEFAULT_BANNER =
            Identifier.fromNamespaceAndPath("qrm_bank", "textures/gui/atm/company_banner.png");

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("qrm_bank");

    private record Banner(Identifier id, int width, int height) {}

    private final Map<String, Banner> banners = new HashMap<>();

    /**
     * Banner dell'organizzazione: {@code assets/qrm_bank/textures/gui/atm/banner/<idOrganizzazione>.png}, di qualsiasi
     * dimensione (viene ritagliato al centro per riempire il riquadro), altrimenti quello predefinito.
     */
    private Banner bannerOf(String orgId) {
        return banners.computeIfAbsent(orgId, id -> {
            if (this.minecraft != null && id.matches("[a-z0-9_]{1,32}")) {
                Identifier custom = Identifier.fromNamespaceAndPath("qrm_bank", "textures/gui/atm/banner/" + id + ".png");
                Banner found = readBanner(custom);
                if (found != null) return found;
            }
            Banner fallback = readBanner(DEFAULT_BANNER);
            return fallback != null ? fallback : new Banner(DEFAULT_BANNER, 448, 148);
        });
    }

    /** Legge solo la testata del PNG (larghezza e altezza): non serve decodificare l'immagine. */
    private Banner readBanner(Identifier id) {
        var resource = this.minecraft.getResourceManager().getResource(id);
        if (resource.isEmpty()) return null;
        try (var in = resource.get().open()) {
            byte[] head = in.readNBytes(24);
            if (head.length < 24 || head[1] != 'P' || head[2] != 'N' || head[3] != 'G') {
                LOGGER.warn("Banner {} is not a PNG file", id);
                return null;
            }
            int w = java.nio.ByteBuffer.wrap(head, 16, 4).getInt();
            int h = java.nio.ByteBuffer.wrap(head, 20, 4).getInt();
            if (w <= 0 || h <= 0) return null;
            return new Banner(id, w, h);
        } catch (java.io.IOException | RuntimeException e) {
            LOGGER.warn("Cannot read banner {}", id, e);
            return null;
        }
    }

    private void drawBanner(GuiGraphicsExtractor g, String orgId, int x, int y, int w, int h) {
        Banner banner = bannerOf(orgId);
        double want = (double) w / h;
        double have = (double) banner.width() / banner.height();
        int srcW = banner.width();
        int srcH = banner.height();
        if (have > want) srcW = (int) Math.round(banner.height() * want);
        else srcH = (int) Math.round(banner.width() / want);
        float u = (banner.width() - srcW) / 2f;
        float v = (banner.height() - srcH) / 2f;
        g.blit(RenderPipelines.GUI_TEXTURED, banner.id(), x, y, u, v, w, h, srcW, srcH, banner.width(), banner.height());
    }

    private void renderAccount(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = leftX();
        int p = 6;
        int inner = LEFT_W - 2 * p;
        AtmStyle.panel(g, x, by, LEFT_W, ACCOUNT_H);
        int bustW = 34;
        int bustH = 44;
        boolean company = !state.activeOrg().isEmpty();
        if (company) {
            drawBanner(g, state.activeOrg(), x, by, LEFT_W, 48);
        } else {
            drawBust(g, x + p, by + p, bustW, bustH, 42, mouseX, mouseY);
        }
        String name = state.holder().isEmpty() ? "?" : state.holder();
        int space = company ? -1 : name.indexOf(' ');
        String first = space < 0 ? name : name.substring(0, space);
        String rest = space < 0 ? "" : name.substring(space + 1);
        int nx = company ? x + p : x + p + bustW + 6;
        int nameY = company ? by + 54 : by + p + 6;
        int badgeRoom = state.vip() ? 14 : 0;
        int nw = LEFT_W - p - (nx - x) - badgeRoom;
        AtmStyle.text(g, this.font, AtmStyle.fit(this.font, first, nw), nx, nameY, AtmStyle.color("qrm_bank.ink"));
        if (!rest.isEmpty()) {
            AtmStyle.text(g, this.font, AtmStyle.fit(this.font, rest, nw), nx, by + p + 17,
                    AtmStyle.color("qrm_bank.ink_soft"));
        }
        if (state.vip()) {
            int bx2 = x + LEFT_W - p - 12;
            int vy = company ? by + 52 : by + p + 8;
            ThemeRenderer.draw(g, ICON + "vip", bx2, vy, 12, 12);
            if (mouseX >= bx2 && mouseX < bx2 + 12 && mouseY >= vy && mouseY < vy + 12) {
                g.setTooltipForNextFrame(this.font, Component.translatable("qrm_bank.atm.vip"), mouseX, mouseY);
            }
        }
        int dy = company ? 8 : 0;
        g.fill(x + p, by + 56 + dy, x + LEFT_W - p, by + 57 + dy, AtmStyle.LINE_SOFT);
        String code = selectedCurrency(allCurrencyCodes());
        int gold = AtmStyle.color("qrm_bank.gold");
        label(g, Component.translatable("qrm_bank.atm.account_id").getString(), x + p, by + 62 + dy);
        AtmStyle.text(g, this.font, AtmStyle.fit(this.font, state.accountCode(), inner), x + p, by + 72 + dy, gold);
        label(g, Component.translatable("qrm_bank.atm.balance_label").getString(), x + p, by + 83 + dy);
        String balance = code.isEmpty() ? "0" : format(code, shown(balanceTweens, code, balanceOf(code)));
        AtmStyle.text(g, this.font, AtmStyle.fit(this.font, balance, inner), x + p, by + 92 + dy, gold);
    }

    private void renderCash(GuiGraphicsExtractor g) {
        int x = leftX();
        int y = by + BODY_H - CASH_H;
        AtmStyle.panel(g, x, y, LEFT_W, CASH_H);
        String code = selectedCurrency(allCurrencyCodes());
        boolean cash = state.cashCurrencies().contains(code);
        ValueTween tween = cash ? cashTweens.computeIfAbsent(code, k -> new ValueTween(heldOf(code))) : null;
        long now = System.currentTimeMillis();
        int hop = 0;
        if (tween != null) {
            tween.retarget(heldOf(code), now);
            if (tween.running(now)) hop = (int) Math.round(2 * Math.abs(Math.sin(now / 60.0)));
        }
        long shownCash = cash ? tween.value(now) : 0;
        ThemeRenderer.draw(g, ICON + cashIcon(code, shownCash), x + 5, y + 4 - hop, 14, 14);
        int ty = y + (CASH_H - AtmStyle.TEXT_H) / 2 + 1;
        String labelText = Component.translatable("qrm_bank.atm.cash_label").getString();
        AtmStyle.text(g, this.font, labelText, x + 22, ty, AtmStyle.color("qrm_bank.label"));
        String value = cash ? format(code, shownCash) : "-";
        int vx = x + 22 + AtmStyle.width(this.font, labelText) + 6;
        AtmStyle.text(g, this.font, AtmStyle.fit(this.font, value, x + LEFT_W - 4 - vx), vx, ty,
                AtmStyle.color(cash ? "qrm_bank.gold" : "qrm_bank.ink_soft"));
    }

    private void renderQuickHeaders(GuiGraphicsExtractor g) {
        header(g, by, ICON + "withdraw", "qrm_bank.atm.quick_withdraw");
        header(g, by + DEPOSIT_Y, ICON + "deposit", "qrm_bank.atm.quick_deposit");
    }

    /** Titolo di sezione: solo testo, con l'icona subito a destra. */
    private void header(GuiGraphicsExtractor g, int y, String icon, String key) {
        int x = centerX();
        String text = Component.translatable(key).getString();
        AtmStyle.text(g, this.font, text, x + 2, y + (HEADER_H - AtmStyle.TEXT_H) / 2 + 1,
                AtmStyle.color("qrm_bank.cream"), false);
        ThemeRenderer.draw(g, icon, x + 2 + AtmStyle.width(this.font, text) + 4, y - 1, 16, 16);
    }

    private void renderTransferPanel(GuiGraphicsExtractor g) {
        int x = centerX();
        int y = by + TRANSFER_Y;
        String text = Component.translatable("qrm_bank.atm.transfer").getString();
        AtmStyle.text(g, this.font, text, x + (CENTER_W - AtmStyle.width(this.font, text)) / 2, y + 6, AtmStyle.color("qrm_bank.title"),
                false);
    }

    private void renderStatus(GuiGraphicsExtractor g) {
        int y = by + BODY_H + GAP;
        int w = PANEL_W - 2 * MARGIN;
        long age = System.currentTimeMillis() - resultAt;
        AtmStyle.panel(g, bx, y, w, STATUS_H);
        if (lastResult == null) {
            ThemeRenderer.draw(g, ICON + "coin", bx + 3, y - 1, 16, 16);
            statusText(g, y, Component.translatable("qrm_bank.atm.ready").getString(), AtmStyle.color("qrm_bank.lcd"), w - 30);
            return;
        }
        String message = Component.translatable("qrm_bank.result." + lastResult.name().toLowerCase(Locale.ROOT)).getString();
        if (lastResult == BankResult.OK) {
            float flash = Math.max(0f, 1f - age / 700f);
            if (flash > 0) g.fill(bx + 2, y + 2, bx + w - 2, y + STATUS_H - 2, ((int) (flash * 0x50) << 24) | 0x3CE07A);
            int pop = age < 250 ? (int) (6 * (1 - age / 250f)) : 0;
            ThemeRenderer.draw(g, ICON + "ok", bx + 3 - pop / 2, y - 1 - pop / 2, 16 + pop, 16 + pop);
            String stamp = Component.translatable("qrm_bank.atm.stamp").getString();
            int stampW = AtmStyle.width(this.font, stamp) + 10;
            statusText(g, y, message, AtmStyle.color("qrm_bank.positive"), w - 30 - (age < 2000 ? stampW + 6 : 0));
            if (age < 2000) drawStamp(g, bx + w - stampW - 3, y + 2, stampW, STATUS_H - 4, stamp, age);
            return;
        }
        // Errore: riga da terminale che si scrive da sola, con il cursore che lampeggia per qualche secondo.
        int tint = (int) (Math.min(1f, age / 150f) * 0x70);
        g.fill(bx + 2, y + 2, bx + w - 2, y + STATUS_H - 2, (tint << 24) | 0x5A0F0F);
        ThemeRenderer.draw(g, ICON + "error", bx + 3, y - 1, 16, 16);
        int typed = (int) Math.min(message.length(), age / 18);
        boolean cursor = age < 3000 && (age / 350) % 2 == 0;
        String line = "> " + message.substring(0, typed) + (cursor ? "_" : "");
        statusText(g, y, line, AtmStyle.color("qrm_bank.negative"), w - 30);
    }

    private void statusText(GuiGraphicsExtractor g, int y, String text, int color, int room) {
        AtmStyle.text(g, this.font, AtmStyle.fit(this.font, text, room), bx + 22, y + (STATUS_H - AtmStyle.TEXT_H) / 2 + 1, color, false);
    }

    /** Timbro verde con il bordo, che sparisce in una frazione di secondo dopo un paio di secondi. */
    private void drawStamp(GuiGraphicsExtractor g, int x, int y, int w, int h, String text, long age) {
        float fade = age > 1500 ? Math.max(0f, 1f - (age - 1500) / 500f) : 1f;
        int a = Math.max(4, (int) (fade * 255));
        int rgb = 0x7FE39A;
        int c = (a << 24) | rgb;
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
        AtmStyle.text(g, this.font, text, x + 5, y + (h - AtmStyle.TEXT_H) / 2 + 1, c, false);
    }

    private static String reasonName(String reason) {
        String key = "qrm_bank.reason." + reason.replace('.', '_');
        return Language.getInstance().has(key) ? Language.getInstance().getOrDefault(key) : reason;
    }

    private void renderHistory(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int px = rightX();
        AtmStyle.panel(g, px, by, HIST_W, BODY_H);
        String text = Component.translatable("qrm_bank.atm.history").getString();
        AtmStyle.text(g, this.font, text, px + (HIST_W - AtmStyle.width(this.font, text)) / 2, by + 7, AtmStyle.color("qrm_bank.title"),
                false);
        int ledgerTop = ledgerTop();
        int ledgerBottom = ledgerBottom();
        int cx = px + PAD;
        int cw = HIST_W - 2 * PAD;
        g.fill(cx, ledgerTop, cx + cw, ledgerBottom, 0x33000000);
        if (state.history().isEmpty()) {
            ThemeRenderer.draw(g, ICON + "account", cx + (cw - 16) / 2, ledgerTop + (ledgerBottom - ledgerTop - 16) / 2, 16, 16);
            return;
        }
        int ink = AtmStyle.color("qrm_bank.ink");
        int inkSoft = AtmStyle.color("qrm_bank.ink_soft");
        boolean scrolls = ledgerScrolls();
        int x = cx + 4;
        int w = cw - 8 - (scrolls ? 6 : 0);
        int y = ledgerTop + 4;
        int rows = ledgerCapacity();
        int first = Math.min(scroll, Math.max(0, state.history().size() - rows));
        int hover = rowAt(mouseX, mouseY, x, w);
        for (int i = first; i < state.history().size() && i < first + rows; i++) {
            AtmStatePayload.HistoryEntry e = state.history().get(i);
            if (i == selected) {
                g.fill(x - 1, y, x + w + 1, y + LEDGER_ROW - 1, 0x55E2B24A);
            } else if (i == hover) {
                g.fill(x - 1, y, x + w + 1, y + LEDGER_ROW - 1, 0x22FFFFFF);
            }
            g.fill(x - 1, y + LEDGER_ROW - 1, x + w + 1, y + LEDGER_ROW, 0x18FFFFFF);
            HistoryCategory category = HistoryCategory.of(e.reason(), e.signedAmount());
            ThemeRenderer.draw(g, ICON + category.icon(), x, y + (LEDGER_ROW - 16) / 2 - 1, 16, 16);
            int tx = x + 19;
            int tw = w - 19;
            String amount = (e.signedAmount() > 0 ? "+" : "") + format(e.currency(), e.signedAmount());
            AtmStyle.text(g, this.font, AtmStyle.fit(this.font, amount, tw), tx, y + 1, AtmStyle.color(
                    e.signedAmount() > 0 ? "qrm_bank.ledger_pos" : "qrm_bank.ledger_neg"), false);
            String who = e.counterparty().isEmpty() ? categoryName(category, e.reason()) : e.counterparty();
            AtmStyle.text(g, this.font, AtmStyle.fit(this.font, who, tw), tx, y + 10, e.counterparty().isEmpty() ? inkSoft : ink,
                    false);
            y += LEDGER_ROW;
        }
        if (scrolls) drawScrollbar(g, cx + cw - 7, ledgerTop, ledgerBottom);
        AtmStatePayload.HistoryEntry picked = selectedEntry();
        if (picked != null) {
            int dy = detailTop();
            g.fill(cx, dy, cx + cw, dy + detailHeight(), 0x33000000);
            int ty = dy + 5;
            for (DetailLine line : detailLines(picked)) {
                AtmStyle.text(g, this.font, line.text(), cx + 6, ty, AtmStyle.color(line.color()), false);
                ty += DETAIL_LINE;
            }
        }
    }

    /** Nome della categoria; per «altro» il motivo scritto da chi ha mosso il denaro, se c'è. */
    private static String categoryName(HistoryCategory category, String reason) {
        if (category == HistoryCategory.OTHER && !reason.isEmpty()) return reasonName(reason);
        return Component.translatable("qrm_bank.category." + category.key()).getString();
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
