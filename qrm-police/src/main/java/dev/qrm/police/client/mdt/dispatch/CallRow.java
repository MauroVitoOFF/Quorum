package dev.qrm.police.client.mdt.dispatch;

import static dev.qrm.police.client.mdt.dispatch.CallFormat.*;
import static dev.qrm.police.client.mdt.ui.MdtStatus.PRIORITY_COLORS;
import static dev.qrm.police.client.mdt.ui.MdtText.HOUR;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.BLUE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.LINE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.PANEL;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.PANEL_HI;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.RED;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT;

import com.google.gson.JsonObject;
import dev.qrm.police.client.ClientPolice;
import dev.qrm.police.client.mdt.MdtPage;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Btn;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import dev.qrm.police.logic.CallType;
import dev.qrm.police.logic.StatoChiamata;
import java.time.Instant;
import java.util.Locale;

/** Una riga di chiamata, uguale in Dashboard e in Dispatch. Il clic apre la pagina della chiamata. */
public final class CallRow {
    private CallRow() {}

    /** Una riga di chiamata, uguale in Dashboard e in Dispatch. Il clic apre la pagina della chiamata. */
    public static void add(MdtPage page, JsonObject call, int x, int y, int w, int h) {
        int prio = prioOf(call);
        StatoChiamata state = stateOf(call);
        CallType type = CallType.parse(CallRow.str(call, "type"));
        int color = PRIORITY_COLORS[prio];
        boolean assigned = !CallRow.str(call, "assignee").isEmpty();
        String id = CallRow.str(call, "id");
        String meta = "#" + id + "  " + HOUR.format(Instant.ofEpochMilli(call.get("time").getAsLong()));
        int metaW = (int) (page.font().width(meta) * 0.8f) + 1;
        page.addRenderableWidget(new Btn(x, y, w, h, (g, bx, by, bw, bh, hot, active) -> {
            // Le P1 senza incaricato lampeggiano piano sul bordo, per non passare inosservate.
            boolean urgent = prio == 1;
            boolean pulse = urgent && !assigned && (System.currentTimeMillis() / 600) % 2 == 0;
            MdtWidgets.box(g, bx, by, bw, bh, urgent ? (pulse ? RED : MdtWidgets.alpha(RED, 0x90)) : LINE,
                    urgent ? MdtWidgets.alpha(RED, hot ? 0x28 : 0x16) : hot ? PANEL_HI : PANEL);
            g.fill(bx, by, bx + 3, by + bh, color);
            MdtWidgets.box(g, bx + 7, by + 4, 14, 9, color, MdtWidgets.alpha(color, 0x40));
            page.small(g, "P" + prio, bx + 10, by + 5, 10, TEXT);
            g.text(page.font(), MdtWidgets.fit(page.font(), type.label(), bw - 32 - metaW), bx + 25, by + 4, TEXT, false);
            page.small(g, meta, bx + bw - metaW - 4, by + 5, metaW, DIM);
            page.small(g, CallRow.str(call, "desc"), bx + 7, by + 17, bw - 12, TEXT);
            page.small(g, "X " + call.get("x").getAsInt() + " Y " + call.get("y").getAsInt() + " Z "
                    + call.get("z").getAsInt() + " - " + shortDim(CallRow.str(call, "dim")) + " - " + distance(call), bx + 7,
                    by + 28, bw - 12, DIM);
            if (assigned) {
                String no = CallRow.str(call, "assigneeNo");
                page.small(g, state.label() + " - " + CallRow.str(call, "assignee") + (no.isEmpty() ? "" : " (" + no + ")"),
                        bx + 7, by + 38, bw - 12, stateColor(state));
            } else {
                page.small(g, state.label().toUpperCase(Locale.ROOT), bx + bw - 54, by + 38, 50, stateColor(state));
            }
        }, () -> page.host().open(DispatchPage.class).showCall(id)));
        if (state == StatoChiamata.PENDING) {
            Btn take = page.addRenderableWidget(page.smallButton(x + 6, y + 35, 80, 10, "Prendi in carico", BLUE, () -> {
                page.send("call_take", id);
                ClientPolice.setWaypoint(new ClientPolice.Waypoint(id, CallRow.str(call, "desc"), call.get("x").getAsInt(),
                        call.get("y").getAsInt(), call.get("z").getAsInt()));
            }));
            take.active = !page.myStatus().equals("OFF");
        }
    }

    private static String str(JsonObject o, String key) {
        return o.get(key).getAsString();
    }

}
