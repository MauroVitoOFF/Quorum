package dev.qrm.police.client.mdt.citizens;

import static dev.qrm.police.client.mdt.ui.MdtText.DATE;
import static dev.qrm.police.client.mdt.ui.MdtText.money;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.AMBER;
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
import dev.qrm.police.client.mdt.MdtForm;
import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.MdtPage;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Btn;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import dev.qrm.police.logic.FineStatus;
import dev.qrm.police.logic.RecordKind;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/** Cittadini: elenco con ricerca, scheda personale, voci dello storico e procedure di multa e arresto. */
public final class CitizensPage extends MdtPage {
    private final FineWizard fine;
    private final ArrestWizard arrest;

    public CitizensPage(MdtHost host) {
        super(host);
        fine = new FineWizard(host, this);
        arrest = new ArrestWizard(host, this);
    }

    @Override
    public void sync() {
        super.sync();
        fine.sync();
        arrest.sync();
    }

    /** Dalla dashboard: l'elenco già ristretto ai ricercati. */
    public void showWanted() {
        onlyWanted = true;
        selectedCitizen = null;
        openRecordId = null;
        rebuildWidgets();
    }

    /** Chiamata dalle procedure guidate quando partono: la voce dello storico aperta si chiude. */
    void closeRecord() {
        openRecordId = null;
    }

    @Override
    public void onEnter() {
        scroll = 0;
        recordScroll = 0;
    }

    /** Un secondo clic su "Cittadini" riporta all'elenco. */
    @Override
    public void onReselect() {
        selectedCitizen = null;
        openRecordId = null;
        fine.reset();
        arrest.reset();
        onEnter();
    }

    @Override
    public boolean onScroll(double mx, double my, int step) {
        if (fine.active()) return fine.onScroll(mx, my, step);
        if (arrest.active()) return arrest.onScroll(mx, my, step);
        if (selectedCitizen != null) {
            recordScroll = Math.max(0, recordScroll + step);
            return true;
        }
        return super.onScroll(mx, my, step);
    }

    private static final String[] DANGER_LABEL = {"Basso", "Medio", "Alto", "Armato e pericoloso"};
    private static final int[] DANGER_COLOR = {GREEN, AMBER, RED, RED};
    private static final int FACE_BORDER = 0xFF3A5A80;
    private int recordScroll;

    /** La voce dello storico aperta a pagina intera, o null. */
    private String openRecordId;

    /** 0 = tutte le voci, altrimenti l'indice in {@link RecordKind#values()} più uno. */
    private int recordFilter;
    private String selectedCitizen;
    private boolean onlyWanted;
    private static final int FILE_TOP = 86;
    private static final int FILE_COL = 100;
    private static final int FACTS_W = 104;
    private static final int[] STATUS_COLOR = {AMBER, GREEN, DIM};

    /** I numeri che all'agente servono a colpo d'occhio. Le voci archiviate non contano. */
    private record Facts(long unpaid, int unpaidCount, int fines, long fineTotal, int arrests, int months,
            int archived, int weapons, int flaggedWeapons) {}

    private List<JsonObject> filteredCitizens() {
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<JsonObject> out = new ArrayList<>();
        for (JsonObject c : list("citizens")) {
            if (onlyWanted && !c.get("wanted").getAsBoolean()) continue;
            String name = (str(c, "first") + " " + str(c, "last")).toLowerCase(Locale.ROOT);
            String rev = (str(c, "last") + " " + str(c, "first")).toLowerCase(Locale.ROOT);
            if (q.isEmpty() || name.contains(q) || rev.contains(q)) out.add(c);
        }
        return out;
    }

    @Override
    public void build() {
        JsonObject c = citizenById(selectedCitizen);
        if (c == null) {
            selectedCitizen = null;
            fine.reset();
            arrest.reset();
            buildCitizenList();
        } else if (fine.active()) {
            fine.build();
        } else if (arrest.active()) {
            arrest.build();
        } else if (openRecord(c) != null) {
            buildRecordView(c, openRecord(c));
        } else {
            buildCitizenFile(c);
        }
    }

    /** Elenco: una riga larga per cittadino. Un clic apre la scheda a pagina intera. */
    private void buildCitizenList() {
        searchField(cx, cy, cw - 106, "Nome o cognome...");
        addRenderableWidget(smallButton(cx + cw - 102, cy, 102, 16,
                onlyWanted ? "[x] Solo Ricercati" : "[ ] Solo Ricercati", onlyWanted ? RED : DIM, () -> {
                    onlyWanted = !onlyWanted;
                    scroll = 0;
                    rebuildWidgets();
                }));
        int base = cy + 22;
        List<JsonObject> found = filteredCitizens();
        int rowH = 28;
        int rows = (ch - 22 - 4) / rowH;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, found.size() - rows)));
        for (int i = 0; i < rows && scroll + i < found.size(); i++) {
            JsonObject c = found.get(scroll + i);
            addRenderableWidget(new Btn(cx + 2, base + 2 + i * rowH, cw - 4, rowH - 2,
                    (g, x, y, w, h, hot, active) -> {
                        boolean wanted = c.get("wanted").getAsBoolean();
                        MdtWidgets.box(g, x, y, w, h, hot ? BLUE : LINE, hot ? PANEL_HI : PANEL);
                        face(g, c, x + 4, y + 3, 20, wanted);
                        String name = str(c, "last") + " " + str(c, "first");
                        g.text(font, MdtWidgets.fit(font, name, w - 100), x + 32, y + 4,
                                wanted ? RED : TEXT, false);
                        small(g, "ID " + str(c, "idn") + " - Nato il " + str(c, "birth"), x + 32, y + 15, w - 100, DIM);
                        if (wanted) small(g, "RICERCATO", x + w - 56, y + 9, 52, RED);
                    }, () -> {
                        selectedCitizen = str(c, "id");
                        openRecordId = null;
                        recordScroll = 0;
                        recordFilter = 0;
                        rebuildWidgets();
                    }));
        }
    }

    private Facts factsOf(JsonObject c) {
        String id = str(c, "id");
        long unpaid = 0;
        int unpaidCount = 0;
        for (JsonObject f : list("fines")) {
            if (str(f, "cid").equals(id) && str(f, "status").equals("UNPAID")) {
                unpaid += f.get("amount").getAsLong();
                unpaidCount++;
            }
        }
        int fines = 0;
        long fineTotal = 0;
        int arrests = 0;
        int months = 0;
        int archived = 0;
        for (JsonElement e : arr(c, "records")) {
            JsonObject r = e.getAsJsonObject();
            if (r.get("arch").getAsBoolean()) {
                archived++;
                continue;
            }
            RecordKind kind = RecordKind.parse(str(r, "kind"), RecordKind.FINE);
            if (kind == RecordKind.FINE) {
                fines++;
                fineTotal += r.get("fine").getAsLong();
            } else if (kind == RecordKind.ARREST) {
                arrests++;
                months += r.get("jail").getAsInt();
            }
        }
        int weapons = 0;
        int flagged = 0;
        for (JsonObject w : list("weapons")) {
            if (!str(w, "ownerId").equals(id)) continue;
            weapons++;
            String st = str(w, "status");
            if (st.equals("STOLEN") || st.equals("SEIZED")) flagged++;
        }
        return new Facts(unpaid, unpaidCount, fines, fineTotal, arrests, months, archived, weapons, flagged);
    }

    /** Le multe emesse, per id della voce di storico a cui sono collegate. */
    private java.util.Map<String, JsonObject> finesByRecord() {
        java.util.Map<String, JsonObject> out = new java.util.HashMap<>();
        for (JsonObject f : list("fines")) {
            String rec = str(f, "rec");
            if (!rec.isEmpty()) out.put(rec, f);
        }
        return out;
    }

    private static int filterOptions() {
        return RecordKind.values().length + 3;
    }

    private static boolean archivedFilter(int filter) {
        return filter == RecordKind.values().length + 1;
    }

    private static boolean unpaidFilter(int filter) {
        return filter == RecordKind.values().length + 2;
    }

    private static String filterLabel(int filter) {
        if (filter == 0) return "Attive";
        if (unpaidFilter(filter)) return "Da pagare";
        if (archivedFilter(filter)) return "Archiviate";
        return RecordKind.values()[filter - 1].label();
    }

    /** La scheda del cittadino: prima ciò che conta (ricercato, multe da pagare, patente), poi azioni e storico. */
    private void buildCitizenFile(JsonObject c) {
        String id = str(c, "id");
        backButton("< Elenco", () -> {
            selectedCitizen = null;
            rebuildWidgets();
        });
        Facts k = factsOf(c);
        if (k.unpaidCount() > 0) {
            // La riga "Da pagare" filtra lo storico sulle multe non ancora pagate.
            addRenderableWidget(new Btn(cx + cw - FACTS_W - 6, cy + 22, FACTS_W + 4, 10,
                    (g, x, y, w, h, hot, active) -> {
                        if (hot) g.fill(x, y, x + w, y + h, MdtWidgets.alpha(RED, 0x30));
                    }, () -> {
                        recordFilter = RecordKind.values().length + 2;
                        recordScroll = 0;
                        rebuildWidgets();
                    }));
        }
        boolean wanted = c.get("wanted").getAsBoolean();
        boolean gun = c.get("gun").getAsBoolean();
        boolean drive = c.get("drive").getAsBoolean();
        int top = cy + FILE_TOP;
        int y = top;
        addRenderableWidget(smallButton(cx, y, FILE_COL, 14,
                wanted ? "Revoca stato di ricercato" : "Segnala come ricercato", wanted ? GREEN : RED, () -> {
                    if (wanted) {
                        send("wanted", id, "0", "");
                    } else {
                        host.setForm(new MdtForm("Segnala come ricercato", new String[] {"Motivo"}, new String[] {""},
                                v -> send("wanted", id, "1", v[0])));
                        rebuildWidgets();
                    }
                }));
        y += 16;
        int half = (FILE_COL - 2) / 2;
        String[] actionLabels = {"Multa", "Arresto", "Rapporto", "Nota"};
        for (int i = 0; i < actionLabels.length; i++) {
            final int which = i;
            addRenderableWidget(smallButton(cx + (i % 2) * (half + 2), y + (i / 2) * 16, half, 14, actionLabels[i],
                    BLUE, () -> {
                        if (which == 0) fine.start(id);
                        else if (which == 1) arrest.start(id);
                        else openActionForm(id, which, "", "", "");
                    }));
        }
        y += 32;
        addRenderableWidget(smallButton(cx, y, FILE_COL, 14, gun ? "Revoca porto d'armi" : "Concedi porto d'armi",
                gun ? AMBER : GREEN, () -> send("gun_license", id)));
        y += 16;
        addRenderableWidget(smallButton(cx, y, FILE_COL, 14, drive ? "Sospendi patente" : "Ripristina patente",
                drive ? AMBER : GREEN, () -> send("drive_license", id)));
        y += 16;
        int dIdx = dangerIndex(c);
        addRenderableWidget(smallButton(cx, y, FILE_COL, 14, "Pericolosità: " + DANGER_LABEL[dIdx],
                DANGER_COLOR[dIdx], () -> send("danger", id)));

        int rx = cx + FILE_COL + 6;
        int rw = cw - FILE_COL - 6;
        List<JsonObject> recs = recordsOf(c);
        addRenderableWidget(smallButton(rx, top, rw, 14, "Storico (" + recs.size() + "): " + filterLabel(recordFilter),
                DIM, () -> {
                    recordFilter = (recordFilter + 1) % filterOptions();
                    recordScroll = 0;
                    rebuildWidgets();
                }));

        java.util.Map<String, JsonObject> fineOf = finesByRecord();
        int rowH = 26;
        int listTop = top + 18;
        int rows = Math.max(1, (cy + ch - listTop) / rowH);
        recordScroll = Math.max(0, Math.min(recordScroll, Math.max(0, recs.size() - rows)));
        for (int i = 0; i < rows && recordScroll + i < recs.size(); i++) {
            JsonObject r = recs.get(recs.size() - 1 - (recordScroll + i));
            RecordKind kind = RecordKind.parse(str(r, "kind"), RecordKind.FINE);
            boolean archived = r.get("arch").getAsBoolean();
            JsonObject linked = fineOf.get(str(r, "id"));
            addRenderableWidget(new Btn(rx, listTop + i * rowH, rw, rowH - 2, (g, x, yy, w, h, hot, active) -> {
                MdtWidgets.box(g, x, yy, w, h, hot ? BLUE : LINE, hot ? PANEL_HI : PANEL);
                g.fill(x, yy, x + 3, yy + h, archived ? DIM : kindColor(kind));
                int textColor = archived ? DIM : TEXT;
                String amount = kind == RecordKind.FINE || kind == RecordKind.ARREST
                        ? money(r.get("fine").getAsLong()) : "";
                int amountW = amount.isEmpty() ? 0 : font.width(amount) + 6;
                g.text(font, MdtWidgets.fit(font, kind.label() + ": " + str(r, "offense"), w - 12 - amountW),
                        x + 7, yy + 2, textColor, false);
                if (!amount.isEmpty()) {
                    g.text(font, amount, x + w - 5 - font.width(amount), yy + 2,
                            archived ? DIM : AMBER, false);
                }
                String status = linked == null ? "" : FineStatus.parse(str(linked, "status"), FineStatus.UNPAID).label();
                int statusW = status.isEmpty() ? 0 : (int) (font.width(status) * 0.8f) + 6;
                String notes = str(r, "notes");
                String line = DATE.format(Instant.ofEpochMilli(r.get("time").getAsLong())) + " - " + str(r, "officer")
                        + (notes.isEmpty() ? "" : " - " + notes);
                small(g, line, x + 7, yy + 13, w - 12 - statusW, DIM);
                if (!status.isEmpty()) {
                    int sc = STATUS_COLOR[FineStatus.parse(str(linked, "status"), FineStatus.UNPAID).ordinal()];
                    small(g, status, x + w - 5 - (int) (font.width(status) * 0.8f), yy + 13, 70, sc);
                }
            }, () -> {
                openRecordId = str(r, "id");
                rebuildWidgets();
            }));
        }
    }

    /** La voce aperta del cittadino, o null. */
    private JsonObject openRecord(JsonObject c) {
        if (openRecordId == null) return null;
        for (JsonElement e : arr(c, "records")) {
            JsonObject r = e.getAsJsonObject();
            if (str(r, "id").equals(openRecordId)) return r;
        }
        openRecordId = null;
        return null;
    }

    private void buildRecordView(JsonObject c, JsonObject r) {
        backButton("< Scheda", () -> {
            openRecordId = null;
            rebuildWidgets();
        });
        String citizenId = str(c, "id");
        String recordId = str(r, "id");
        boolean archived = r.get("arch").getAsBoolean();
        JsonObject linked = finesByRecord().get(recordId);
        Btn archive = addRenderableWidget(new Btn(cx + cw - 96, cy, 96, 16,
                Component.literal(archived ? "Ripristina" : "Archivia"), archived ? GREEN : AMBER,
                () -> send("record_archive", citizenId, recordId, archived ? "0" : "1")));
        // Una multa non pagata resta in fedina finché non viene pagata o annullata.
        archive.active = archived || linked == null || !str(linked, "status").equals("UNPAID");
        if (linked != null && str(linked, "status").equals("UNPAID")) {
            String fineId = str(linked, "id");
            addRenderableWidget(new Btn(cx + 8, cy + ch - 20, 110, 16, Component.literal("Segna pagata"), GREEN,
                    () -> send("fine_status", fineId, "PAID")));
            addRenderableWidget(new Btn(cx + 122, cy + ch - 20, 110, 16, Component.literal("Annulla multa"), RED,
                    () -> send("fine_status", fineId, "CANCELED")));
        }
    }

    private List<JsonObject> recordsOf(JsonObject c) {
        java.util.Map<String, JsonObject> fineOf = unpaidFilter(recordFilter) ? finesByRecord() : java.util.Map.of();
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : arr(c, "records")) {
            JsonObject r = e.getAsJsonObject();
            if (r.get("arch").getAsBoolean() != archivedFilter(recordFilter)) continue;
            if (unpaidFilter(recordFilter)) {
                JsonObject linked = fineOf.get(str(r, "id"));
                if (linked == null || !str(linked, "status").equals("UNPAID")) continue;
            } else if (recordFilter >= 1 && !archivedFilter(recordFilter)
                    && RecordKind.parse(str(r, "kind"), RecordKind.FINE) != RecordKind.values()[recordFilter - 1]) {
                continue;
            }
            out.add(r);
        }
        return out;
    }

    private static int kindColor(RecordKind kind) {
        return switch (kind) {
            case FINE -> AMBER;
            case ARREST, WARRANT -> RED;
            case REPORT -> BLUE;
            case NOTE -> DIM;
        };
    }

    /** Importo, pena, data e agente su una riga (le note vanno a parte). */
    private static String recordDetail(JsonObject r, RecordKind kind) {
        StringBuilder sb = new StringBuilder();
        if (kind == RecordKind.FINE || kind == RecordKind.ARREST) sb.append(money(r.get("fine").getAsLong()));
        if (kind == RecordKind.ARREST) sb.append(" - ").append(r.get("jail").getAsInt()).append(" mesi");
        if (sb.length() > 0) sb.append(" - ");
        sb.append(DATE.format(Instant.ofEpochMilli(r.get("time").getAsLong()))).append(" - ").append(str(r, "officer"));
        return sb.toString();
    }

    private static int dangerIndex(JsonObject c) {
        return switch (str(c, "danger")) {
            case "MEDIUM" -> 1;
            case "HIGH" -> 2;
            case "ARMED" -> 3;
            default -> 0;
        };
    }

    /** 2 rapporto, 3 nota (multe e arresti hanno la loro procedura guidata). */
    private void openActionForm(String id, int which, String title, String amount, String months) {
        host.setForm(switch (which) {
            case 2 -> new MdtForm("Crea rapporto", new String[] {"Titolo", "Dettagli"}, new String[] {title, ""},
                    v -> send("record_add", id, "REPORT", v[0], "0", "0", v[1]));
            default -> new MdtForm("Aggiungi nota", new String[] {"Nota"}, new String[] {title},
                    v -> send("record_add", id, "NOTE", v[0], "0", "0", ""));
        });
        openRecordId = null;
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
        // La skin è quella del giocatore proprietario, letta a ogni disegno: se la cambia, cambia anche qui.
        UUID skinOwner = id;
        if (c.has("player") && !str(c, "player").isEmpty()) {
            try {
                skinOwner = UUID.fromString(str(c, "player"));
            } catch (IllegalArgumentException e) {
                // resta l'id del cittadino
            }
        }
        var conn = Minecraft.getInstance().getConnection();
        PlayerInfo info = conn == null ? null : conn.getPlayerInfo(skinOwner);
        if (info != null) {
            PlayerFaceExtractor.extractRenderState(g, info.getSkin(), x, y, size);
            return;
        }
        int hue = Math.floorMod(id.hashCode(), 360);
        int rgb = java.awt.Color.HSBtoRGB(hue / 360f, 0.45f, 0.55f) | 0xFF000000;
        g.fill(x, y, x + size, y + size, rgb);
        String ini = (str(c, "first").substring(0, 1) + str(c, "last").substring(0, 1)).toUpperCase(Locale.ROOT);
        float scale = size >= 32 ? 1.5f : 0.9f;
        int tw = font.width(ini);
        g.pose().pushMatrix();
        g.pose().translate(x + (size - tw * scale) / 2f, y + (size - 8 * scale) / 2f + 0.5f);
        g.pose().scale(scale, scale);
        g.text(font, ini, 0, 0, 0xFFFFFFFF, false);
        g.pose().popMatrix();
    }

    @Override
    public void render(GuiGraphicsExtractor g) {
        JsonObject c = citizenById(selectedCitizen);
        if (c == null) {
            MdtWidgets.inset(g, cx, cy + 22, cw, ch - 22);
            if (filteredCitizens().isEmpty()) g.text(font, "Nessun risultato.", cx + 8, cy + 32, DIM, false);
            return;
        }
        if (fine.active()) {
            fine.render(g);
            return;
        }
        if (arrest.active()) {
            arrest.render(g);
            return;
        }
        JsonObject rec = openRecord(c);
        if (rec != null) {
            renderRecordView(g, c, rec);
            return;
        }
        renderCitizenFile(g, c);
    }

    private void renderCitizenFile(GuiGraphicsExtractor g, JsonObject c) {
        boolean wanted = c.get("wanted").getAsBoolean();
        Facts k = factsOf(c);
        small(g, "SCHEDA CITTADINO", cx + 86, cy + 5, cw - 86 - 100, DIM);

        // Intestazione: chi è, e a destra le cose da sapere subito.
        MdtWidgets.box(g, cx, cy + 20, cw, 46, LINE, PANEL);
        face(g, c, cx + 6, cy + 24, 38, wanted);
        int tx = cx + 52;
        int textW = cw - 58 - FACTS_W - 6;
        g.text(font, MdtWidgets.fit(font, str(c, "first") + " " + str(c, "last"), textW), tx, cy + 24,
                TEXT, false);
        small(g, "Nato il " + str(c, "birth"), tx, cy + 36, textW, DIM);
        small(g, "ID " + str(c, "idn"), tx, cy + 45, textW, DIM);
        int dIdx = dangerIndex(c);
        small(g, "Pericolosità: " + DANGER_LABEL[dIdx], tx, cy + 54, textW, DANGER_COLOR[dIdx]);

        int fx = cx + cw - FACTS_W - 4;
        g.fill(fx - 4, cy + 22, fx - 3, cy + 64, LINE);
        boolean drive = c.get("drive").getAsBoolean();
        boolean gun = c.get("gun").getAsBoolean();
        small(g, k.unpaidCount() == 0 ? "Nessuna multa da pagare"
                : "Da pagare: " + money(k.unpaid()) + " (" + k.unpaidCount() + ")", fx, cy + 24, FACTS_W,
                k.unpaidCount() == 0 ? GREEN : RED);
        small(g, "Patente: " + (drive ? "valida" : "sospesa"), fx, cy + 34, FACTS_W, drive ? GREEN : AMBER);
        small(g, "Porto d'armi: " + (gun ? "sì" : "no"), fx, cy + 44, FACTS_W, gun ? GREEN : DIM);
        small(g, k.weapons() == 0 ? "Nessuna arma registrata"
                : "Armi: " + k.weapons() + (k.flaggedWeapons() > 0 ? " (" + k.flaggedWeapons() + " segnalate)" : ""),
                fx, cy + 54, FACTS_W, k.flaggedWeapons() > 0 ? RED : TEXT);

        // Mandato: la prima cosa che l'agente deve vedere.
        if (wanted) {
            double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 220.0);
            MdtWidgets.box(g, cx, cy + 68, cw, 13, MdtWidgets.alpha(RED, 0x60 + (int) (pulse * 0x9F)),
                    MdtWidgets.alpha(RED, 0x30 + (int) (pulse * 0x50)));
            small(g, "RICERCATO - " + str(c, "reason"), cx + 5, cy + 71, cw - 10, TEXT);
        } else {
            MdtWidgets.box(g, cx, cy + 68, cw, 13, MdtWidgets.alpha(GREEN, 0x60), MdtWidgets.alpha(GREEN, 0x14));
            small(g, "Nessun mandato attivo", cx + 5, cy + 71, cw - 10, GREEN);
        }

        // Riepilogo della fedina sotto i pulsanti.
        int sy = cy + FILE_TOP + 98;
        small(g, "Multe: " + k.fines() + " (" + money(k.fineTotal()) + ")", cx, sy, FILE_COL, TEXT);
        small(g, "Arresti: " + k.arrests() + " (" + k.months() + " mesi)", cx, sy + 10, FILE_COL, TEXT);

        int rx = cx + FILE_COL + 6;
        int rw = cw - FILE_COL - 6;
        int listTop = cy + FILE_TOP + 18;
        MdtWidgets.inset(g, rx, listTop, rw, cy + ch - listTop);
        if (recordsOf(c).isEmpty()) {
            String empty = archivedFilter(recordFilter) ? "Nessuna voce archiviata."
                    : unpaidFilter(recordFilter) ? "Nessuna multa da pagare." : "Fedina pulita.";
            if (recordFilter == 0 && k.archived() > 0) empty += " (" + k.archived() + " archiviate)";
            small(g, empty, rx + 6, listTop + 6, rw - 12, DIM);
        }
    }

    /** Una voce dello storico a pagina intera, con le note per esteso. */
    private void renderRecordView(GuiGraphicsExtractor g, JsonObject c, JsonObject r) {
        RecordKind kind = RecordKind.parse(str(r, "kind"), RecordKind.FINE);
        boolean archived = r.get("arch").getAsBoolean();
        JsonObject linked = finesByRecord().get(str(r, "id"));
        small(g, str(c, "first") + " " + str(c, "last") + " - ID " + str(c, "idn"), cx + 86, cy + 5, cw - 86 - 200,
                DIM);
        MdtWidgets.box(g, cx, cy + 20, cw, ch - 20, LINE, PANEL);
        g.fill(cx, cy + 20, cx + 3, cy + ch, archived ? DIM : kindColor(kind));
        int x = cx + 10;
        int w = cw - 18;
        int y = cy + 26;
        small(g, kind.label().toUpperCase(Locale.ROOT) + (archived ? " - ARCHIVIATA" : ""), x, y, w,
                archived ? DIM : kindColor(kind));
        y += 12;
        for (String line : wrap(str(r, "offense"), w)) {
            g.text(font, MdtWidgets.fit(font, line, w), x, y, archived ? DIM : TEXT, false);
            y += 11;
        }
        y += 4;
        small(g, "Data: " + DATE.format(Instant.ofEpochMilli(r.get("time").getAsLong())), x, y, w, DIM);
        y += 11;
        small(g, "Agente: " + str(r, "officer"), x, y, w, DIM);
        y += 11;
        if (kind == RecordKind.FINE || kind == RecordKind.ARREST) {
            small(g, "Importo: " + money(r.get("fine").getAsLong()), x, y, w, DIM);
            y += 11;
        }
        if (kind == RecordKind.ARREST) {
            small(g, "Pena: " + r.get("jail").getAsInt() + " mesi di carcere", x, y, w, DIM);
            y += 11;
        }
        if (linked != null) {
            FineStatus st = FineStatus.parse(str(linked, "status"), FineStatus.UNPAID);
            small(g, "Pagamento: " + st.label() + (st == FineStatus.UNPAID ? " - da pagare o annullare prima di archiviare" : ""), x, y, w,
                    STATUS_COLOR[st.ordinal()]);
            y += 11;
        }
        if (archived) {
            small(g, "Archiviata da " + str(r, "archBy") + ": non conta più nella fedina.", x, y, w, DIM);
            y += 11;
        }
        String notes = str(r, "notes");
        y += 6;
        small(g, "NOTE", x, y, w, DIM);
        y += 11;
        if (notes.isEmpty()) {
            small(g, "Nessuna nota.", x, y, w, DIM);
            return;
        }
        for (String line : wrap(notes, w)) {
            if (y > cy + ch - 24) break;
            g.text(font, MdtWidgets.fit(font, line, w), x, y, TEXT, false);
            y += 11;
        }
    }
}
