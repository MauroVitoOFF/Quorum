package dev.qrm.police.client.mdt.dispatch;

import static dev.qrm.police.client.mdt.dispatch.CallFormat.*;
import static dev.qrm.police.client.mdt.ui.MdtStatus.PRIORITY_COLORS;
import static dev.qrm.police.client.mdt.ui.MdtText.DATE;
import static dev.qrm.police.client.mdt.ui.MdtText.HOUR;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.BLUE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.GREEN;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.LINE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.ORANGE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.PANEL;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.PANEL_HI;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.RED;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.qrm.police.client.ClientPolice;
import dev.qrm.police.client.mdt.MdtForm;
import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.ui.MdtStatus;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Btn;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import dev.qrm.police.logic.CallType;
import dev.qrm.police.logic.StatoChiamata;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** La pagina di una chiamata: dettagli, azioni possibili e cronologia. */
final class CallDetailView extends DispatchView {
    CallDetailView(MdtHost host, DispatchPage owner) {
        super(host, owner);
    }

    /** Una voce d'azione nella pagina di una chiamata. */
    private record CallAction(String label, int color, Runnable run) {}

    private boolean assignPicker;

    private boolean isMine(JsonObject call) {
        return !str(call, "assigneeId").isEmpty() && str(call, "assigneeId").equals(myId());
    }

    private boolean canManage(JsonObject call) {
        return isMine(call) || canOperate();
    }

    private List<CallAction> actionsFor(JsonObject call) {
        List<CallAction> out = new ArrayList<>();
        StatoChiamata state = stateOf(call);
        if (state.terminal()) return out;
        String id = str(call, "id");
        boolean assigned = !str(call, "assignee").isEmpty();
        boolean manage = canManage(call);
        if (state == StatoChiamata.PENDING && !myStatus().equals("OFF")) {
            out.add(new CallAction("Prendi in carico", BLUE, () -> {
                send("call_take", id);
                setWaypointFor(call);
            }));
        }
        if (canOperate()) {
            out.add(new CallAction("Assegna unità", BLUE, () -> {
                assignPicker = true;
                rebuildWidgets();
            }));
        }
        if (assigned && manage) {
            out.add(new CallAction("Rilascia", DIM, () -> send("call_release", id)));
            StatoChiamata[] steps = {StatoChiamata.EN_ROUTE, StatoChiamata.ON_SCENE, StatoChiamata.INTERVENING};
            String[] names = {"Segna in viaggio", "Segna sul posto", "Segna in intervento"};
            for (int i = 0; i < steps.length; i++) {
                final StatoChiamata target = steps[i];
                if (state != target) out.add(new CallAction(names[i], GREEN, () -> send("call_state", id, target.name())));
            }
            if (state != StatoChiamata.BACKUP_REQUESTED) {
                out.add(new CallAction("Richiedi rinforzi", ORANGE, () -> send("call_backup", id)));
            }
        }
        if (canOperate()) {
            out.add(new CallAction("Priorità P" + prioOf(call), PRIORITY_COLORS[prioOf(call)],
                    () -> send("call_priority", id, String.valueOf(prioOf(call) % 4 + 1))));
        }
        if (manage && (assigned || canOperate())) {
            out.add(new CallAction("Chiudi intervento", GREEN, () -> {
                host.setForm(new MdtForm("Chiudi intervento " + "#" + id, new String[] {"Esito dell'intervento"},
                        new String[] {""}, v -> send("call_close", id, v[0])));
                rebuildWidgets();
            }));
        }
        if (canOperate()) {
            out.add(new CallAction("Annulla chiamata", RED, () -> {
                host.setForm(new MdtForm("Annulla chiamata " + "#" + id, new String[] {"Motivo"}, new String[] {""},
                        v -> send("call_cancel", id, v[0])));
                rebuildWidgets();
            }));
        }
        return out;
    }

    private void setWaypointFor(JsonObject call) {
        ClientPolice.setWaypoint(new ClientPolice.Waypoint(str(call, "id"), str(call, "desc"),
                call.get("x").getAsInt(), call.get("y").getAsInt(), call.get("z").getAsInt()));
    }

    private void buildCallPage(JsonObject call) {
        if (assignPicker) {
            addRenderableWidget(new Btn(cx, cy, 64, 14, Component.literal("< Indietro"), DIM, () -> {
                assignPicker = false;
                rebuildWidgets();
            }));
            int y = cy + 24;
            for (JsonElement e : arr(data, "units")) {
                JsonObject u = e.getAsJsonObject();
                String st = str(u, "status");
                if (st.equals("OFF") || y + 18 > cy + ch) continue;
                String no = str(u, "no");
                addRenderableWidget(new Btn(cx, y, cw, 16, (g, bx, by, bw, bh, hot, active) -> {
                    MdtWidgets.box(g, bx, by, bw, bh, hot ? BLUE : LINE, hot ? PANEL_HI : PANEL);
                    g.fill(bx + 5, by + 5, bx + 10, by + 10, MdtStatus.color(st));
                    g.text(font, MdtWidgets.fit(font, str(u, "name") + (no.isEmpty() ? "" : "  " + no), bw - 90),
                            bx + 16, by + 4, TEXT, false);
                    small(g, MdtStatus.shortLabel(st),
                            bx + bw - 70, by + 5, 66, DIM);
                }, () -> {
                    send("call_assign", str(call, "id"), str(u, "id"));
                    assignPicker = false;
                }));
                y += 18;
            }
            return;
        }
        addRenderableWidget(new Btn(cx, cy, 56, 14, Component.literal("< Lista"), DIM, () -> {
            owner.backToList();
        }));
        addRenderableWidget(smallButton(cx + cw - 76, cy + 51, 76, 10, "Imposta waypoint", BLUE, () -> {
            setWaypointFor(call);
            rebuildWidgets();
        }));
        List<CallAction> actions = actionsFor(call);
        int bw = (cw - 4) / 3;
        for (int i = 0; i < actions.size(); i++) {
            CallAction a = actions.get(i);
            addRenderableWidget(smallButton(cx + (i % 3) * (bw + 2), cy + 94 + (i / 3) * 14, bw, 12, a.label(), a.color(),
                    a.run()));
        }
    }

    private int callActionsEnd(JsonObject call) {
        int n = actionsFor(call).size();
        return cy + 94 + ((n + 2) / 3) * 14;
    }

    private void renderCallPage(GuiGraphicsExtractor g, JsonObject call) {
        if (assignPicker) {
            g.text(font, "ASSEGNA " + "#" + str(call, "id"), cx + 70, cy + 3, BLUE, false);
            boolean any = false;
            for (JsonElement e : arr(data, "units")) if (!str(e.getAsJsonObject(), "status").equals("OFF")) any = true;
            if (!any) small(g, "Nessun agente disponibile: serve un agente collegato e non fuori servizio.", cx, cy + 28,
                    cw, DIM);
            return;
        }
        StatoChiamata state = stateOf(call);
        int prio = prioOf(call);
        CallType type = CallType.parse(str(call, "type"));
        String title = "#" + str(call, "id") + "  P" + prio + "  " + type.label();
        int badgeW = (int) (font.width(state.label()) * 0.8f) + 10;
        g.text(font, MdtWidgets.fit(font, title, cw - 64 - badgeW - 6), cx + 62, cy + 3, PRIORITY_COLORS[prio],
                false);
        MdtWidgets.box(g, cx + cw - badgeW, cy + 1, badgeW, 12, stateColor(state), MdtWidgets.alpha(stateColor(state), 0x30));
        small(g, state.label(), cx + cw - badgeW + 5, cy + 4, badgeW - 6, stateColor(state));

        int x = cx + 2;
        int w = cw - 4;
        String parent = str(call, "parent");
        small(g, "Ricevuta " + DATE.format(Instant.ofEpochMilli(call.get("time").getAsLong())) + "  -  " + type.category()
                + (parent.isEmpty() ? "" : "  -  da #" + parent), x, cy + 20, w, DIM);
        String callerId = str(call, "callerId");
        small(g, "Segnalante: " + str(call, "caller") + (callerId.isEmpty() ? "" : "  (" + callerId.substring(0,
                Math.min(8, callerId.length())) + ")"), x, cy + 30, w, TEXT);
        small(g, "Posizione: X " + call.get("x").getAsInt() + "  Y " + call.get("y").getAsInt() + "  Z "
                + call.get("z").getAsInt() + "  -  " + str(call, "dim"), x, cy + 40, w, TEXT);
        small(g, "Distanza: " + distance(call) + direction(call), x, cy + 52, w - 80, TEXT);
        if (!str(call, "assignee").isEmpty()) {
            String no = str(call, "assigneeNo");
            String unitState = "";
            for (JsonElement e : arr(data, "units")) {
                JsonObject u = e.getAsJsonObject();
                if (str(u, "id").equals(str(call, "assigneeId"))) {
                    unitState = "  -  " + MdtStatus.shortLabel(str(u, "status"));
                }
            }
            long at = call.get("assignedAt").getAsLong();
            small(g, "Assegnata a " + str(call, "assignee") + (no.isEmpty() ? "" : " (" + no + ")") + unitState
                    + (at > 0 ? "  -  dalle " + HOUR.format(Instant.ofEpochMilli(at)) : ""), x, cy + 62, w, GREEN);
        } else {
            small(g, "Nessuna unità assegnata", x, cy + 62, w, ORANGE);
        }
        List<String> desc = wrap(str(call, "desc"), (int) (w / 0.8f));
        for (int i = 0; i < 2 && i < desc.size(); i++) {
            small(g, i == 1 && desc.size() > 2 ? desc.get(i) + "..." : desc.get(i), x, cy + 74 + i * 9, w, DIM);
        }

        int end = callActionsEnd(call);
        g.fill(cx, end + 1, cx + cw, end + 2, LINE);
        JsonArray events = arr(call, "events");
        small(g, "CRONOLOGIA (" + events.size() + ")", x, end + 4, w, DIM);
        List<String> lines = new ArrayList<>();
        for (int i = events.size() - 1; i >= 0; i--) {
            JsonObject ev = events.get(i).getAsJsonObject();
            String no = str(ev, "agentNo");
            lines.add("[" + HOUR.format(Instant.ofEpochMilli(ev.get("time").getAsLong())) + "] " + str(ev, "text")
                    + (no.isEmpty() ? "" : " (" + no + ")"));
        }
        int top = end + 14;
        int rows = Math.max(1, (cy + ch - 2 - top) / 9);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - rows)));
        for (int i = 0; i < rows && scroll + i < lines.size(); i++) {
            small(g, lines.get(scroll + i), x, top + i * 9, w, scroll + i == 0 ? TEXT : DIM);
        }
    }

    @Override
    public void onEnter() {
        assignPicker = false;
        scroll = 0;
    }

    @Override
    public void build() {
        buildCallPage(owner.openCall());
    }

    @Override
    public void render(GuiGraphicsExtractor g) {
        renderCallPage(g, owner.openCall());
    }

}
