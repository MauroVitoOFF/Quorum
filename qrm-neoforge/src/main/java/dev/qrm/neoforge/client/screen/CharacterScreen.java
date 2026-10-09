package dev.qrm.neoforge.client.screen;

import dev.qrm.client.api.screen.QrmScreen;
import dev.qrm.client.api.theme.QrmTheme;
import dev.qrm.client.api.theme.ThemeManifest;
import dev.qrm.client.api.theme.ThemeRenderer;
import dev.qrm.client.api.widget.ThemedButton;
import dev.qrm.client.api.widget.ThemedToggle;
import dev.qrm.neoforge.net.ArchiveCharacterPayload;
import dev.qrm.neoforge.net.CharacterAction;
import dev.qrm.neoforge.net.CharacterCard;
import dev.qrm.neoforge.net.CharacterResultPayload;
import dev.qrm.neoforge.net.CharacterRosterPayload;
import dev.qrm.neoforge.net.CreateCharacterPayload;
import dev.qrm.neoforge.net.ResultCode;
import dev.qrm.neoforge.net.SelectCharacterPayload;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Schermata Personaggi: carosello di carte documento, creazione e archiviazione con conferma. */
public final class CharacterScreen extends QrmScreen {
    private enum Mode { LIST, CREATE, CONFIRM_ARCHIVE }

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int EDIT_BOX_HEIGHT = 18;
    private static final int NAME_MAX_LENGTH = 64;
    private static final int DATE_MAX_LENGTH = 10;
    private static final int VISIBLE_RADIUS = 2;
    private static final String ELLIPSIS = "…";
    private static final int FIELD_H = EDIT_BOX_HEIGHT;
    private static final int ROW_GAP = 4;
    /** Nome, cognome, nascita, genere, firma. */
    private static final int FORM_ROWS = 5;
    private static final int PHOTO_W = 60;
    private static final int TOGGLE_W = 28;
    private static final int FIELD_TEXT_INSET = 5;
    /**
     * Inquadratura della testa 3D: pixel per blocco (la testa è larga ~0,5 blocchi) e spostamento verticale che porta
     * l'altezza degli occhi (~1,62 blocchi) al centro della cornice, partendo dal centro del corpo (0,9).
     */
    private static final int HEAD_SCALE = 80;
    private static final float HEAD_OFFSET_Y = 0.72F;
    /** Dimensioni interne della cornice foto di riferimento (creazione) per cui {@link #HEAD_SCALE} è tarato. */
    private static final int HEAD_REF_W = 54;
    private static final int HEAD_REF_H = 64;
    private static final String[] FORM_LABEL_KEYS = {"qrm.character.id.first", "qrm.character.id.last",
            "qrm.character.id.birth", "qrm.character.id.gender", "qrm.character.id.signature"};
    /** Altezza minima della carta (rispetto a {@code cardHeight}) quando lo schermo è basso. */
    private static final double MIN_CARD_HEIGHT_SCALE = 0.7;

    private final CarouselModel model = new CarouselModel();
    private CharacterRosterPayload roster;
    private List<CarouselModel.Entry> entries;
    private Mode mode;
    private boolean busy;
    private String errorKey;
    private boolean lastResultOk;
    /** Le azioni dei pulsanti segnano la schermata sporca: la ricostruzione avviene a fine evento, mai durante. */
    private boolean dirty;

    private UUID archiveTarget;
    private String archiveName = "";

    private String firstText = "";
    private String lastText = "";
    private String dateText = "";
    /** Genere mostrato sulla carta: solo grafico, non viene inviato né salvato. */
    private boolean male = true;
    private EditBox firstBox;
    private final EditBox[] boxes = new EditBox[3];
    private ThemedButton confirmCancelButton;
    /**
     * Vero se la conferma è stata aperta da tastiera: Spazio/Invio vengono ignorati finché un tasto non viene
     * rilasciato, così la ripetizione del tasto che l'ha aperta non può attivare nulla nella conferma.
     */
    private boolean confirmKeyGuard;
    /** Vero mentre un tasto premuto viene inoltrato ai widget (per sapere se un'azione nasce dalla tastiera). */
    private boolean inKeyDispatch;

    public CharacterScreen(CharacterRosterPayload initial) {
        super(Component.translatable("qrm.character.title"));
        applyRoster(initial);
        this.mode = initial.cards().isEmpty() && initial.slots() > 0 ? Mode.CREATE : Mode.LIST;
    }

    // ---- rete ----------------------------------------------------------------------------------------------

    public void onRoster(CharacterRosterPayload p) {
        applyRoster(p);
        busy = false;
        if (p.cards().isEmpty() && p.slots() > 0) {
            setMode(Mode.CREATE);
        } else if (mode == Mode.CREATE && lastResultOk) {
            setMode(Mode.LIST);
        }
        if (mode == Mode.CONFIRM_ARCHIVE && !containsCard(archiveTarget)) setMode(Mode.LIST);
        rebuildNow();
    }

    public void onResult(CharacterResultPayload p) {
        busy = false;
        if (p.action() != CharacterAction.ROSTER) {
            lastResultOk = p.code() == ResultCode.OK;
            errorKey = p.code() == ResultCode.OK ? null : "qrm.character.result." + p.code().name();
            if (p.action() == CharacterAction.CREATE && p.code() == ResultCode.OK) setMode(Mode.LIST);
            if (p.action() == CharacterAction.SELECT && p.code() == ResultCode.OK) {
                // Gioca riuscito: si torna in gioco. Il roster che segue ha openScreen=false e non la riapre.
                onClose();
                return;
            }
        }
        rebuildNow();
    }

    private void applyRoster(CharacterRosterPayload p) {
        this.roster = p;
        model.update(p.cards(), p.slots(), p.activeId());
        this.entries = model.entries();
    }

    private boolean containsCard(UUID id) {
        if (id == null) return false;
        for (CharacterCard c : roster.cards()) {
            if (id.equals(c.id())) return true;
        }
        return false;
    }

    private void send(CustomPacketPayload payload) {
        errorKey = null;
        lastResultOk = false;
        busy = true;
        ClientPacketDistributor.sendToServer(payload);
        dirty = true;
    }

    private void setMode(Mode m) {
        if (m == Mode.CREATE && mode != Mode.CREATE) {
            firstText = "";
            lastText = "";
            dateText = "";
            male = true;
        }
        if (m != Mode.CONFIRM_ARCHIVE) {
            archiveTarget = null;
            archiveName = "";
            confirmKeyGuard = false;
        }
        mode = m;
    }

    private void rebuildNow() {
        dirty = false;
        rebuildWidgets();
    }

    private void flushRebuild() {
        if (dirty && this.minecraft.gui.screen() == this) rebuildNow();
        dirty = false;
    }

    // ---- azioni --------------------------------------------------------------------------------------------

    private CarouselModel.Entry selectedEntry() {
        return entries.get(model.selectedIndex());
    }

    private void play() {
        if (busy || mode != Mode.LIST) return;
        CarouselModel.Entry e = selectedEntry();
        if (e.kind() == CarouselModel.Kind.CARD) send(new SelectCharacterPayload(e.card().id()));
    }

    private void askArchive() {
        if (busy || mode != Mode.LIST) return;
        CarouselModel.Entry e = selectedEntry();
        if (e.kind() != CarouselModel.Kind.CARD) return;
        errorKey = null;
        setMode(Mode.CONFIRM_ARCHIVE);
        archiveTarget = e.card().id();
        archiveName = fullName(e.card().firstName(), e.card().lastName());
        confirmKeyGuard = inKeyDispatch;
        dirty = true;
    }

    private void confirmArchive() {
        if (busy || archiveTarget == null) return;
        UUID target = archiveTarget;
        setMode(Mode.LIST);
        send(new ArchiveCharacterPayload(target));
    }

    private void backToList() {
        errorKey = null;
        setMode(Mode.LIST);
        dirty = true;
    }

    private void openCreate() {
        if (busy) return;
        errorKey = null;
        setMode(Mode.CREATE);
        dirty = true;
    }

    private void submitCreate() {
        if (busy) return;
        CreateForm.Validation v = CreateForm.validate(firstText, lastText, dateText, LocalDate.now());
        if (!v.valid()) {
            errorKey = v.errorKey();
            dirty = true;
            return;
        }
        CreateForm.Parsed p = v.parsed();
        send(new CreateCharacterPayload(p.firstName(), p.lastName(), p.year(), p.month(), p.day()));
    }

    private void cancelCreate() {
        if (roster.cards().isEmpty()) {
            onClose();
        } else {
            backToList();
        }
    }

    // ---- metriche e layout ---------------------------------------------------------------------------------

    private static ThemeManifest theme() {
        return QrmTheme.current();
    }

    private static int pad() {
        return Math.max(0, (int) Math.round(theme().metric("padding")));
    }

    private static int cardW() {
        return Math.max(1, (int) Math.round(theme().metric("cardWidth")));
    }

    private static int cardH() {
        return Math.max(1, (int) Math.round(theme().metric("cardHeight")));
    }

    private static double carouselScale() {
        return theme().metric("carouselScale");
    }

    private static int buttonH() {
        return Math.max(1, theme().texture("button.normal").height());
    }

    private int lineH() {
        return this.font.lineHeight;
    }

    /** Primo y utile sotto l'intestazione (titolo + slot), lasciando una riga per la striscia d'errore. */
    private int contentTop() {
        int pad = pad();
        int headerBottom = pad + lineH() + pad / 2 + lineH() + pad;
        return headerBottom + lineH() + pad;
    }

    /**
     * Layout verticale del modo LIST: titolo, slot, striscia d'errore, carta, pulsanti, attesa e suggerimento
     * formano un unico blocco centrato in {@code height}; se non entra, la carta si accorcia fino a
     * {@link #MIN_CARD_HEIGHT_SCALE}.
     */
    private record ListLayout(int titleY, int slotsY, int errorY, int cardTop, int cardH, double faceScale,
            int buttonsY, int busyY, int hintY) {}

    private ListLayout listLayout() {
        int pad = pad();
        int lh = lineH();
        int fixed = pad + lh + pad / 2 + lh + pad   // titolo + slot
                + lh + pad                          // striscia d'errore
                + pad + buttonH()                   // pulsanti
                + pad / 2 + lh + pad / 2 + lh       // attesa + suggerimento
                + pad;                              // margine inferiore
        int fullH = cardH();
        int minH = Math.max(1, (int) Math.ceil(fullH * MIN_CARD_HEIGHT_SCALE));
        int ch = Math.max(minH, Math.min(fullH, this.height - fixed));
        int top = Math.max(0, (this.height - (fixed + ch)) / 2);
        int titleY = top + pad;
        int slotsY = titleY + lh + pad / 2;
        int errorY = slotsY + lh + pad;
        int cardTop = errorY + lh + pad;
        int buttonsY = cardTop + ch + pad;
        int busyY = buttonsY + buttonH() + pad / 2;
        int hintY = busyY + lh + pad / 2;
        return new ListLayout(titleY, slotsY, errorY, cardTop, ch, (double) ch / fullH, buttonsY, busyY, hintY);
    }

    private int sideW() {
        return Math.max(1, (int) Math.round(cardW() * carouselScale()));
    }

    private int gap() {
        return Math.max(0, (int) Math.round(theme().metric("carouselGap")));
    }

    /**
     * Rettangolo della carta a distanza {@code offset} dalla selezionata (unica fonte per disegno e clic), o
     * {@code null} se cade interamente fuori dallo schermo. Fra i bordi di carte adiacenti c'è sempre {@code carouselGap}.
     */
    private Rect cardRect(ListLayout l, int offset) {
        int cw = cardW();
        int x0 = this.width / 2 - cw / 2;
        if (offset == 0) return new Rect(x0, l.cardTop(), cw, l.cardH());
        int sw = sideW();
        int sh = Math.max(1, (int) Math.round(l.cardH() * carouselScale()));
        int n = Math.abs(offset);
        int x = offset > 0
                ? x0 + cw + gap() + (n - 1) * (sw + gap())
                : x0 - n * (sw + gap());
        if (x + sw <= 0 || x >= this.width) return null;
        return new Rect(x, l.cardTop() + (l.cardH() - sh) / 2, sw, sh);
    }

    private int panelW() {
        return 2 * cardW();
    }

    // ---- layout della carta d'identità (modo CREATE) ------------------------------------------------------

    private int idCardW() {
        int wanted = Math.max(1, (int) Math.round(theme().metric("idCardWidth")));
        return Math.max(1, Math.min(wanted, this.width - 2 * pad()));
    }

    /** Barra del titolo della carta: margine, riga di testo, margine; il filetto sta subito sotto. */
    private int titleBlockH() {
        return pad() + lineH() + pad();
    }

    private int rowH() {
        return FIELD_H + ROW_GAP;
    }

    private int rowsH() {
        return FORM_ROWS * rowH() - ROW_GAP;
    }

    private int idCardH() {
        return titleBlockH() + 1 + pad() + rowsH() + pad();
    }

    /** Intestazione, carta e pulsanti formano un unico blocco centrato. */
    private int createBlockH() {
        return lineH() + pad() + idCardH() + pad() + buttonH();
    }

    private int createBlockTop() {
        return Math.max(lineH() + 2 * pad(), (this.height - createBlockH()) / 2);
    }

    private Rect idCard() {
        int w = idCardW();
        return new Rect((this.width - w) / 2, createBlockTop() + lineH() + pad(), w, idCardH());
    }

    private int rowsTop(Rect card) {
        return card.y() + titleBlockH() + 1 + pad();
    }

    private int labelW() {
        int w = 0;
        for (String key : FORM_LABEL_KEYS) w = Math.max(w, this.font.width(upper(key)));
        return w;
    }

    private int fieldX(Rect card) {
        return card.x() + pad() + PHOTO_W + pad() + labelW() + pad();
    }

    private Rect fieldRect(Rect card, int row) {
        int x = fieldX(card);
        return new Rect(x, rowsTop(card) + row * rowH(), Math.max(1, card.x() + card.w() - pad() - x), FIELD_H);
    }

    private Rect[] createButtons(Rect card) {
        int bw = Math.max(1, (card.w() - pad()) / 2);
        int y = card.y() + card.h() + pad();
        return new Rect[] {
                new Rect(card.x(), y, bw, buttonH()),
                new Rect(card.x() + bw + pad(), y, bw, buttonH())
        };
    }

    private List<FormattedCharSequence> confirmLines() {
        return this.font.split(Component.translatable("qrm.character.confirm.text"), Math.max(1, panelW() - 2 * pad()));
    }

    private Rect confirmPanel() {
        int pad = pad();
        int pw = panelW();
        int ph = pad + lineH() + pad / 2 + lineH() + pad / 2 + confirmLines().size() * lineH() + pad + buttonH() + pad;
        int y = Math.max(contentTop(), (this.height - ph) / 2);
        return new Rect((this.width - pw) / 2, y, pw, ph);
    }

    /** Due pulsanti affiancati in fondo al pannello. */
    private Rect[] panelButtons(Rect panel) {
        int pad = pad();
        int inner = panel.w() - 2 * pad;
        int bw = Math.max(1, (inner - pad) / 2);
        int y = panel.y() + panel.h() - pad - buttonH();
        return new Rect[] {
                new Rect(panel.x() + pad, y, bw, buttonH()),
                new Rect(panel.x() + pad + bw + pad, y, bw, buttonH())
        };
    }

    private int errorY() {
        return switch (mode) {
            case LIST -> listLayout().errorY();
            case CREATE -> createBlockTop() - pad() - lineH();
            case CONFIRM_ARCHIVE -> confirmPanel().y() - pad() - lineH();
        };
    }

    // ---- widget --------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        firstBox = null;
        java.util.Arrays.fill(boxes, null);
        confirmCancelButton = null;
        switch (mode) {
            case LIST -> initList();
            case CREATE -> initCreate();
            case CONFIRM_ARCHIVE -> initConfirm();
        }
    }

    private void initList() {
        ListLayout l = listLayout();
        Rect card = cardRect(l, 0);
        int pad = pad();
        int y = l.buttonsY();
        CarouselModel.Entry e = selectedEntry();
        switch (e.kind()) {
            case CARD -> {
                int bw = Math.max(1, (card.w() - pad) / 2);
                addButton(new Rect(card.x(), y, bw, buttonH()), "qrm.character.play", this::play, ThemedButton.Style.CONFIRM);
                addButton(new Rect(card.x() + bw + pad, y, bw, buttonH()), "qrm.character.archive", this::askArchive,
                        ThemedButton.Style.DANGER);
            }
            case NEW -> addButton(new Rect(card.x(), y, card.w(), buttonH()), "qrm.character.new", this::openCreate,
                    ThemedButton.Style.CONFIRM);
            case LOCKED -> { }
        }
    }

    private void initCreate() {
        Rect card = idCard();
        String[] hints = {null, null, "qrm.character.id.birth.hint"};
        String[] values = {firstText, lastText, dateText};
        int[] maxLengths = {NAME_MAX_LENGTH, NAME_MAX_LENGTH, DATE_MAX_LENGTH};
        List<Consumer<String>> responders = List.of(v -> firstText = v, v -> lastText = v, v -> dateText = v);
        for (int i = 0; i < boxes.length; i++) {
            boxes[i] = addBox(fieldRect(card, i), maxLengths[i], values[i], responders.get(i), hints[i]);
        }
        firstBox = boxes[0];
        Rect g = fieldRect(card, 3);
        addRenderableWidget(new ThemedToggle(g.x(), g.y(), TOGGLE_W, FIELD_H, Component.translatable("qrm.character.id.male"),
                () -> male, () -> male = true));
        addRenderableWidget(new ThemedToggle(g.x() + TOGGLE_W + pad() / 2 + 2, g.y(), TOGGLE_W, FIELD_H,
                Component.translatable("qrm.character.id.female"), () -> !male, () -> male = false));
        Rect[] b = createButtons(card);
        addButton(b[0], "qrm.character.id.confirm", this::submitCreate, ThemedButton.Style.CONFIRM);
        addButton(b[1], "qrm.character.id.cancel", this::cancelCreate, ThemedButton.Style.DANGER);
    }

    /** Campo di testo senza bordo vanilla: lo sfondo lo disegna il tema (vedi {@link #drawField}). */
    private EditBox addBox(Rect field, int maxLength, String value, Consumer<String> responder, String hintKey) {
        EditBox box = new EditBox(this.font, field.x() + FIELD_TEXT_INSET, field.y() + (field.h() - lineH()) / 2,
                Math.max(1, field.w() - 2 * FIELD_TEXT_INSET), lineH(), Component.empty());
        box.setBordered(false);
        box.setTextColor(ThemeRenderer.color("text"));
        box.setMaxLength(maxLength);
        box.setValue(value);
        box.setResponder(responder);
        if (hintKey != null) box.setHint(Component.translatable(hintKey).withColor(ThemeRenderer.color("muted")));
        return addRenderableWidget(box);
    }

    private void initConfirm() {
        Rect[] b = panelButtons(confirmPanel());
        addButton(b[0], "qrm.character.confirm.yes", this::confirmArchive, ThemedButton.Style.DANGER);
        confirmCancelButton = addButton(b[1], "qrm.character.confirm.no", this::backToList, ThemedButton.Style.CONFIRM);
    }

    private ThemedButton addButton(Rect r, String key, Runnable action) {
        return addButton(r, key, action, ThemedButton.Style.DEFAULT);
    }

    private ThemedButton addButton(Rect r, String key, Runnable action, ThemedButton.Style style) {
        ThemedButton button = addRenderableWidget(new ThemedButton(r.x(), r.y(), r.w(), r.h(),
                Component.translatable(key), action, style));
        button.active = !busy;
        return button;
    }

    @Override
    protected void setInitialFocus() {
        if (mode == Mode.CREATE && firstBox != null) {
            setInitialFocus(firstBox);
        } else if (mode == Mode.CONFIRM_ARCHIVE && confirmCancelButton != null) {
            // mai il pulsante distruttivo: il fuoco iniziale va su Annulla
            setInitialFocus(confirmCancelButton);
        } else {
            super.setInitialFocus();
        }
    }

    // ---- input ---------------------------------------------------------------------------------------------

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (mode == Mode.LIST && !busy) {
            int key = event.key();
            boolean handled = true;
            if (key == GLFW.GLFW_KEY_LEFT) {
                model.previous();
            } else if (key == GLFW.GLFW_KEY_RIGHT) {
                model.next();
            } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                CarouselModel.Kind kind = selectedEntry().kind();
                if (kind == CarouselModel.Kind.CARD) play();
                else if (kind == CarouselModel.Kind.NEW) openCreate();
            } else {
                handled = false;
            }
            if (handled) {
                rebuildNow();
                return true;
            }
        }
        if (mode == Mode.CONFIRM_ARCHIVE && confirmKeyGuard && isActivationKey(event.key())) {
            return true; // ripetizione del tasto che ha aperto la conferma: ignorata
        }
        boolean result;
        inKeyDispatch = true;
        try {
            result = super.keyPressed(event);
        } finally {
            inKeyDispatch = false;
        }
        flushRebuild();
        return result;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (isActivationKey(event.key())) confirmKeyGuard = false;
        return super.keyReleased(event);
    }

    private static boolean isActivationKey(int key) {
        return key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (mode == Mode.LIST && scrollY != 0) {
            if (scrollY > 0) model.previous();
            else model.next();
            rebuildNow();
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        flushRebuild();
        if (handled) return true;
        if (mode == Mode.CREATE && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Rect card = idCard();
            for (int i = 0; i < boxes.length; i++) {
                if (boxes[i] != null && fieldRect(card, i).contains(event.x(), event.y())) {
                    setFocused(boxes[i]);
                    return true;
                }
            }
        }
        if (mode == Mode.LIST && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            ListLayout l = listLayout();
            if (cardRect(l, 0).contains(event.x(), event.y())) return false;
            int sel = model.selectedIndex();
            for (int i = 0; i < entries.size(); i++) {
                int offset = i - sel;
                if (offset == 0 || Math.abs(offset) > VISIBLE_RADIUS) continue;
                Rect r = cardRect(l, offset);
                if (r != null && r.contains(event.x(), event.y())) {
                    model.select(i);
                    rebuildNow();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void tick() {
        flushRebuild();
    }


    // ---- disegno -------------------------------------------------------------------------------------------


    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int pad = pad();
        int cx = this.width / 2;
        ListLayout l = mode == Mode.LIST ? listLayout() : null;
        int titleY = l != null ? l.titleY() : pad;
        int slotsY = l != null ? l.slotsY() : pad + lineH() + pad / 2;
        if (mode != Mode.CREATE) {
            g.centeredText(this.font, upper("qrm.character.title"), cx, titleY, ThemeRenderer.color("text"));
            g.centeredText(this.font, Component.translatable("qrm.character.slots", roster.cards().size(), roster.slots()),
                    cx, slotsY, ThemeRenderer.color("muted"));
        }

        switch (mode) {
            case LIST -> drawList(g, l, mouseX, mouseY);
            case CREATE -> drawCreate(g, mouseX, mouseY);
            case CONFIRM_ARCHIVE -> drawConfirm(g);
        }

        if (errorKey != null) drawErrorStrip(g, Component.translatable(errorKey));
        int hintY = l != null ? l.hintY() : this.height - pad - lineH();
        int busyY = l != null ? l.busyY() : hintY - lineH() - pad / 2;
        if (busy) {
            g.centeredText(this.font, Component.translatable("qrm.character.busy"), cx, busyY,
                    ThemeRenderer.color("muted"));
        }
        if (l != null) {
            g.centeredText(this.font, Component.translatable("qrm.character.hint"), cx, hintY,
                    ThemeRenderer.color("muted"));
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void drawErrorStrip(GuiGraphicsExtractor g, Component message) {
        int pad = pad();
        String text = fit(message.getString(), Math.max(1, this.width - 4 * pad));
        int tw = this.font.width(text);
        int y = errorY();
        int cx = this.width / 2;
        g.fill(cx - tw / 2 - pad, y - pad / 2, cx + tw / 2 + pad, y + lineH() + pad / 2, ThemeRenderer.color("scrim"));
        g.centeredText(this.font, text, cx, y, ThemeRenderer.color("error"));
    }

    private void drawList(GuiGraphicsExtractor g, ListLayout l, int mouseX, int mouseY) {
        List<CarouselModel.Entry> all = this.entries;
        int sel = model.selectedIndex();
        for (int d = VISIBLE_RADIUS; d >= 1; d--) {
            for (int sign = -1; sign <= 1; sign += 2) {
                int i = sel + sign * d;
                if (i < 0 || i >= all.size()) continue;
                Rect r = cardRect(l, sign * d);
                if (r != null) drawCard(g, all.get(i), r, false, l.faceScale(), mouseX, mouseY);
            }
        }
        drawCard(g, all.get(sel), cardRect(l, 0), true, l.faceScale(), mouseX, mouseY);
    }

    private void drawCard(GuiGraphicsExtractor g, CarouselModel.Entry e, Rect r, boolean selected, double faceScale,
            int mouseX, int mouseY) {
        switch (e.kind()) {
            case CARD -> {
                ThemeRenderer.draw(g, selected ? "character.card.selected" : "character.card", r.x(), r.y(), r.w(), r.h());
                CharacterCard c = e.card();
                if (selected) {
                    drawCardFace(g, r, fullName(c.firstName(), c.lastName()),
                            c.birthDate() == null ? "" : c.birthDate().format(DATE_FORMAT),
                            c.documentNumber(), c.jobLabel(), roster.activeId().isPresent()
                                    && roster.activeId().get().equals(c.id()), true, faceScale, mouseX, mouseY);
                } else {
                    String name = fit(fullName(c.firstName(), c.lastName()), Math.max(1, r.w() - pad()));
                    g.centeredText(this.font, name, r.x() + r.w() / 2, r.y() + (r.h() - lineH()) / 2,
                            ThemeRenderer.color("text"));
                }
            }
            case NEW -> drawSpecialCard(g, r, "character.card.new", "icon.plus", "qrm.character.new", "text");
            case LOCKED -> drawSpecialCard(g, r, "character.card.locked", "icon.lock", "qrm.character.locked", "muted");
        }
    }

    private void drawSpecialCard(GuiGraphicsExtractor g, Rect r, String cardKey, String iconKey, String labelKey,
            String colorKey) {
        ThemeRenderer.draw(g, cardKey, r.x(), r.y(), r.w(), r.h());
        ThemeManifest.Texture icon = theme().texture(iconKey);
        int iw = Math.min(icon.width(), r.w());
        int ih = Math.min(icon.height(), r.h());
        int blockH = ih + pad() / 2 + lineH();
        int top = r.y() + (r.h() - blockH) / 2;
        ThemeRenderer.draw(g, iconKey, r.x() + (r.w() - iw) / 2, top, iw, ih);
        String label = fit(Component.translatable(labelKey).getString(), Math.max(1, r.w() - pad()));
        g.centeredText(this.font, label, r.x() + r.w() / 2, top + ih + pad() / 2, ThemeRenderer.color(colorKey));
    }

    /** Faccia completa di una carta: avatar, nome, nascita, documento, lavoro, tag attivo. */
    private void drawCardFace(GuiGraphicsExtractor g, Rect r, String name, String birth, String document,
            Optional<String> job, boolean active, boolean showJob, double faceScale, int mouseX, int mouseY) {
        int pad = pad();
        int innerW = Math.max(1, r.w() - 2 * pad);
        // offset verticali e avatar seguono l'accorciamento della carta (faceScale = altezza effettiva / cardHeight)
        int topOff = (int) Math.round(pad * faceScale);
        int gapOff = (int) Math.round(pad / 2 * faceScale);
        ThemeManifest.Texture avatar = theme().texture("character.avatar");
        int aw = Math.min((int) Math.round(avatar.width() * faceScale), innerW);
        int ah = Math.min((int) Math.round(avatar.height() * faceScale), Math.max(0, r.h() - 2 * topOff));
        drawPhotoFrame(g, r.x() + (r.w() - aw) / 2, r.y() + topOff, aw, ah, mouseX, mouseY);

        int cx = r.x() + r.w() / 2;
        int step = lineH() + (int) Math.round(pad / 4 * faceScale);
        int limit = r.y() + r.h() - gapOff;
        int y = r.y() + topOff + ah + gapOff;
        int text = ThemeRenderer.color("text");
        int muted = ThemeRenderer.color("muted");
        y = line(g, fit(name, innerW), cx, y, limit, step, ThemeRenderer.color("accent"));
        y = line(g, fit(Component.translatable("qrm.character.born", birth).getString(), innerW), cx, y, limit, step, muted);
        if (document != null && !document.isEmpty()) y = line(g, fit(document, innerW), cx, y, limit, step, muted);
        if (showJob) {
            Component jobLine = job != null && job.isPresent()
                    ? Component.translatable("qrm.character.job", job.get())
                    : Component.translatable("qrm.character.unemployed");
            y = line(g, fit(jobLine.getString(), innerW), cx, y, limit, step, text);
        }
        if (active) line(g, fit(Component.translatable("qrm.character.active").getString(), innerW), cx, y, limit, step,
                ThemeRenderer.color("accent"));
    }

    /** Disegna una riga centrata se c'è spazio nella carta; restituisce il y della riga successiva. */
    private int line(GuiGraphicsExtractor g, String s, int cx, int y, int limit, int step, int color) {
        if (y + lineH() <= limit) g.centeredText(this.font, s, cx, y, color);
        return y + step;
    }

    private void drawCreate(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Rect card = idCard();
        int pad = pad();
        int cx = this.width / 2;
        int accent = ThemeRenderer.color("accent");
        g.centeredText(this.font, upper("qrm.character.create.title"), cx, createBlockTop(), ThemeRenderer.color("text"));
        ThemeRenderer.draw(g, "character.idcard", card.x(), card.y(), card.w(), card.h());
        g.centeredText(this.font, Component.literal(upper("qrm.character.id.title")).withStyle(ChatFormatting.BOLD), cx,
                card.y() + pad, accent);
        int ruleY = card.y() + titleBlockH();
        g.fill(card.x() + pad, ruleY, card.x() + card.w() - pad, ruleY + 1, (accent & 0x00FFFFFF) | 0x66000000);

        drawPhoto(g, card, mouseX, mouseY);

        int label = ThemeRenderer.color("accent2");
        int rowsTop = rowsTop(card);
        int labelX = card.x() + pad + PHOTO_W + pad;
        for (int i = 0; i < FORM_ROWS; i++) {
            g.text(this.font, upper(FORM_LABEL_KEYS[i]), labelX, rowsTop + i * rowH() + (FIELD_H - lineH()) / 2, label);
        }
        for (int i = 0; i < boxes.length; i++) {
            drawField(g, fieldRect(card, i), boxes[i] != null && boxes[i].isFocused());
        }
        Rect sig = fieldRect(card, 4);
        drawField(g, sig, false);
        String signature = signature(firstText, lastText);
        if (!signature.isEmpty()) {
            g.text(this.font, Component.literal(fit(signature, Math.max(1, sig.w() - 2 * FIELD_TEXT_INSET)))
                            .withStyle(ChatFormatting.ITALIC),
                    sig.x() + FIELD_TEXT_INSET, sig.y() + (sig.h() - lineH()) / 2, accent);
        }
    }

    private void drawField(GuiGraphicsExtractor g, Rect r, boolean focused) {
        ThemeRenderer.draw(g, focused ? "character.field.focus" : "character.field", r.x(), r.y(), r.w(), r.h());
    }

    /** Colonna sinistra della carta: etichetta, cornice con la testa 3D del giocatore, impronta e numero documento. */
    private void drawPhoto(GuiGraphicsExtractor g, Rect card, int mouseX, int mouseY) {
        int pad = pad();
        int x = card.x() + pad;
        int top = rowsTop(card);
        g.text(this.font, upper("qrm.character.id.photo"), x, top, ThemeRenderer.color("accent"));
        ThemeManifest.Texture icon = theme().texture("icon.fingerprint");
        int frameY = top + lineH() + 2;
        int frameH = Math.max(1, rowsH() - (frameY - top) - pad / 2 - icon.height());
        drawPhotoFrame(g, x, frameY, PHOTO_W, frameH, mouseX, mouseY);
        int iconY = frameY + frameH + pad / 2;
        ThemeRenderer.draw(g, "icon.fingerprint", x, iconY, icon.width(), icon.height());
        int textX = x + icon.width() + pad / 2;
        g.text(this.font, fit(Component.translatable("qrm.character.id.number", "—").getString(),
                Math.max(1, x + PHOTO_W - textX)), textX, iconY + (icon.height() - lineH()) / 2, ThemeRenderer.color("muted"));
    }

    /** Cornice foto con la testa 3D del giocatore (o il segnaposto se manca), centrata e ritagliata dalla cornice. */
    private void drawPhotoFrame(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        ThemeRenderer.draw(g, "character.photo", x, y, w, h);
        int inset = 3;
        int iw = Math.max(1, w - 2 * inset);
        int ih = Math.max(1, h - 2 * inset);
        if (this.minecraft != null && this.minecraft.player != null) {
            // scala proporzionale alla cornice: HEAD_SCALE è tarato su una cornice interna larga HEAD_REF_W
            int scale = Math.max(1, Math.round(HEAD_SCALE * Math.min(iw / (float) HEAD_REF_W, ih / (float) HEAD_REF_H)));
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, x + inset, y + inset, x + inset + iw, y + inset + ih,
                    scale, HEAD_OFFSET_Y, mouseX, mouseY, this.minecraft.player);
        } else {
            ThemeRenderer.draw(g, "character.avatar", x + inset, y + inset, iw, ih);
        }
    }

    /** Firma di anteprima: iniziale del nome e cognome ("M. Rossi"). */
    private static String signature(String first, String last) {
        String f = first == null ? "" : first.strip();
        String l = last == null ? "" : last.strip();
        String initial = f.isEmpty() ? "" : f.substring(0, f.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT) + ".";
        return (initial + " " + l).strip();
    }

    private static String upper(String translationKey) {
        return Component.translatable(translationKey).getString().toUpperCase(Locale.ROOT);
    }

    private void drawConfirm(GuiGraphicsExtractor g) {
        Rect panel = confirmPanel();
        int pad = pad();
        int cx = panel.x() + panel.w() / 2;
        int innerW = Math.max(1, panel.w() - 2 * pad);
        ThemeRenderer.draw(g, "character.idcard", panel.x(), panel.y(), panel.w(), panel.h());
        int y = panel.y() + pad;
        g.centeredText(this.font, fit(upper("qrm.character.confirm.title"), innerW), cx, y, ThemeRenderer.color("accent"));
        y += lineH() + pad / 2;
        g.centeredText(this.font, fit(archiveName, innerW), cx, y, ThemeRenderer.color("accent"));
        y += lineH() + pad / 2;
        for (FormattedCharSequence seq : confirmLines()) {
            g.centeredText(this.font, seq, cx, y, ThemeRenderer.color("muted"));
            y += lineH();
        }
    }

    // ---- utilità -------------------------------------------------------------------------------------------

    private static String fullName(String first, String last) {
        String f = first == null ? "" : first.strip();
        String l = last == null ? "" : last.strip();
        return (f + " " + l).strip();
    }

    /** Tronca una stringa alla larghezza in pixel, con puntini di sospensione. */
    private String fit(String s, int maxW) {
        if (s == null) return "";
        if (this.font.width(s) <= maxW) return s;
        int room = maxW - this.font.width(ELLIPSIS);
        if (room <= 0) return "";
        return this.font.plainSubstrByWidth(s, room) + ELLIPSIS;
    }
}
