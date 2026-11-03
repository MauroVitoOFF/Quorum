package dev.qrm.police.client.mdt.dispatch;

import static dev.qrm.police.client.mdt.dispatch.CallFormat.*;
import static dev.qrm.police.client.mdt.ui.MdtStatus.PRIORITY_COLORS;
import static dev.qrm.police.client.mdt.ui.MdtText.HOUR;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.BLUE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.LINE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.ORANGE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.PANEL;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.PANEL_HI;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.RED;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT_SOFT;

import com.google.gson.JsonObject;
import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Btn;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import dev.qrm.police.logic.CallType;
import dev.qrm.police.logic.StatoChiamata;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Le chiamate aperte, le più vicine all'agente per prime (a parità la più grave, poi la più vecchia). Un clic apre la
 * pagina della chiamata; in fondo ci sono "+ Chiamata" (per chi gestisce il Dispatch) e "Supporto".
 */
final class CallListView extends DispatchView {
    CallListView(MdtHost host, DispatchPage owner) {
        super(host, owner);
    }

    private static final int ROW_H = 25;
    private static final int TOP = 12;
    private static final int BAR_H = 14;
    /** Una chiamata è "nuova" per il primo minuto dopo l'arrivo. */
    private static final long NEW_MS = 60_000;

    private List<JsonObject> visible() {
        List<JsonObject> out = new ArrayList<>(list("calls"));
        out.sort((a, b) -> {
            int byDistance = Integer.compare(meters(a), meters(b));
            if (byDistance != 0) return byDistance;
            int byPriority = Integer.compare(prioOf(a), prioOf(b));
            return byPriority != 0 ? byPriority : Long.compare(a.get("time").getAsLong(), b.get("time").getAsLong());
        });
        // La chiamata che l'agente ha in carico resta sempre la prima, fuori dall'ordine per distanza.
        for (int i = 0; i < out.size(); i++) {
            if (isMine(out.get(i))) {
                out.add(0, out.remove(i));
                break;
            }
        }
        return out;
    }

    private boolean isMine(JsonObject call) {
        String me = myId();
        return !me.isEmpty() && str(call, "assigneeId").equals(me);
    }

    private int rowsFit() {
        return Math.max(1, (ch - TOP - BAR_H - 4) / ROW_H);
    }

    @Override
    public void build() {
        int barY = cy + ch - BAR_H + 1;
        int x = cx;
        if (canOperate()) {
            addRenderableWidget(smallButton(x, barY, 62, 12, "+ Chiamata", BLUE, owner::startCreating));
            x += 66;
        }
        addRenderableWidget(smallButton(x, barY, 48, 12, "Supporto", RED, () -> send("support")));

        List<JsonObject> calls = visible();
        int rows = rowsFit();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, calls.size() - rows)));
        for (int i = 0; i < rows && scroll + i < calls.size(); i++) {
            addRow(calls.get(scroll + i), cy + TOP + i * ROW_H);
        }
    }

    private void addRow(JsonObject call, int y) {
        int prio = prioOf(call);
        int color = PRIORITY_COLORS[prio];
        StatoChiamata state = stateOf(call);
        CallType type = CallType.parse(str(call, "type"));
        String id = str(call, "id");
        long time = call.get("time").getAsLong();
        String meta = distanceShort(call) + "  -  #" + id + "  " + HOUR.format(Instant.ofEpochMilli(time));
        int metaW = (int) (font.width(meta) * 0.8f) + 1;
        String assignee = str(call, "assignee");
        boolean mine = isMine(call);
        String right = mine ? "In carico a te - " + state.label()
                : assignee.isEmpty() ? state.label() : state.label() + " - " + assignee;
        boolean isNew = System.currentTimeMillis() - time < NEW_MS;
        boolean urgent = prio == 1 && assignee.isEmpty();
        String level = "P" + prio;
        int levelW = (int) (font.width(level) * 0.8f) + 1;
        addRenderableWidget(new Btn(cx, y, cw, ROW_H - 2, (g, bx, by, bw, bh, hot, active) -> {
            // Riga piatta: nessun riquadro, solo un filo in basso e lo sfondo sotto il mouse.
            if (hot) g.fill(bx, by, bx + bw, by + bh, PANEL_HI);
            g.fill(bx, by + bh, bx + bw, by + bh + 1, MdtWidgets.INSET_LINE);
            if (mine) g.fill(bx, by, bx + bw, by + bh, MdtWidgets.alpha(BLUE, 0x14));
            g.fill(bx, by + 2, bx + 2, by + bh - 2, MdtWidgets.alpha(color, 0xA0));
            int tx = bx + 7;
            String label = MdtWidgets.fit(font, type.label(), bw - 7 - metaW - (isNew ? 12 : 0) - 14);
            g.text(font, label, tx, by + 3, TEXT, false);
            if (isNew) {
                // Icona "nuova": un quadratino ambra con un punto esclamativo.
                int ix = tx + font.width(label) + 4;
                g.fill(ix, by + 3, ix + 7, by + 10, MdtWidgets.alpha(ORANGE, 0xB0));
                g.fill(ix + 3, by + 4, ix + 4, by + 7, 0xFF1A2230);
                g.fill(ix + 3, by + 8, ix + 4, by + 9, 0xFF1A2230);
            }
            small(g, meta, bx + bw - metaW - 5, by + 4, metaW, DIM);
            // Seconda riga: descrizione, poi stato e livello di allarme in piccolo a destra.
            boolean dim = urgent && (System.currentTimeMillis() / 600) % 2 == 0;
            small(g, level, bx + bw - levelW - 5, by + 14, levelW, MdtWidgets.alpha(color, dim ? 0x70 : 0xE0));
            int rw = Math.min((int) (font.width(right) * 0.8f) + 1, bw / 3);
            small(g, right, bx + bw - levelW - rw - 12, by + 14, rw, MdtWidgets.alpha(stateColor(state), 0xC0));
            small(g, str(call, "desc"), tx, by + 14, bw - levelW - rw - 28, TEXT_SOFT);
        }, () -> owner.showCall(id)));
    }

    @Override
    public void render(GuiGraphicsExtractor g) {
        int n = list("calls").size();
        small(g, n == 0 ? "Nessuna chiamata aperta" : n + (n == 1 ? " chiamata aperta" : " chiamate aperte")
                + " - le più vicine per prime", cx + 1, cy + 1, cw, DIM);
    }

}
