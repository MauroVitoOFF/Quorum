package dev.qrm.police.client;

import static dev.qrm.police.client.MdtWidgets.AMBER;
import static dev.qrm.police.client.MdtWidgets.BG;
import static dev.qrm.police.client.MdtWidgets.BLUE;
import static dev.qrm.police.client.MdtWidgets.DIM;
import static dev.qrm.police.client.MdtWidgets.GREEN;
import static dev.qrm.police.client.MdtWidgets.LINE;
import static dev.qrm.police.client.MdtWidgets.PANEL;
import static dev.qrm.police.client.MdtWidgets.PANEL_HI;
import static dev.qrm.police.client.MdtWidgets.RED;
import static dev.qrm.police.client.MdtWidgets.TEXT;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.qrm.police.client.MdtWidgets.Btn;
import dev.qrm.police.client.MdtWidgets.Field;
import dev.qrm.police.logic.PenalCode;
import dev.qrm.police.net.PolicePayloads;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Terminale operativo: quattro schede su un tablet disegnato in codice. Il server decide tutto; qui si mostra lo stato
 * ricevuto e si mandano azioni.
 */
final class PoliceMdtScreen extends Screen {
    private static final int W = 400;
    private static final int H = 256;
    private static final int STATUS = 12;
    private static final int BEZEL = 12;
    private static final int HEADER = 22;
    private static final int SIDE = 100;
    private static final int PAD = 6;
    private static final String[] TABS = {"Dashboard", "Cittadini", "Dispatch", "Codice penale"};
    private static final int T_DASH = 0, T_CITIZENS = 1, T_DISPATCH = 2, T_PENAL = 3;
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm").withZone(ZoneId.systemDefault());
    private static final String[] DANGER_LABEL = {"Basso", "Medio", "Alto", "Armato e pericoloso"};
    private static final int[] DANGER_COLOR = {GREEN, AMBER, RED, RED};
    private static final int FACE_BORDER = 0xFF3A5A80;

    /** Un modulo testuale sopra la scheda: titolo, campi e cosa fare al salvataggio. */
    private record Form(String title, String[] labels, String[] values, Consumer<String[]> submit) {}

    private String officer;
    private JsonObject data;

    private int tab;
    private int scroll;
    private int recordScroll;
    private final String[] queries = new String[TABS.length];
    private String selectedCitizen;
    private boolean onlyWanted;
    private final Set<String> checkedOffenses = new HashSet<>();
    private Form form;
    private Field refocus;

    private int x0, y0, cx, cy, cw, ch;

    PoliceMdtScreen(String officer, JsonObject data) {
        super(Component.literal("MDT"));
        this.officer = officer;
        this.data = data;
        java.util.Arrays.fill(queries, "");
    }

    void update(String officer, JsonObject data) {
        this.officer = officer;
        this.data = data;
        rebuildWidgets();
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

    // ---- dati ----

    private static JsonArray arr(JsonObject o, String key) {
        return o.getAsJsonArray(key);
    }

    private static String str(JsonObject o, String key) {
        return o.get(key).getAsString();
    }

    private List<JsonObject> list(String key) {
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : arr(data, key)) out.add(e.getAsJsonObject());
        return out;
    }

    private JsonObject citizenById(String id) {
        if (id == null) return null;
        for (JsonObject c : list("citizens")) if (str(c, "id").equals(id)) return c;
        return null;
    }

    private int wantedCount() {
        int n = 0;
        for (JsonObject c : list("citizens")) if (c.get("wanted").getAsBoolean()) n++;
        return n;
    }

    private void send(String kind, String... args) {
        ClientPacketDistributor.sendToServer(new PolicePayloads.Action(kind, List.of(args)));
    }

    private static String money(long v) {
        return "$" + String.format(Locale.ROOT, "%,d", v);
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

        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            addRenderableWidget(new Btn(x0 + PAD, y0 + STATUS + HEADER + PAD + i * 22, SIDE - 2 * PAD, 18,
                    (g, bx, by, bw, bh, hot, active) -> {
                        boolean sel = t == tab;
                        MdtWidgets.box(g, bx, by, bw, bh, sel ? BLUE : hot ? MdtWidgets.alpha(BLUE, 0x90) : LINE,
                                sel ? MdtWidgets.alpha(BLUE, 0x30) : hot ? PANEL_HI : PANEL);
                        if (sel) g.fill(bx, by, bx + 3, by + bh, BLUE);
                        g.text(this.font, MdtWidgets.fit(this.font, TABS[t], bw - 14), bx + 8, by + 5,
                                sel ? TEXT : DIM, false);
                    }, () -> switchTab(t)));
        }
        addRenderableWidget(new Btn(x0 + W - 22, y0 + STATUS + 4, 16, 14, Component.literal("x"), RED, this::onClose));

        if (form != null) {
            buildForm();
            return;
        }
        switch (tab) {
            case T_DASH -> buildDashboard();
            case T_CITIZENS -> buildCitizens();
            case T_DISPATCH -> buildDispatch();
            default -> buildPenal();
        }
    }

    private void switchTab(int t) {
        tab = t;
        scroll = 0;
        recordScroll = 0;
        form = null;
        rebuildWidgets();
    }

    private Field searchField(int x, int y, int w, String hint) {
        Field f = new Field(this.font, x, y, w, 16, Component.literal(hint));
        f.setMaxLength(40);
        f.setValue(queries[tab]);
        f.setResponder(v -> {
            if (v.equals(queries[tab])) return;
            queries[tab] = v;
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

    /** Testo al 80%, tagliato perché non esca mai dal riquadro. */
    private void small(GuiGraphicsExtractor g, String s, int x, int y, int width, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.8f, 0.8f);
        g.text(this.font, MdtWidgets.fit(this.font, s, (int) (width / 0.8f)), 0, 0, color, false);
        g.pose().popMatrix();
    }

    /** Pulsante con testo al 80%, per le celle strette. */
    private Btn smallButton(int x, int y, int w, int h, String label, int color, Runnable run) {
        return new Btn(x, y, w, h, (g, bx, by, bw, bh, hot, active) -> {
            MdtWidgets.box(g, bx, by, bw, bh, hot ? color : MdtWidgets.alpha(color, 0xA0),
                    MdtWidgets.alpha(color, hot ? 0x40 : 0x1C));
            small(g, label, bx + 4, by + (bh - 7) / 2 + 1, bw - 8, active ? TEXT : DIM);
        }, run);
    }

    // ---- scheda 1: dashboard, BOLO e note ----

    private void buildDashboard() {
        int leftW = 180;
        int rightX = cx + leftW + 6;
        int rightW = cw - leftW - 6;
        int top = cy + 42;
        addRenderableWidget(new Btn(cx + leftW - 76, top - 2, 76, 14, Component.literal("+ Nuovo BOLO"), BLUE, () -> {
            form = new Form("Nuovo BOLO", new String[] {"Titolo", "Descrizione", "Priorità (1-3)"},
                    new String[] {"", "", "2"}, v -> send("bolo_add", v[0], v[1], v[2]));
            rebuildWidgets();
        }));
        List<JsonObject> bolos = list("bolos");
        int rowH = 30;
        int listTop = top + 16;
        int rows = Math.max(1, (cy + ch - 3 - listTop) / rowH);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, bolos.size() - rows)));
        for (int i = 0; i < rows && scroll + i < bolos.size(); i++) {
            JsonObject b = bolos.get(scroll + i);
            int y = listTop + i * rowH;
            int prio = b.get("prio").getAsInt();
            int color = prio >= 3 ? RED : prio == 2 ? AMBER : BLUE;
            addRenderableWidget(new Btn(cx + 3, y, leftW - 6 - 46, rowH - 2, (g, x, yy, w, h, hot, active) -> {
                MdtWidgets.box(g, x, yy, w, h, LINE, hot ? PANEL_HI : PANEL);
                g.fill(x, yy, x + 3, yy + h, color);
                g.text(this.font, MdtWidgets.fit(this.font, str(b, "title"), w - 14), x + 8, yy + 4, TEXT, false);
                g.text(this.font, MdtWidgets.fit(this.font, str(b, "desc"), w - 14), x + 8, yy + 14, DIM, false);
            }, () -> {}));
            addRenderableWidget(new Btn(cx + leftW - 3 - 44, y, 44, rowH - 2, Component.literal("Risolvi"), GREEN,
                    () -> send("bolo_remove", str(b, "id"))));
        }

        String[] notes = ClientPolice.notes();
        for (int i = 0; i < ClientPolice.NOTE_LINES; i++) {
            final int line = i;
            Field f = new Field(this.font, rightX + 4, cy + 60 + i * 20, rightW - 8, 16,
                    Component.literal("Nota " + (i + 1)));
            f.setMaxLength(120);
            f.setValue(notes[i]);
            f.setResponder(v -> ClientPolice.saveNote(line, v));
            addRenderableWidget(f);
        }
    }

    private void renderDashboard(GuiGraphicsExtractor g) {
        int cardW = (cw - 8) / 3;
        int lastX = cx + 2 * (cardW + 4);
        int calls = arr(data, "calls").size();
        card(g, cx, cardW, "Chiamate Attive", String.valueOf(calls), calls > 0 ? RED : GREEN);
        card(g, cx + cardW + 4, cardW, "Ricercati (BOLO)", String.valueOf(wantedCount()), AMBER);
        card(g, lastX, cx + cw - lastX, "Totale Schedati", String.valueOf(arr(data, "citizens").size()), BLUE);

        int leftW = 180;
        int rightX = cx + leftW + 6;
        int rightW = cw - leftW - 6;
        g.text(this.font, "BOLO ATTIVI (" + arr(data, "bolos").size() + ")", cx, cy + 44, DIM, false);
        MdtWidgets.inset(g, cx, cy + 56, leftW, ch - 56);
        if (arr(data, "bolos").isEmpty()) g.text(this.font, "Nessun avviso attivo.", cx + 8, cy + 64, DIM, false);

        g.text(this.font, "NOTE RAPIDE", rightX, cy + 44, DIM, false);
        MdtWidgets.inset(g, rightX, cy + 56, rightW, ch - 56);
        small(g, "Salvate su questo PC", rightX + 5, cy + 142, rightW - 10, DIM);
    }

    private void card(GuiGraphicsExtractor g, int x, int w, String label, String value, int color) {
        MdtWidgets.box(g, x, cy, w, 34, LINE, PANEL);
        g.fill(x, cy, x + 3, cy + 34, color);
        small(g, label, x + 8, cy + 5, w - 12, DIM);
        g.text(this.font, value, x + 8, cy + 18, color, false);
    }

    // ---- scheda 2: cittadini ----

    private List<JsonObject> filteredCitizens() {
        String q = queries[T_CITIZENS].trim().toLowerCase(Locale.ROOT);
        List<JsonObject> out = new ArrayList<>();
        for (JsonObject c : list("citizens")) {
            if (onlyWanted && !c.get("wanted").getAsBoolean()) continue;
            String name = (str(c, "first") + " " + str(c, "last")).toLowerCase(Locale.ROOT);
            String rev = (str(c, "last") + " " + str(c, "first")).toLowerCase(Locale.ROOT);
            if (q.isEmpty() || name.contains(q) || rev.contains(q)) out.add(c);
        }
        return out;
    }

    private void buildCitizens() {
        int listW = 124;
        searchField(cx, cy, cw - 106, "Nome o cognome...");
        addRenderableWidget(smallButton(cx + cw - 102, cy, 102, 16,
                onlyWanted ? "[x] Solo Ricercati" : "[ ] Solo Ricercati", onlyWanted ? RED : DIM, () -> {
                    onlyWanted = !onlyWanted;
                    scroll = 0;
                    rebuildWidgets();
                }));
        int base = cy + 22;
        List<JsonObject> found = filteredCitizens();
        int rowH = 24;
        int rows = (ch - 22 - 4) / rowH;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, found.size() - rows)));
        for (int i = 0; i < rows && scroll + i < found.size(); i++) {
            JsonObject c = found.get(scroll + i);
            boolean sel = str(c, "id").equals(selectedCitizen);
            addRenderableWidget(new Btn(cx + 2, base + 2 + i * rowH, listW - 4, rowH - 1,
                    (g, x, y, w, h, hot, active) -> {
                        MdtWidgets.box(g, x, y, w, h, sel ? BLUE : LINE, sel || hot ? PANEL_HI : PANEL);
                        boolean wanted = c.get("wanted").getAsBoolean();
                        face(g, c, x + 2, y + 1, 20, wanted);
                        String name = str(c, "last") + " " + str(c, "first");
                        g.text(this.font, MdtWidgets.fit(this.font, name, w - 30), x + 27, y + 8,
                                wanted ? RED : TEXT, false);
                    }, () -> {
                        selectedCitizen = str(c, "id");
                        recordScroll = 0;
                        rebuildWidgets();
                    }));
        }

        JsonObject c = citizenById(selectedCitizen);
        if (c == null) return;
        String id = str(c, "id");
        int dx = cx + listW + 6;
        int dw = cw - listW - 6;
        boolean wanted = c.get("wanted").getAsBoolean();
        addRenderableWidget(smallButton(dx, base + 48, dw, 14,
                wanted ? "Revoca stato di ricercato" : "Segnala come ricercato", wanted ? GREEN : RED, () -> {
                    if (wanted) {
                        send("wanted", id, "0", "");
                    } else {
                        form = new Form("Segnala come ricercato", new String[] {"Motivo"}, new String[] {""},
                                v -> send("wanted", id, "1", v[0]));
                        rebuildWidgets();
                    }
                }));
        addRenderableWidget(smallButton(dx, base + 64, dw, 14, "+ Aggiungi reato / multa", BLUE,
                () -> openRecordForm(id, "", "", "")));
        boolean gun = c.get("gun").getAsBoolean();
        boolean drive = c.get("drive").getAsBoolean();
        int half = (dw - 4) / 2;
        addRenderableWidget(smallButton(dx, base + 80, half, 14, gun ? "Revoca porto" : "Concedi porto",
                gun ? AMBER : GREEN, () -> send("gun_license", id)));
        addRenderableWidget(smallButton(dx + half + 4, base + 80, half, 14,
                drive ? "Sospendi patente" : "Ripristina patente", drive ? AMBER : GREEN,
                () -> send("drive_license", id)));
        int dIdx = dangerIndex(c);
        addRenderableWidget(smallButton(dx, base + 96, dw, 14, "Pericolosità: " + DANGER_LABEL[dIdx],
                DANGER_COLOR[dIdx], () -> send("danger", id)));

        JsonArray recs = arr(c, "records");
        int recRowH = 20;
        int top = base + 126;
        int rows2 = Math.max(1, (cy + ch - 2 - top) / recRowH);
        recordScroll = Math.max(0, Math.min(recordScroll, Math.max(0, recs.size() - rows2)));
        for (int i = 0; i < rows2 && recordScroll + i < recs.size(); i++) {
            int idx = recs.size() - 1 - (recordScroll + i);
            JsonObject r = recs.get(idx).getAsJsonObject();
            addRenderableWidget(new Btn(dx + 2, top + i * recRowH, dw - 4, recRowH - 1,
                    (g, x, y, w, h, hot, active) -> {
                        MdtWidgets.box(g, x, y, w, h, LINE, hot ? PANEL_HI : PANEL);
                        g.text(this.font, MdtWidgets.fit(this.font, str(r, "offense"), w - 8), x + 4, y + 2, TEXT,
                                false);
                        String detail = money(r.get("fine").getAsLong()) + " - " + r.get("jail").getAsInt()
                                + " mesi - " + DATE.format(Instant.ofEpochMilli(r.get("time").getAsLong()));
                        small(g, detail, x + 4, y + 12, w - 8, DIM);
                    }, () -> {}));
        }
    }

    private static int dangerIndex(JsonObject c) {
        return switch (str(c, "danger")) {
            case "MEDIUM" -> 1;
            case "HIGH" -> 2;
            case "ARMED" -> 3;
            default -> 0;
        };
    }

    private void openRecordForm(String citizenId, String offense, String fine, String months) {
        form = new Form("Aggiungi reato / multa", new String[] {"Capo d'imputazione", "Importo ($)",
                "Mesi di carcere", "Note"}, new String[] {offense, fine, months, ""},
                v -> send("record_add", citizenId, v[0], v[1], v[2], v[3]));
        tab = T_CITIZENS;
        rebuildWidgets();
    }

    /**
     * Fototessera: la faccia vera se il cittadino è un giocatore online, altrimenti un avatar con le iniziali su un
     * colore che dipende dall'UUID. Bordo rosso se ricercato.
     */
    private void face(GuiGraphicsExtractor g, JsonObject c, int x, int y, int size, boolean wanted) {
        MdtWidgets.outline(g, x - 1, y - 1, size + 2, size + 2, wanted ? RED : FACE_BORDER);
        UUID id;
        try {
            id = UUID.fromString(str(c, "id"));
        } catch (IllegalArgumentException e) {
            id = new UUID(0, 0);
        }
        var conn = Minecraft.getInstance().getConnection();
        PlayerInfo info = conn == null ? null : conn.getPlayerInfo(id);
        if (info != null) {
            PlayerFaceExtractor.extractRenderState(g, info.getSkin(), x, y, size);
            return;
        }
        int hue = Math.floorMod(id.hashCode(), 360);
        int rgb = java.awt.Color.HSBtoRGB(hue / 360f, 0.45f, 0.55f) | 0xFF000000;
        g.fill(x, y, x + size, y + size, rgb);
        String ini = (str(c, "first").substring(0, 1) + str(c, "last").substring(0, 1)).toUpperCase(Locale.ROOT);
        float scale = size >= 32 ? 1.5f : 0.9f;
        int tw = this.font.width(ini);
        g.pose().pushMatrix();
        g.pose().translate(x + (size - tw * scale) / 2f, y + (size - 8 * scale) / 2f + 0.5f);
        g.pose().scale(scale, scale);
        g.text(this.font, ini, 0, 0, 0xFFFFFFFF, false);
        g.pose().popMatrix();
    }

    private void renderCitizens(GuiGraphicsExtractor g) {
        int listW = 124;
        int base = cy + 22;
        int dx = cx + listW + 6;
        int dw = cw - listW - 6;
        MdtWidgets.inset(g, cx, base, listW, ch - 22);
        if (filteredCitizens().isEmpty()) g.text(this.font, "Nessun risultato.", cx + 6, base + 8, DIM, false);
        JsonObject c = citizenById(selectedCitizen);
        if (c == null) {
            small(g, "Seleziona un cittadino dalla lista.", dx, base + 4, dw, DIM);
            return;
        }
        boolean wanted = c.get("wanted").getAsBoolean();
        face(g, c, dx + 1, base + 1, 32, wanted);
        g.text(this.font, MdtWidgets.fit(this.font, str(c, "first") + " " + str(c, "last"), dw - 42), dx + 40,
                base + 2, TEXT, false);
        small(g, "Nato il " + str(c, "birth"), dx + 40, base + 13, dw - 42, DIM);
        if (wanted) {
            double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 220.0);
            MdtWidgets.box(g, dx + 40, base + 23, 56, 11, MdtWidgets.alpha(RED, 0x60 + (int) (pulse * 0x9F)),
                    MdtWidgets.alpha(RED, 0x40 + (int) (pulse * 0xA0)));
            small(g, "RICERCATO", dx + 44, base + 25, 50, TEXT);
            small(g, str(c, "reason"), dx, base + 37, dw, RED);
        } else {
            small(g, "Non ricercato", dx + 40, base + 25, dw - 42, GREEN);
        }
        small(g, "PRECEDENTI (" + arr(c, "records").size() + ")", dx, base + 114, dw, DIM);
        MdtWidgets.inset(g, dx, base + 124, dw, cy + ch - (base + 124));
        if (arr(c, "records").isEmpty()) small(g, "Casellario pulito.", dx + 6, base + 130, dw - 12, DIM);
    }

    // ---- scheda 3: dispatch ----

    private void buildDispatch() {
        addRenderableWidget(new Btn(cx + cw - 172, cy - 2, 172, 14, Component.literal("! Invia Posizione / Supporto"),
                RED, () -> send("support")));
        List<JsonObject> calls = list("calls");
        java.util.Collections.reverse(calls);
        int rowH = 38;
        int rows = Math.max(1, (ch - 20) / rowH);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, calls.size() - rows)));
        for (int i = 0; i < rows && scroll + i < calls.size(); i++) {
            JsonObject call = calls.get(scroll + i);
            int y = cy + 17 + i * rowH;
            int btnW = 62;
            addRenderableWidget(new Btn(cx + 3, y, cw - btnW - 10, rowH - 2, (g, x, yy, w, h, hot, active) -> {
                MdtWidgets.box(g, x, yy, w, h, LINE, hot ? PANEL_HI : PANEL);
                String type = str(call, "type");
                int color = type.equals("MEDICAL") ? GREEN : type.equals("SHOOTING") ? RED : BLUE;
                g.fill(x, yy, x + 3, yy + h, color);
                g.text(this.font, MdtWidgets.fit(this.font, typeLabel(type) + " - " + str(call, "desc"), w - 12),
                        x + 8, yy + 3, TEXT, false);
                small(g, distance(call) + " - X " + call.get("x").getAsInt() + " Y " + call.get("y").getAsInt()
                        + " Z " + call.get("z").getAsInt() + " - " + DATE.format(Instant.ofEpochMilli(
                                call.get("time").getAsLong())), x + 8, yy + 14, w - 12, DIM);
                String units = units(call);
                small(g, units.isEmpty() ? "Nessuna unità in carico" : "Unità: " + units, x + 8, yy + 25, w - 12,
                        units.isEmpty() ? AMBER : GREEN);
            }, () -> {}));
            boolean mine = units(call).contains(officer);
            Btn take = addRenderableWidget(new Btn(cx + cw - btnW - 3, y, btnW, 18,
                    Component.literal(mine ? "In carico" : "Rispondi"), BLUE, () -> {
                        send("call_take", str(call, "id"));
                        ClientPolice.setWaypoint(new ClientPolice.Waypoint(str(call, "id"), str(call, "desc"),
                                call.get("x").getAsInt(), call.get("y").getAsInt(), call.get("z").getAsInt()));
                    }));
            take.active = !mine;
            addRenderableWidget(new Btn(cx + cw - btnW - 3, y + 19, btnW, 17, Component.literal("Chiudi"), RED,
                    () -> send("call_close", str(call, "id"))));
        }
    }

    private static String units(JsonObject call) {
        List<String> names = new ArrayList<>();
        for (JsonElement e : arr(call, "units")) names.add(e.getAsString());
        return String.join(", ", names);
    }

    private static String typeLabel(String type) {
        return switch (type) {
            case "MEDICAL" -> "MEDICO";
            case "SHOOTING" -> "SPARATORIA";
            default -> "POLIZIA";
        };
    }

    private String distance(JsonObject call) {
        var p = Minecraft.getInstance().player;
        if (p == null) return "?";
        double dx = call.get("x").getAsInt() + 0.5 - p.getX();
        double dz = call.get("z").getAsInt() + 0.5 - p.getZ();
        return (int) Math.sqrt(dx * dx + dz * dz) + " m";
    }

    private void renderDispatch(GuiGraphicsExtractor g) {
        int n = arr(data, "calls").size();
        g.text(this.font, "CHIAMATE ATTIVE (" + n + ")", cx, cy + 2, DIM, false);
        MdtWidgets.inset(g, cx, cy + 14, cw, ch - 14);
        if (n == 0) g.text(this.font, "Nessuna chiamata in corso.", cx + 8, cy + 22, DIM, false);
    }

    // ---- scheda 4: codice penale (calcolatore) ----

    private List<PenalCode.Offense> selectedOffenses() {
        return PenalCode.OFFENSES.stream().filter(o -> checkedOffenses.contains(o.name())).toList();
    }

    private String summary() {
        List<PenalCode.Offense> sel = selectedOffenses();
        long fine = sel.stream().mapToLong(PenalCode.Offense::fine).sum();
        int months = sel.stream().mapToInt(PenalCode.Offense::jailMonths).sum();
        return "Reati selezionati: " + sel.size() + "  |  Multa Totale: " + money(fine)
                + "  |  Detenzione Totale: " + months + " mesi";
    }

    private void buildPenal() {
        searchField(cx, cy, cw, "Cerca reato o categoria...");
        List<PenalCode.Offense> found = PenalCode.filter(queries[T_PENAL]);
        int rowH = 22;
        int rows = (ch - 22 - 36 - 4) / rowH;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, found.size() - rows)));
        for (int i = 0; i < rows && scroll + i < found.size(); i++) {
            PenalCode.Offense o = found.get(scroll + i);
            boolean on = checkedOffenses.contains(o.name());
            addRenderableWidget(new Btn(cx + 2, cy + 22 + i * rowH, cw - 4, rowH - 2,
                    (g, x, y, w, h, hot, active) -> {
                        MdtWidgets.box(g, x, y, w, h, on ? BLUE : LINE,
                                on ? MdtWidgets.alpha(BLUE, 0x28) : hot ? PANEL_HI : PANEL);
                        MdtWidgets.box(g, x + 4, y + 5, 10, 10, on ? BLUE : DIM, 0xFF0A1424);
                        if (on) g.fill(x + 6, y + 7, x + 12, y + 13, BLUE);
                        String s = money(o.fine()) + " - "
                                + (o.jailMonths() == 0 ? "no carcere" : o.jailMonths() + " mesi");
                        int sw = this.font.width(s);
                        g.text(this.font, MdtWidgets.fit(this.font, o.name(), w - sw - 36), x + 20, y + 2, TEXT,
                                false);
                        small(g, o.category(), x + 20, y + 12, w - sw - 36, DIM);
                        g.text(this.font, s, x + w - 6 - sw, y + 7, AMBER, false);
                    }, () -> {
                        if (!checkedOffenses.remove(o.name())) checkedOffenses.add(o.name());
                        rebuildWidgets();
                    }));
        }
        int by = cy + ch - 16;
        int half = (cw - 4) / 2;
        addRenderableWidget(new Btn(cx, by, half, 16, Component.literal("Azzera Selezione"), DIM, () -> {
            checkedOffenses.clear();
            rebuildWidgets();
        }));
        addRenderableWidget(new Btn(cx + half + 4, by, cw - half - 4, 16, Component.literal("Copia Riepilogo"), BLUE,
                this::copySummary));
    }

    /** Copia il riepilogo; se c'è un cittadino aperto, lo propone anche come nuovo reato nel suo casellario. */
    private void copySummary() {
        List<PenalCode.Offense> sel = selectedOffenses();
        if (sel.isEmpty()) return;
        Minecraft.getInstance().keyboardHandler.setClipboard(summary());
        if (citizenById(selectedCitizen) == null) return;
        String names = String.join(", ", sel.stream().map(PenalCode.Offense::name).toList());
        if (names.length() > 80) names = names.substring(0, 77) + "...";
        openRecordForm(selectedCitizen, names, String.valueOf(sel.stream().mapToLong(PenalCode.Offense::fine).sum()),
                String.valueOf(sel.stream().mapToInt(PenalCode.Offense::jailMonths).sum()));
    }

    private void renderPenal(GuiGraphicsExtractor g) {
        MdtWidgets.inset(g, cx, cy + 20, cw, ch - 22 - 36);
        if (PenalCode.filter(queries[T_PENAL]).isEmpty()) {
            g.text(this.font, "Nessun reato trovato.", cx + 8, cy + 28, DIM, false);
        }
        int sy = cy + ch - 34;
        MdtWidgets.box(g, cx, sy, cw, 15, LINE, PANEL);
        small(g, summary(), cx + 5, sy + 4, cw - 10, selectedOffenses().isEmpty() ? DIM : AMBER);
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
            Form done = form;
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

    // ---- disegno ----

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        renderBezel(g, mouseX, mouseY);
        MdtWidgets.box(g, x0, y0, W, H, LINE, BG);
        renderStatusBar(g);
        renderHeader(g);
        g.fill(x0 + SIDE, y0 + STATUS + HEADER, x0 + SIDE + 1, y0 + H, LINE);

        if (form != null) {
            renderForm(g);
        } else {
            switch (tab) {
                case T_DASH -> renderDashboard(g);
                case T_CITIZENS -> renderCitizens(g);
                case T_DISPATCH -> renderDispatch(g);
                default -> renderPenal(g);
            }
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        renderGlare(g);
    }

    private void renderHeader(GuiGraphicsExtractor g) {
        int hy = y0 + STATUS;
        g.fill(x0, hy, x0 + W, hy + HEADER, PANEL);
        g.fill(x0, hy + HEADER - 1, x0 + W, hy + HEADER, BLUE);
        int ty = hy + (HEADER - 8) / 2;
        String title = "MDT // TERMINALE OPERATIVO";
        g.text(this.font, title, x0 + 10, ty, TEXT, false);
        int badgeX = x0 + 10 + this.font.width(title) + 8;
        String badge = "RETE: GOV";
        int bw = scaledWidth(badge) + 8;
        MdtWidgets.box(g, badgeX, hy + 5, bw, 11, MdtWidgets.alpha(BLUE, 0xA0), MdtWidgets.alpha(BLUE, 0x20));
        scaled(g, badge, badgeX + 4, hy + 8, BLUE);
        int room = (x0 + W - 28) - (badgeX + bw + 10);
        String who = MdtWidgets.fit(this.font, "Op. " + officer, room);
        g.text(this.font, who, x0 + W - 28 - this.font.width(who), ty, DIM, false);
    }

    // ---- scocca del tablet ----

    private static final int SHELL = 0xFF1E2229;
    private static final int SHELL_EDGE = 0xFF323842;
    private static final int SHELL_RECESS = 0xFF090C10;
    private static final int BUMPER = 0xFF141619;
    private static final int HOME = 0xFF3A414D;

    /** Rettangolo con i 2x2 pixel di ogni angolo tagliati. */
    private static void chamfered(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x + 2, y, x + w - 2, y + h, color);
        g.fill(x, y + 2, x + w, y + h - 2, color);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
    }

    private int homeX() {
        return x0 + W / 2 - 20;
    }

    private int homeY() {
        return y0 + H + BEZEL - 8;
    }

    private boolean overHome(double mx, double my) {
        return mx >= homeX() && mx < homeX() + 40 && my >= homeY() && my < homeY() + 4;
    }

    private void renderBezel(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int fx = x0 - BEZEL;
        int fy = y0 - BEZEL;
        int fw = W + 2 * BEZEL;
        int fh = H + 2 * BEZEL;
        chamfered(g, fx, fy, fw, fh, SHELL_EDGE);
        chamfered(g, fx + 1, fy + 1, fw - 2, fh - 2, SHELL);
        int b = 14;
        chamfered(g, fx, fy, b, b, BUMPER);
        chamfered(g, fx + fw - b, fy, b, b, BUMPER);
        chamfered(g, fx, fy + fh - b, b, b, BUMPER);
        chamfered(g, fx + fw - b, fy + fh - b, b, b, BUMPER);
        g.fill(x0 - 2, y0 - 2, x0 + W + 2, y0 + H + 2, SHELL_RECESS);

        int mid = x0 + W / 2;
        g.fill(mid - 8, fy + 4, mid - 4, fy + 8, 0xFF050505);
        g.fill(mid - 8, fy + 4, mid - 7, fy + 5, 0xFF6EC1FF);
        // Verde fisso a terminale acceso; rosso e ambra alternati se c'è almeno una chiamata attiva.
        int led = 0xFF00E676;
        if (!arr(data, "calls").isEmpty()) led = (System.currentTimeMillis() / 400) % 2 == 0 ? 0xFFFF1744 : 0xFFFFB300;
        g.fill(mid + 2, fy + 4, mid + 5, fy + 7, led);

        g.fill(homeX(), homeY(), homeX() + 40, homeY() + 4, overHome(mouseX, mouseY) ? 0xFF5B6575 : HOME);
    }

    /** Fascia diagonale di luce sul vetro. */
    private void renderGlare(GuiGraphicsExtractor g) {
        for (int r = 0; r < H; r++) {
            int x = x0 + (int) (W * 0.62) - r * 3 / 4;
            g.fill(Math.max(x0, x), y0 + r, Math.min(x0 + W, x + 46), y0 + r + 1, 0x05FFFFFF);
            g.fill(Math.max(x0, x + 56), y0 + r, Math.min(x0 + W, x + 62), y0 + r + 1, 0x05FFFFFF);
        }
    }

    // ---- barra di stato ----

    private void scaled(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.75f, 0.75f);
        g.text(this.font, s, 0, 0, color, false);
        g.pose().popMatrix();
    }

    private int scaledWidth(String s) {
        return (int) Math.ceil(this.font.width(s) * 0.75f);
    }

    private String worldTime() {
        var level = Minecraft.getInstance().level;
        if (level == null) return "--:--";
        long t = level.getDefaultClockTime() % 24000L;
        int hour = (int) ((t / 1000 + 6) % 24);
        int minute = (int) ((t % 1000) * 60 / 1000);
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    private void renderStatusBar(GuiGraphicsExtractor g) {
        g.fill(x0, y0, x0 + W, y0 + STATUS, 0xFF060A12);
        int ty = y0 + 3;
        int x = x0 + 6;
        String a = "Q-OS  |  LTE ";
        scaled(g, a, x, ty, DIM);
        x += scaledWidth(a);
        for (int i = 0; i < 4; i++) {
            int h = 2 + i * 2;
            g.fill(x + i * 3, y0 + 9 - h, x + i * 3 + 2, y0 + 9, i < 3 ? GREEN : BLUE);
        }
        x += 14;
        scaled(g, " |  GPS", x, ty, DIM);

        int right = x0 + W - 6;
        int bx = right - 14;
        MdtWidgets.outline(g, bx, y0 + 3, 12, 6, DIM);
        g.fill(bx + 12, y0 + 5, bx + 14, y0 + 7, DIM);
        g.fill(bx + 1, y0 + 4, bx + 11, y0 + 8, GREEN);
        String time = worldTime();
        scaled(g, time, bx - 5 - scaledWidth(time), ty, TEXT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && overHome(event.x(), event.y())) {
            MdtWidgets.click();
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (scrollY != 0 && form == null) {
            int step = -(int) Math.signum(scrollY);
            if (tab == T_CITIZENS && mx > cx + 130) recordScroll = Math.max(0, recordScroll + step);
            else scroll = Math.max(0, scroll + step);
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }
}
