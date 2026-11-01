package dev.qrm.police.client.mdt;

import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Btn;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Field;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

/**
 * Una pagina dell'MDT, o una vista dentro una pagina. Si costruisce con {@link #build()} (widget) e si disegna con
 * {@link #render} (testo e sfondi); lo stato ricevuto dal server e l'area utile le arrivano dalla schermata.
 */
public abstract class MdtPage {
    protected final MdtHost host;
    protected Font font;
    protected JsonObject data;
    protected int cx, cy, cw, ch;
    protected int scroll;
    /** Il testo cercato, per le pagine che hanno una casella di ricerca. */
    protected String query = "";
    private Field refocus;

    protected MdtPage(MdtHost host) {
        this.host = host;
    }

    /** Aggiorna font, dati e misure; la schermata lo chiama prima di costruire, disegnare e far passare il tempo. */
    public void sync() {
        font = host.font();
        data = host.data();
        cx = host.cx();
        cy = host.cy();
        cw = host.cw();
        ch = host.ch();
    }

    public abstract void build();

    public abstract void render(GuiGraphicsExtractor g);

    /** Ogni tick, solo per la pagina visibile e senza moduli aperti. */
    public void tick() {}

    /** Una risposta del server a una richiesta di questa pagina; true se la pagina va ricostruita. */
    public boolean onReply(String kind, List<JsonObject> rows) {
        return false;
    }

    /** Si entra nella pagina da un'altra scheda. */
    public void onEnter() {
        scroll = 0;
    }

    /** Si preme di nuovo la scheda della pagina già aperta: tornare alla vista iniziale. */
    public void onReselect() {}

    /** La rotella; false se la pagina vuole lasciarla a un suo widget. */
    public boolean onScroll(double mx, double my, int step) {
        scroll = Math.max(0, scroll + step);
        return true;
    }

    public final Font font() {
        return font;
    }

    public final MdtHost host() {
        return host;
    }

    // ---- ponte verso la schermata ----

    public final <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget) {
        return host.add(widget);
    }

    protected final void rebuildWidgets() {
        host.rebuild();
    }

    protected final void setFocused(GuiEventListener widget) {
        host.focus(widget);
    }

    public final void send(String kind, String... args) {
        host.send(kind, args);
    }

    // ---- lettura dello stato ----

    protected static JsonArray arr(JsonObject o, String key) {
        return o.getAsJsonArray(key);
    }

    protected static String str(JsonObject o, String key) {
        return o.get(key).getAsString();
    }

    protected final List<JsonObject> list(String key) {
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : arr(data, key)) out.add(e.getAsJsonObject());
        return out;
    }

    protected final JsonObject citizenById(String id) {
        if (id == null) return null;
        for (JsonObject c : list("citizens")) if (str(c, "id").equals(id)) return c;
        return null;
    }

    /** Lo stato scelto dall'agente: PATROL, BUSY o OFF. */
    public final String myStatus() {
        return data.has("me") ? str(data, "me") : "OFF";
    }

    protected final int wantedCount() {
        int n = 0;
        for (JsonObject c : list("citizens")) if (c.get("wanted").getAsBoolean()) n++;
        return n;
    }

    public final void resetScroll() {
        scroll = 0;
    }

    protected final boolean canOperate() {
        return data.has("canOperate") && data.get("canOperate").getAsBoolean();
    }

    protected static String myId() {
        var p = net.minecraft.client.Minecraft.getInstance().player;
        return p == null ? "" : p.getUUID().toString();
    }

    // ---- elementi ricorrenti ----

    /** Testo al 80%, tagliato perché non esca mai dal riquadro. */
    public final void small(GuiGraphicsExtractor g, String s, int x, int y, int width, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.8f, 0.8f);
        g.text(font, MdtWidgets.fit(font, s, (int) (width / 0.8f)), 0, 0, color, false);
        g.pose().popMatrix();
    }

    /** Pulsante con testo al 80%, per le celle strette. */
    public final Btn smallButton(int x, int y, int w, int h, String label, int color, Runnable run) {
        return new Btn(x, y, w, h, (g, bx, by, bw, bh, hot, active) -> {
            MdtWidgets.box(g, bx, by, bw, bh, hot ? color : MdtWidgets.alpha(color, 0xA0),
                    MdtWidgets.alpha(color, hot ? 0x40 : 0x1C));
            small(g, label, bx + 4, by + (bh - 7) / 2 + 1, bw - 8, active ? TEXT : DIM);
        }, run);
    }

    /** Come {@link #smallButton}, con una piccola freccia in giù disegnata a pixel: apre un elenco. */
    public final Btn dropdownButton(int x, int y, int w, int h, String label, int color, Runnable run) {
        return new Btn(x, y, w, h, (g, bx, by, bw, bh, hot, active) -> {
            MdtWidgets.box(g, bx, by, bw, bh, hot ? color : MdtWidgets.alpha(color, 0xA0),
                    MdtWidgets.alpha(color, hot ? 0x40 : 0x1C));
            small(g, label, bx + 4, by + (bh - 7) / 2 + 1, bw - 18, active ? TEXT : DIM);
            int ax = bx + bw - 12;
            int ay = by + bh / 2 - 2;
            int arrow = active ? TEXT : DIM;
            g.fill(ax, ay, ax + 7, ay + 1, arrow);
            g.fill(ax + 1, ay + 1, ax + 6, ay + 2, arrow);
            g.fill(ax + 2, ay + 2, ax + 5, ay + 3, arrow);
            g.fill(ax + 3, ay + 3, ax + 4, ay + 4, arrow);
        }, run);
    }

    protected final void backButton(String label, Runnable run) {
        addRenderableWidget(new Btn(cx, cy, 78, 16, Component.literal(label), DIM, run));
    }

    /** Casella di ricerca che ricostruisce la pagina a ogni modifica e ridà il focus a se stessa. */
    protected final Field searchField(int x, int y, int w, String hint) {
        Field f = new Field(font, x, y, w, 16, Component.literal(hint));
        f.setMaxLength(40);
        f.setValue(query);
        f.setResponder(v -> {
            if (v.equals(query)) return;
            query = v;
            scroll = 0;
            refocus = f;
            rebuildWidgets();
        });
        addRenderableWidget(f);
        if (refocus != null && refocus.getValue().equals(f.getValue())) {
            setFocused(f);
            f.setFocused(true);
            refocus = null;
        }
        return f;
    }

    /** Casella di testo il cui valore sta in una bozza della pagina, senza ricostruire a ogni tasto. */
    protected final Field draftField(int x, int y, int w, String hint, int max, String value,
            java.util.function.Consumer<String> setter) {
        Field f = new Field(font, x, y, w, 16, Component.literal(hint));
        f.setMaxLength(max);
        f.setValue(value);
        f.setResponder(setter);
        addRenderableWidget(f);
        return f;
    }

    /** Va a capo a {@code width} pixel di testo. */
    protected final List<String> wrap(String s, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String t = line.length() == 0 ? word : line + " " + word;
            if (font.width(t) <= width || line.length() == 0) {
                line = new StringBuilder(t);
            } else {
                out.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }
}
