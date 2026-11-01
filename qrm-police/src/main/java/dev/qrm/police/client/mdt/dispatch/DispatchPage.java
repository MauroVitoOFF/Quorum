package dev.qrm.police.client.mdt.dispatch;

import static dev.qrm.police.client.mdt.dispatch.CallFormat.stateOf;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.BLUE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.GREEN;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.LINE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.PANEL;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.PANEL_HI;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.RED;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.MdtPage;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Btn;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Il Dispatch: la centrale operativa. È un instradatore: tiene la testata (schede e contatori) e decide quale vista
 * mostrare: elenco delle chiamate, unità, storico, dettaglio di una chiamata o creazione di una nuova.
 */
public final class DispatchPage extends MdtPage {
    private static final String[] VIEW_NAMES = {"Chiamate", "Unità", "Storico"};
    private final CallListView calls;
    private final UnitsView units;
    private final HistoryView history;
    private final CallDetailView detail;
    private final NewCallView creator;
    private final DispatchView[] tabs;
    private int view;
    private String openCallId;
    private boolean creating;

    public DispatchPage(MdtHost host) {
        super(host);
        calls = new CallListView(host, this);
        units = new UnitsView(host, this);
        history = new HistoryView(host, this);
        detail = new CallDetailView(host, this);
        creator = new NewCallView(host, this);
        tabs = new DispatchView[] {calls, units, history};
    }

    @Override
    public void sync() {
        super.sync();
        for (DispatchView v : tabs) v.sync();
        detail.sync();
        creator.sync();
    }

    // ---- navigazione ----

    /** Apre la pagina di una chiamata; se la pagina non è quella visibile, ci passa. */
    public void showCall(String id) {
        openCallId = id;
        creating = false;
        detail.onEnter();
        host.setForm(null);
        host.open(DispatchPage.class);
    }

    /** Dal dettaglio di una chiamata all'elenco in cui era. */
    void backToList() {
        openCallId = null;
        active().resetScroll();
        rebuildWidgets();
    }

    void startCreating() {
        creator.start();
        openCallId = null;
        creating = true;
        rebuildWidgets();
    }

    void finishCreating() {
        creating = false;
        rebuildWidgets();
    }

    /** La chiamata aperta, cercata tra le attive e tra quelle dello storico; null se non c'è più. */
    JsonObject openCall() {
        if (openCallId == null) return null;
        for (JsonObject c : list("calls")) if (str(c, "id").equals(openCallId)) return c;
        for (JsonObject c : history.rows()) if (str(c, "id").equals(openCallId)) return c;
        openCallId = null;
        detail.onEnter();
        return null;
    }

    private void setView(int v) {
        view = v;
        tabs[v].onEnter();
        rebuildWidgets();
    }

    /** La vista che occupa il corpo della pagina in questo momento. */
    private MdtPage active() {
        if (creating) return creator;
        if (openCallId != null && openCall() != null) return detail;
        return tabs[view];
    }

    @Override
    public void onEnter() {
        active().resetScroll();
    }

    /** Un secondo clic su "Dispatch" riporta all'elenco delle chiamate. */
    @Override
    public void onReselect() {
        if (openCallId == null && !creating) view = 0;
        openCallId = null;
        creating = false;
        detail.onEnter();
        creator.onEnter();
        active().resetScroll();
    }

    @Override
    public boolean onScroll(double mx, double my, int step) {
        return active().onScroll(mx, my, step);
    }

    @Override
    public void tick() {
        if (creating || openCall() != null) return;
        tabs[view].tick();
    }

    @Override
    public boolean onReply(String kind, List<JsonObject> rows) {
        boolean changed = false;
        for (DispatchView v : tabs) changed |= v.onReply(kind, rows);
        return changed && !creating;
    }

    // ---- costruzione e disegno ----

    @Override
    public void build() {
        MdtPage body = active();
        if (body != tabs[view]) {
            body.build();
            return;
        }
        int tw = 44;
        for (int i = 0; i < VIEW_NAMES.length; i++) {
            final int v = i;
            addRenderableWidget(new Btn(cx + i * (tw + 2), cy, tw, 13, (g, bx, by, bw, bh, hot, active) -> {
                boolean sel = v == view;
                MdtWidgets.box(g, bx, by, bw, bh, sel ? BLUE : hot ? MdtWidgets.alpha(BLUE, 0x90) : LINE,
                        sel ? MdtWidgets.alpha(BLUE, 0x30) : hot ? PANEL_HI : PANEL);
                small(g, VIEW_NAMES[v], bx + 5, by + 4, bw - 8, sel ? TEXT : DIM);
            }, () -> setView(v)));
        }
        body.build();
    }

    @Override
    public void render(GuiGraphicsExtractor g) {
        MdtPage body = active();
        if (body != tabs[view]) {
            body.render(g);
            return;
        }
        renderHeader(g);
        body.render(g);
    }

    /** Stato della centrale e tre contatori, sotto le schede. */
    private void renderHeader(GuiGraphicsExtractor g) {
        List<JsonObject> open = list("calls");
        int inProgress = 0;
        for (JsonObject c : open) if (stateOf(c).inProgress()) inProgress++;
        int free = 0;
        int onDuty = 0;
        for (JsonElement e : arr(data, "units")) {
            String st = str(e.getAsJsonObject(), "status");
            if (st.equals("PATROL")) free++;
            if (!st.equals("OFF")) onDuty++;
        }
        boolean live = onDuty > 0;
        String chip = live ? "ATTIVO" : "INATTIVO";
        int chipW = (int) (font.width(chip) * 0.8f) + 12;
        int chipX = cx + cw - chipW;
        MdtWidgets.box(g, chipX, cy + 17, chipW, 12, MdtWidgets.alpha(live ? GREEN : DIM, 0xA0),
                MdtWidgets.alpha(live ? GREEN : DIM, 0x20));
        g.fill(chipX + 3, cy + 21, chipX + 6, cy + 24, live ? GREEN : DIM);
        small(g, chip, chipX + 9, cy + 20, chipW - 10, live ? GREEN : DIM);

        int third = (cw - chipW - 4 - 8) / 3;
        String[] labels = {"Chiamate", "Disponibili", "In corso"};
        String[] values = {String.valueOf(open.size()), String.valueOf(free), String.valueOf(inProgress)};
        int[] colors = {open.isEmpty() ? DIM : RED, GREEN, BLUE};
        for (int i = 0; i < 3; i++) {
            int bx = cx + i * (third + 4);
            MdtWidgets.box(g, bx, cy + 17, third, 12, LINE, PANEL);
            g.fill(bx, cy + 17, bx + 3, cy + 29, colors[i]);
            int vw = font.width(values[i]);
            small(g, labels[i], bx + 6, cy + 20, third - vw - 14, DIM);
            g.text(font, values[i], bx + third - 4 - vw, cy + 20, colors[i], false);
        }
    }
}
