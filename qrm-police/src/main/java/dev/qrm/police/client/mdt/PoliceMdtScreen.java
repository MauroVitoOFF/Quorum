package dev.qrm.police.client.mdt;

import static dev.qrm.police.client.mdt.MdtLayout.H;
import static dev.qrm.police.client.mdt.MdtLayout.HEADER;
import static dev.qrm.police.client.mdt.MdtLayout.PAD;
import static dev.qrm.police.client.mdt.MdtLayout.SIDE;
import static dev.qrm.police.client.mdt.MdtLayout.STATUS;
import static dev.qrm.police.client.mdt.MdtLayout.W;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.BLUE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.GREEN;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.LINE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.PANEL;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.PANEL_HI;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.RED;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.qrm.police.client.mdt.citizens.CitizensPage;
import dev.qrm.police.client.mdt.dashboard.DashboardPage;
import dev.qrm.police.client.mdt.dispatch.DispatchPage;
import dev.qrm.police.client.mdt.glossary.GlossaryPage;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Btn;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Field;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import dev.qrm.police.net.PolicePayloads;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Il terminale operativo: un tablet disegnato in codice che ospita le pagine (Dashboard, Cittadini, Dispatch,
 * Glossario). Qui ci sono solo la cornice, il menu laterale e l'instradamento di eventi, dati e moduli; il contenuto
 * sta nelle pagine. Il server decide tutto: la schermata mostra lo stato ricevuto e manda azioni.
 */
public final class PoliceMdtScreen extends Screen implements MdtHost {
    private final List<MdtPage> pages = new ArrayList<>();
    private final List<String> titles = new ArrayList<>();
    private String officer;
    private JsonObject data;
    private int tab;
    private MdtForm form;
    private int x0, y0, cx, cy, cw, ch;
    private MdtChrome chrome;

    public PoliceMdtScreen(String officer, JsonObject data) {
        super(Component.literal("MDT"));
        this.officer = officer;
        this.data = data;
        addPage("Dashboard", new DashboardPage(this));
        addPage("Cittadini", new CitizensPage(this));
        addPage("Dispatch", new DispatchPage(this));
        addPage("Glossario", new GlossaryPage(this));
    }

    private void addPage(String title, MdtPage page) {
        titles.add(title);
        pages.add(page);
    }

    private MdtPage page() {
        return pages.get(tab);
    }

    private void syncPages() {
        for (MdtPage p : pages) p.sync();
    }

    // ---- dati dal server ----

    public void update(String officer, JsonObject data) {
        this.officer = officer;
        this.data = data;
        rebuildWidgets();
    }

    /** Una risposta del server a una richiesta di una pagina (posizioni delle unità, storico). */
    public void onReply(String kind, JsonArray rows) {
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : rows) if (e.isJsonObject()) out.add(e.getAsJsonObject());
        syncPages();
        if (page().onReply(kind, out) && form == null) rebuildWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        if (form != null) return;
        syncPages();
        page().tick();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        ClientPacketDistributor.sendToServer(new PolicePayloads.Action("close", List.of()));
        super.removed();
    }

    // ---- MdtHost ----

    @Override
    public Font font() {
        return this.font;
    }

    @Override
    public String officer() {
        return officer;
    }

    @Override
    public JsonObject data() {
        return data;
    }

    @Override
    public int cx() {
        return cx;
    }

    @Override
    public int cy() {
        return cy;
    }

    @Override
    public int cw() {
        return cw;
    }

    @Override
    public int ch() {
        return ch;
    }

    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T add(T widget) {
        return addRenderableWidget(widget);
    }

    @Override
    public void rebuild() {
        rebuildWidgets();
    }

    @Override
    public void focus(GuiEventListener widget) {
        setFocused(widget);
    }

    @Override
    public void send(String kind, String... args) {
        ClientPacketDistributor.sendToServer(new PolicePayloads.Action(kind, List.of(args)));
    }

    @Override
    public void setForm(MdtForm form) {
        this.form = form;
    }

    @Override
    public <P extends MdtPage> P open(Class<P> type) {
        for (int i = 0; i < pages.size(); i++) {
            if (type.isInstance(pages.get(i))) {
                if (i != tab) {
                    pages.get(i).onEnter();
                    tab = i;
                    form = null;
                }
                rebuildWidgets();
                return type.cast(pages.get(i));
            }
        }
        throw new IllegalArgumentException("Nessuna pagina " + type.getSimpleName());
    }

    // ---- costruzione ----

    @Override
    protected void init() {
        x0 = (this.width - W) / 2;
        y0 = (this.height - H) / 2;
        cx = x0 + SIDE + PAD;
        cy = y0 + STATUS + HEADER + PAD;
        cw = W - SIDE - 2 * PAD;
        ch = H - STATUS - HEADER - 2 * PAD;
        chrome = new MdtChrome(this.font, x0, y0);

        for (int i = 0; i < titles.size(); i++) {
            final int t = i;
            addRenderableWidget(new Btn(x0 + PAD, y0 + STATUS + HEADER + PAD + i * 22, SIDE - 2 * PAD, 18,
                    (g, bx, by, bw, bh, hot, active) -> {
                        boolean sel = t == tab;
                        MdtWidgets.box(g, bx, by, bw, bh, sel ? BLUE : hot ? MdtWidgets.alpha(BLUE, 0x90) : LINE,
                                sel ? MdtWidgets.alpha(BLUE, 0x30) : hot ? PANEL_HI : PANEL);
                        if (sel) {
                            g.fill(bx, by, bx + 3, by + bh, BLUE);
                            g.fill(bx + bw - 3, by, bx + bw, by + bh, MdtWidgets.alpha(BLUE, 0x80));
                        } else if (hot) {
                            g.fill(bx, by, bx + 2, by + bh, MdtWidgets.alpha(BLUE, 0x90));
                        }
                        g.text(this.font, MdtWidgets.fit(this.font, titles.get(t), bw - 14), bx + 8, by + 5,
                                sel ? TEXT : DIM, false);
                    }, () -> select(t)));
        }
        addRenderableWidget(new Btn(x0 + W - 22, y0 + STATUS + 4, 16, 14, Component.literal("x"), RED, this::onClose));

        syncPages();
        if (form != null) {
            buildForm();
        } else {
            page().build();
        }
    }

    /** Un secondo clic sulla scheda già aperta riporta la pagina alla vista iniziale. */
    private void select(int t) {
        syncPages();
        if (t == tab) page().onReselect();
        else pages.get(t).onEnter();
        tab = t;
        form = null;
        rebuildWidgets();
    }

    // ---- modulo ----

    private void buildForm() {
        int y = cy + 22;
        for (int i = 0; i < form.labels().length; i++) {
            final int idx = i;
            Field f = new Field(this.font, cx, y + 10, cw, 16, Component.empty());
            f.setMaxLength(i == 0 ? 120 : 200);
            f.setValue(form.values()[i]);
            f.setResponder(v -> form.values()[idx] = v);
            addRenderableWidget(f);
            if (i == 0) setFocused(f);
            y += 32;
        }
        addRenderableWidget(new Btn(cx + cw - 160, cy + ch - 18, 76, 16, Component.literal("Salva"), GREEN, () -> {
            MdtForm done = form;
            form = null;
            done.submit().accept(done.values().clone());
            rebuildWidgets();
        }));
        addRenderableWidget(new Btn(cx + cw - 80, cy + ch - 18, 76, 16, Component.literal("Annulla"), DIM, () -> {
            form = null;
            rebuildWidgets();
        }));
    }

    private void renderForm(GuiGraphicsExtractor g) {
        g.text(this.font, form.title().toUpperCase(Locale.ROOT), cx, cy + 2, BLUE, false);
        int y = cy + 22;
        for (String label : form.labels()) {
            g.text(this.font, MdtWidgets.fit(this.font, label, cw), cx, y, DIM, false);
            y += 32;
        }
    }

    // ---- disegno e input ----

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        syncPages();
        boolean activeCalls = data.has("calls") && !data.getAsJsonArray("calls").isEmpty();
        chrome.renderBack(g, mouseX, mouseY, activeCalls, officer);
        if (form != null) renderForm(g);
        else page().render(g);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        chrome.renderGlare(g);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && chrome.overHome(event.x(), event.y())) {
            MdtWidgets.click();
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (scrollY != 0 && form == null) {
            syncPages();
            if (page().onScroll(mx, my, -(int) Math.signum(scrollY))) {
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }
}
