package dev.qrm.police.client.mdt.dispatch;

import static dev.qrm.police.client.mdt.dispatch.CallFormat.*;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.BLUE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;

import com.google.gson.JsonObject;
import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Field;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import dev.qrm.police.logic.CallType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Lo storico degli interventi: ricerca e filtri, risultati calcolati dal server. */
final class HistoryView extends DispatchView {
    HistoryView(MdtHost host, DispatchPage owner) {
        super(host, owner);
    }

    private static final List<String> HIST_CATS = java.util.Arrays.stream(CallType.values()).map(CallType::category)
            .distinct().toList();

    private static final String[] HIST_STATES = {"", "CLOSED", "CANCELED"};
    private static final String[] HIST_STATE_LABELS = {"Tutti gli esiti", "Concluse", "Annullate"};
    private static final String[] HIST_DATES = {"", "TODAY", "YESTERDAY", "WEEK"};
    private static final String[] HIST_DATE_LABELS = {"Sempre", "Oggi", "Ieri", "7 giorni"};

    private List<JsonObject> histRows = new ArrayList<>();

    private boolean histLoaded;
    private String histQuery = "";
    private int histCat, histPrio, histState, histDate;
    private boolean histDirty;
    private long histEditedAt, histSentAt;
    private Field histField;

    @Override
    public void build() {
        boolean refocusField = histField != null && histField.isFocused();
        Field f = draftField(cx, cy + 32, cw, "Cerca ID, agente, matricola, segnalante o testo", 40, histQuery, v -> {
            if (v.equals(histQuery)) return;
            histQuery = v;
            histDirty = true;
            histEditedAt = System.currentTimeMillis();
        });
        histField = f;
        if (refocusField) {
            setFocused(f);
            f.setFocused(true);
        }
        int bw = (cw - 6) / 4;
        String[] labels = {"Cat: " + (histCat == 0 ? "tutte" : HIST_CATS.get(histCat - 1)),
                "Priorità: " + (histPrio == 0 ? "tutte" : "P" + histPrio), HIST_STATE_LABELS[histState],
                HIST_DATE_LABELS[histDate]};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            addRenderableWidget(smallButton(cx + i * (bw + 2), cy + 51, bw, 12, labels[i], BLUE, () -> {
                switch (idx) {
                    case 0 -> histCat = (histCat + 1) % (HIST_CATS.size() + 1);
                    case 1 -> histPrio = (histPrio + 1) % 5;
                    case 2 -> histState = (histState + 1) % HIST_STATES.length;
                    default -> histDate = (histDate + 1) % HIST_DATES.length;
                }
                scroll = 0;
                histDirty = true;
                histEditedAt = 0;
                rebuildWidgets();
            }));
        }
        int listTop = cy + 76;
        int rows = Math.max(1, (cy + ch - 3 - listTop) / CALL_ROW_H);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, histRows.size() - rows)));
        for (int i = 0; i < rows && scroll + i < histRows.size(); i++) {
            CallRow.add(this, histRows.get(scroll + i), cx + 3, listTop + i * CALL_ROW_H, cw - 6, CALL_ROW_H - 2);
        }
    }

    /** Le righe dell'ultima risposta: la pagina di una chiamata conclusa si apre da qui. */
    List<JsonObject> rows() {
        return histRows;
    }

    @Override
    public void onEnter() {
        histLoaded = false;
        histDirty = true;
        histEditedAt = 0;
        scroll = 0;
    }

    @Override
    public void tick() {
        long now = System.currentTimeMillis();
        // Il testo cercato si invia a scrittura finita; i filtri subito. Il server accetta una richiesta ogni mezzo secondo.
        if (histDirty && now - histEditedAt >= 350 && now - histSentAt >= 600) {
            histDirty = false;
            histSentAt = now;
            send("history", histQuery, histCat == 0 ? "" : HIST_CATS.get(histCat - 1), String.valueOf(histPrio),
                    HIST_STATES[histState], HIST_DATES[histDate]);
        }
    }

    @Override
    public boolean onReply(String kind, List<JsonObject> rows) {
        if (!kind.equals("history")) return false;
        histRows = rows;
        histLoaded = true;
        return true;
    }

    @Override
    public void render(GuiGraphicsExtractor g) {
        small(g, canOperate() ? "Tutti gli interventi conclusi o annullati, dal più recente."
                : "Gli interventi conclusi o annullati a cui hai partecipato.", cx, cy + 65, cw, DIM);
        MdtWidgets.inset(g, cx, cy + 74, cw, ch - 74);
        if (!histLoaded) g.text(font, "Caricamento...", cx + 8, cy + 82, DIM, false);
        else if (histRows.isEmpty()) g.text(font, "Nessun intervento trovato.", cx + 8, cy + 82, DIM, false);
    }

}
