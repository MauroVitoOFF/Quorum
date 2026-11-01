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

import com.google.gson.JsonObject;
import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.MdtPage;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Btn;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Field;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import dev.qrm.police.logic.PenalCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Procedura guidata dell'arresto: reati, riepilogo e conferma. */
final class ArrestWizard extends MdtPage {
    private final CitizensPage owner;

    ArrestWizard(MdtHost host, CitizensPage owner) {
        super(host);
        this.owner = owner;
    }

    /** Passo dell'arresto: 0 nessuno, 1 reati, 2 riepilogo. */
    private int arrestStep;
    private String arrestCitizen;
    private int arrestCat;

    private final List<String> arrestPicked = new ArrayList<>();

    private String arrestNotes = "";

    private static List<String> penalCategories() {
        return PenalCode.OFFENSES.stream().map(PenalCode.Offense::category).distinct().toList();
    }

    private List<PenalCode.Offense> arrestOffenses() {
        return PenalCode.OFFENSES.stream().filter(o -> arrestPicked.contains(o.name())).toList();
    }

    boolean active() {
        return arrestStep > 0;
    }

    void reset() {
        arrestStep = 0;
    }

    void start(String citizenId) {
        arrestCitizen = citizenId;
        arrestCat = 0;
        arrestPicked.clear();
        arrestNotes = "";
        arrestStep = 1;
        owner.closeRecord();
        scroll = 0;
        host.setForm(null);
        rebuildWidgets();
    }

    void end() {
        arrestStep = 0;
        arrestCitizen = null;
        arrestPicked.clear();
        scroll = 0;
        rebuildWidgets();
    }

    @Override
    public void build() {
        if (arrestStep == 1) {
            backButton("< Annulla", this::end);
            List<String> cats = penalCategories();
            int gap = 2;
            int bw = (cw - gap * (cats.size() - 1)) / cats.size();
            for (int i = 0; i < cats.size(); i++) {
                final int idx = i;
                addRenderableWidget(smallButton(cx + i * (bw + gap), cy + 20, bw, 14, cats.get(i).split(" ")[0],
                        i == arrestCat ? BLUE : DIM, () -> {
                            arrestCat = idx;
                            scroll = 0;
                            rebuildWidgets();
                        }));
            }
            List<PenalCode.Offense> offenses = PenalCode.OFFENSES.stream()
                    .filter(o -> o.category().equals(cats.get(arrestCat))).toList();
            int top = cy + 38;
            int rowH = 22;
            int rows = (cy + ch - 24 - top) / rowH;
            scroll = Math.max(0, Math.min(scroll, Math.max(0, offenses.size() - rows)));
            for (int i = 0; i < rows && scroll + i < offenses.size(); i++) {
                PenalCode.Offense o = offenses.get(scroll + i);
                boolean on = arrestPicked.contains(o.name());
                addRenderableWidget(new Btn(cx + 2, top + i * rowH, cw - 4, rowH - 2, (g, x, y, w, h, hot, active) -> {
                    MdtWidgets.box(g, x, y, w, h, on ? BLUE : LINE,
                            on ? MdtWidgets.alpha(BLUE, 0x28) : hot ? PANEL_HI : PANEL);
                    MdtWidgets.box(g, x + 4, y + 5, 10, 10, on ? BLUE : DIM, 0xFF0A1424);
                    if (on) g.fill(x + 6, y + 7, x + 12, y + 13, BLUE);
                    String s = money(o.fine()) + " - " + (o.jailMonths() == 0 ? "no carcere" : o.jailMonths() + " mesi");
                    int sw = font.width(s);
                    g.text(font, MdtWidgets.fit(font, o.name(), w - sw - 36), x + 20, y + 6, TEXT, false);
                    g.text(font, s, x + w - 6 - sw, y + 6, AMBER, false);
                }, () -> {
                    if (!arrestPicked.remove(o.name())) arrestPicked.add(o.name());
                    rebuildWidgets();
                }));
            }
            Btn next = addRenderableWidget(new Btn(cx + cw - 100, cy + ch - 18, 100, 16, Component.literal("Avanti"),
                    GREEN, () -> {
                        arrestStep = 2;
                        rebuildWidgets();
                    }));
            next.active = !arrestPicked.isEmpty();
            return;
        }
        backButton("< Indietro", () -> {
            arrestStep = 1;
            rebuildWidgets();
        });
        Field f = new Field(font, cx, cy + 130, cw, 16, Component.literal("Note (facoltative)"));
        f.setMaxLength(200);
        f.setValue(arrestNotes);
        f.setResponder(v -> arrestNotes = v);
        addRenderableWidget(f);
        addRenderableWidget(new Btn(cx + cw - 120, cy + ch - 18, 120, 16, Component.literal("Registra arresto"), RED,
                () -> {
                    send("arrest_register", arrestCitizen, String.join("|", arrestPicked), arrestNotes);
                    end();
                }));
    }

    @Override
    public void render(GuiGraphicsExtractor g) {
        small(g, arrestStep == 1 ? "PASSO 1/2 Reati" : "PASSO 2/2 Riepilogo", cx + cw - 92, cy + 5, 92, DIM);
        JsonObject c = citizenById(arrestCitizen);
        String who = c == null ? "?" : str(c, "first") + " " + str(c, "last");
        List<PenalCode.Offense> picked = arrestOffenses();
        long fine = picked.stream().mapToLong(PenalCode.Offense::fine).sum();
        int months = picked.stream().mapToInt(PenalCode.Offense::jailMonths).sum();
        if (arrestStep == 1) {
            small(g, "Arresto di " + who, cx + 86, cy + 5, cw - 190, TEXT);
            small(g, "Reati: " + picked.size() + "  |  Multa: " + money(fine) + "  |  Detenzione: " + months + " mesi",
                    cx, cy + ch - 12, cw - 106, picked.isEmpty() ? DIM : AMBER);
            return;
        }
        MdtWidgets.box(g, cx, cy + 20, cw, 98, LINE, PANEL);
        int x = cx + 8;
        int w = cw - 16;
        int y = cy + 26;
        small(g, "Cittadino: " + who, x, y, w, TEXT);
        y += 12;
        List<String> lines = wrap("Reati (" + picked.size() + "): "
                + String.join(", ", picked.stream().map(PenalCode.Offense::name).toList()), (int) (w / 0.8f));
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            small(g, i == 2 && lines.size() > 3 ? lines.get(i) + "..." : lines.get(i), x, y, w, TEXT);
            y += 10;
        }
        y += 2;
        small(g, "Multa totale: " + money(fine) + " (automatica)", x, y, w, AMBER);
        y += 12;
        small(g, "Detenzione totale: " + months + " mesi (automatica)", x, y, w, AMBER);
        y += 12;
        small(g, "Agente: " + host.officer() + "   -   " + DATE.format(Instant.now()), x, y, w, DIM);
        small(g, "NOTE", cx, cy + 120, cw, DIM);
    }
}
