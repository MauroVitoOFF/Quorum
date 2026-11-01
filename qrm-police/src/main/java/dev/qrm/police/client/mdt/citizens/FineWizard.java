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
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT;

import com.google.gson.JsonObject;
import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.MdtPage;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Btn;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Field;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import dev.qrm.police.logic.FineCatalog;
import dev.qrm.police.logic.FineStatus;
import java.time.Instant;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Procedura guidata della multa: infrazione, importo e riepilogo. */
final class FineWizard extends MdtPage {
    private final CitizensPage owner;

    FineWizard(MdtHost host, CitizensPage owner) {
        super(host);
        this.owner = owner;
    }

    /** Passo della nuova multa: 0 nessuna, 2 infrazione, 3 riepilogo. */
    private int fineStep;
    private String fineDraftCitizen;
    private int fineDraftCat;
    private String fineDraftCode;
    private String fineDraftReason = "";

    boolean active() {
        return fineStep > 0;
    }

    void reset() {
        fineStep = 0;
    }

    void start(String citizenId) {
        fineDraftCitizen = citizenId;
        fineDraftCat = 0;
        fineDraftCode = null;
        fineDraftReason = "";
        fineStep = 2;
        owner.closeRecord();
        scroll = 0;
        host.setForm(null);
        rebuildWidgets();
    }

    void end() {
        fineStep = 0;
        fineDraftCitizen = null;
        fineDraftCode = null;
        scroll = 0;
        rebuildWidgets();
    }

    @Override
    public void build() {
        if (fineStep == 2) {
            backButton("< Annulla", this::end);
            var cats = FineCatalog.CATEGORIES;
            int gap = 2;
            int bw = (cw - gap * (cats.size() - 1)) / cats.size();
            for (int i = 0; i < cats.size(); i++) {
                final int idx = i;
                addRenderableWidget(smallButton(cx + i * (bw + gap), cy + 20, bw, 14, cats.get(i).shortName(),
                        i == fineDraftCat ? BLUE : DIM, () -> {
                            fineDraftCat = idx;
                            scroll = 0;
                            rebuildWidgets();
                        }));
            }
            var infractions = cats.get(fineDraftCat).infractions();
            int top = cy + 38;
            int rowH = 22;
            int rows = (cy + ch - top) / rowH;
            scroll = Math.max(0, Math.min(scroll, Math.max(0, infractions.size() - rows)));
            for (int i = 0; i < rows && scroll + i < infractions.size(); i++) {
                var inf = infractions.get(scroll + i);
                addRenderableWidget(new Btn(cx + 2, top + i * rowH, cw - 4, rowH - 2,
                        (g, x, y, w, h, hot, active) -> {
                            MdtWidgets.box(g, x, y, w, h, hot ? BLUE : LINE, hot ? PANEL_HI : PANEL);
                            g.text(font, MdtWidgets.fit(font, inf.name(), w - 70), x + 6, y + 6, TEXT,
                                    false);
                            String amount = money(inf.amount());
                            g.text(font, amount, x + w - 6 - font.width(amount), y + 6, AMBER, false);
                        }, () -> {
                            fineDraftCode = inf.code();
                            fineStep = 3;
                            rebuildWidgets();
                        }));
            }
            return;
        }
        backButton("< Indietro", () -> {
            fineStep = 2;
            scroll = 0;
            rebuildWidgets();
        });
        Field f = new Field(font, cx, cy + 122, cw, 16, Component.literal("Motivazione (facoltativa)"));
        f.setMaxLength(200);
        f.setValue(fineDraftReason);
        f.setResponder(v -> fineDraftReason = v);
        addRenderableWidget(f);
        addRenderableWidget(new Btn(cx + cw - 120, cy + ch - 18, 120, 16, Component.literal("Emetti multa"), GREEN,
                () -> {
                    send("fine_issue", fineDraftCitizen, fineDraftCode, fineDraftReason);
                    end();
                }));
    }

    @Override
    public void render(GuiGraphicsExtractor g) {
        small(g, fineStep == 2 ? "PASSO 1/2 Infrazione" : "PASSO 2/2 Riepilogo", cx + cw - 92, cy + 5, 92, DIM);
        JsonObject c = citizenById(fineDraftCitizen);
        String who = c == null ? "?" : str(c, "first") + " " + str(c, "last");
        if (fineStep == 2) {
            small(g, "Multa a " + who, cx + 86, cy + 5, cw - 190, TEXT);
            return;
        }
        var inf = FineCatalog.find(fineDraftCode == null ? "" : fineDraftCode).orElse(null);
        if (inf == null) return;
        MdtWidgets.box(g, cx, cy + 20, cw, 88, LINE, PANEL);
        int x = cx + 8;
        int w = cw - 16;
        int y = cy + 26;
        String[][] rows = {
            {"Cittadino", who},
            {"Infrazione", inf.name() + " (" + FineCatalog.categoryOf(inf.code()) + ")"},
            {"Importo (automatico)", money(inf.amount())},
            {"Agente emittente", host.officer()},
            {"Data e ora", DATE.format(Instant.now())},
            {"Stato del pagamento", FineStatus.UNPAID.label()}};
        for (String[] row : rows) {
            small(g, row[0] + ": " + row[1], x, y, w, row[0].startsWith("Importo") ? AMBER : TEXT);
            y += 13;
        }
        small(g, "MOTIVAZIONE", cx, cy + 111, cw, DIM);
    }
}
