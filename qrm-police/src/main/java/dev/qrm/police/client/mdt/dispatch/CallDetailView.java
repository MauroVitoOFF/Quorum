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
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT_SOFT;

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

    /** Una voce d'azione: le principali sono più grandi, le altre piccole e in grigio. */
    private record CallAction(String label, int color, boolean primary, Runnable run) {}

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
        // Principali: dare la chiamata a qualcuno, far avanzare lo stato, chiudere.
        if (state == StatoChiamata.PENDING && !myStatus().equals("OFF")) {
            out.add(new CallAction("Prendi in carico", BLUE, true, () -> {
                send("call_take", id);
                setWaypointFor(call);
            }));
        }
        if (canOperate()) {
            out.add(new CallAction(assigned ? "Riassegna" : "Assegna", BLUE, true, () -> {
                assignPicker = true;
                rebuildWidgets();
            }));
        }
        if (assigned && manage) {
            // Solo i passi che mancano: chi è già "sul posto" vede soltanto "In intervento".
            StatoChiamata[] steps = {StatoChiamata.EN_ROUTE, StatoChiamata.ON_SCENE, StatoChiamata.INTERVENING};
            String[] names = {"In viaggio", "Sul posto", "In intervento"};
            int current = -1;
            for (int i = 0; i < steps.length; i++) if (steps[i] == state) current = i;
            for (int i = current + 1; i < steps.length; i++) {
                final StatoChiamata target = steps[i];
                out.add(new CallAction(names[i], GREEN, true, () -> send("call_state", id, target.name())));
            }
        }
        if (manage && (assigned || canOperate())) {
            out.add(new CallAction("Termina", GREEN, true, () -> {
                host.setForm(new MdtForm("Termina intervento #" + id, new String[] {"Esito dell'intervento"},
                        new String[] {""}, v -> send("call_close", id, v[0])));
                rebuildWidgets();
            }));
        }
        // Secondarie.
        if (manage) {
            out.add(new CallAction("Nota", DIM, false, () -> {
                host.setForm(new MdtForm("Nota su #" + id, new String[] {"Nota operativa"}, new String[] {""},
                        v -> send("call_note", id, v[0])));
                rebuildWidgets();
            }));
        }
        if (assigned && manage && state != StatoChiamata.BACKUP_REQUESTED) {
            out.add(new CallAction("Rinforzi", DIM, false, () -> send("call_backup", id)));
        }
        if (assigned && manage) out.add(new CallAction("Rilascia", DIM, false, () -> send("call_release", id)));
        if (canOperate()) {
            out.add(new CallAction("Priorità P" + prioOf(call), DIM, false,
                    () -> send("call_priority", id, String.valueOf(prioOf(call) % 4 + 1))));
        }
        out.add(new CallAction("Waypoint", DIM, false, () -> {
            setWaypointFor(call);
            rebuildWidgets();
        }));
        if (canOperate()) {
            out.add(new CallAction("Annulla", RED, false, () -> {
                host.setForm(new MdtForm("Annulla chiamata #" + id, new String[] {"Motivo"}, new String[] {""},
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

    private boolean showLog;

    private void buildCallPage(JsonObject call) {
        if (assignPicker) {
            addRenderableWidget(smallButton(cx, cy, 52, 11, "< Indietro", DIM, () -> {
                assignPicker = false;
                rebuildWidgets();
            }));
            int y = cy + 20;
            for (JsonElement e : arr(data, "units")) {
                JsonObject u = e.getAsJsonObject();
                String st = str(u, "status");
                if (st.equals("OFF") || y + 18 > cy + ch) continue;
                String no = str(u, "no");
                addRenderableWidget(new Btn(cx, y, cw, 16, (g, bx, by, bw, bh, hot, active) -> {
                    MdtWidgets.box(g, bx, by, bw, bh, hot ? MdtWidgets.alpha(BLUE, 0x90) : LINE, hot ? PANEL_HI : PANEL);
                    g.fill(bx + 5, by + 5, bx + 10, by + 10, MdtStatus.color(st));
                    g.text(font, MdtWidgets.fit(font, str(u, "name") + (no.isEmpty() ? "" : "  " + no), bw - 90),
                            bx + 16, by + 4, TEXT, false);
                    small(g, MdtStatus.shortLabel(st), bx + bw - 70, by + 5, 66, DIM);
                }, () -> {
                    send("call_assign", str(call, "id"), str(u, "id"));
                    assignPicker = false;
                }));
                y += 18;
            }
            return;
        }
        addRenderableWidget(smallButton(cx, cy, 44, 11, "< Lista", DIM, owner::backToList));
        addRenderableWidget(smallButton(cx + cw - 62, cy, 62, 11, showLog ? "Dettagli" : "Cronologia", DIM, () -> {
            showLog = !showLog;
            scroll = 0;
            rebuildWidgets();
        }));
        List<CallAction> actions = actionsFor(call);
        List<CallAction> main = new ArrayList<>();
        List<CallAction> rest = new ArrayList<>();
        for (CallAction a : actions) (a.primary() ? main : rest).add(a);
        int y = barTop(actions);
        if (!main.isEmpty()) {
            int mw = (cw - (main.size() - 1) * 3) / main.size();
            for (int i = 0; i < main.size(); i++) addRenderableWidget(actionButton(cx + i * (mw + 3), y, mw, 12, main.get(i), true));
            y += 15;
        }
        int rw = (cw - 4 * 2) / 5;
        for (int i = 0; i < rest.size(); i++) {
            addRenderableWidget(actionButton(cx + (i % 5) * (rw + 2), y + (i / 5) * 12, rw, 10, rest.get(i), false));
        }
    }

    /** Pulsante piatto: bordo sottile e colore appena accennato; il testo è chiaro solo sulle azioni principali. */
    private Btn actionButton(int x, int y, int w, int h, CallAction a, boolean primary) {
        return new Btn(x, y, w, h, (g, bx, by, bw, bh, hot, active) -> {
            MdtWidgets.box(g, bx, by, bw, bh, MdtWidgets.alpha(a.color(), hot ? 0xB0 : primary ? 0x70 : 0x40),
                    MdtWidgets.alpha(a.color(), hot ? 0x30 : primary ? 0x12 : 0x00));
            int tw = Math.min((int) (font.width(a.label()) * 0.8f) + 1, bw - 4);
            small(g, a.label(), bx + (bw - tw) / 2, by + (bh - 7) / 2 + 1, bw - 4,
                    primary || hot ? TEXT : a.color() == DIM ? DIM : MdtWidgets.alpha(a.color(), 0xD0));
        }, a.run());
    }

    /** La barra delle azioni sta in fondo: una riga di principali e una o due di secondarie, più piccole. */
    private int barTop(List<CallAction> actions) {
        int main = 0;
        for (CallAction a : actions) if (a.primary()) main++;
        int rest = actions.size() - main;
        return cy + ch - (main > 0 ? 15 : 0) - ((rest + 4) / 5) * 12;
    }

    private void renderCallPage(GuiGraphicsExtractor g, JsonObject call) {
        if (assignPicker) {
            small(g, "Assegna #" + str(call, "id") + " a un'unità", cx + 58, cy + 2, cw - 60, DIM);
            boolean any = false;
            for (JsonElement e : arr(data, "units")) if (!str(e.getAsJsonObject(), "status").equals("OFF")) any = true;
            if (!any) small(g, "Nessun agente disponibile: serve un agente collegato e non fuori servizio.", cx, cy + 24,
                    cw, DIM);
            return;
        }
        StatoChiamata state = stateOf(call);
        int prio = prioOf(call);
        CallType type = CallType.parse(str(call, "type"));
        int x = cx + 2;
        int w = cw - 4;
        int bar = barTop(actionsFor(call));

        g.text(font, MdtWidgets.fit(font, type.label(), w - 90), x, cy + 17, TEXT, false);
        String stateText = state.label();
        int sw = (int) (font.width(stateText) * 0.8f) + 1;
        small(g, stateText, cx + cw - sw - 2, cy + 19, sw, MdtWidgets.alpha(stateColor(state), 0xC0));
        small(g, "#" + str(call, "id") + "  -  ricevuta " + DATE.format(Instant.ofEpochMilli(call.get("time").getAsLong()))
                + "  -  " + type.category(), x, cy + 30, w, DIM);
        String callerId = str(call, "callerId");
        small(g, "Segnalante: " + str(call, "caller") + (callerId.isEmpty() ? "" : "  (" + callerId.substring(0,
                Math.min(8, callerId.length())) + ")"), x, cy + 41, w, DIM);
        g.fill(cx, cy + 53, cx + cw, cy + 54, LINE);

        int bodyTop = cy + 59;
        int bodyEnd = bar - 20;
        if (showLog) {
            JsonArray events = arr(call, "events");
            List<String> lines = new ArrayList<>();
            for (int i = events.size() - 1; i >= 0; i--) {
                JsonObject ev = events.get(i).getAsJsonObject();
                String no = str(ev, "agentNo");
                lines.add("[" + HOUR.format(Instant.ofEpochMilli(ev.get("time").getAsLong())) + "] " + str(ev, "text")
                        + (no.isEmpty() ? "" : " (" + no + ")"));
            }
            int rows = Math.max(1, (bodyEnd - bodyTop + 14) / 11);
            scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - rows)));
            for (int i = 0; i < rows && scroll + i < lines.size(); i++) {
                small(g, lines.get(scroll + i), x, bodyTop + i * 11, w, scroll + i == 0 ? TEXT_SOFT : DIM);
            }
        } else {
            List<String> desc = wrap(str(call, "desc"), (int) (w / 0.8f));
            int rows = Math.max(1, (bodyEnd - bodyTop + 14) / 11);
            for (int i = 0; i < rows && i < desc.size(); i++) {
                small(g, i == rows - 1 && desc.size() > rows ? desc.get(i) + "..." : desc.get(i), x, bodyTop + i * 11, w,
                        TEXT_SOFT);
            }
        }

        // Riga di assegnazione e livello di allarme, sopra la barra delle azioni.
        String level = "P" + prio;
        int lw = (int) (font.width(level) * 0.8f) + 1;
        small(g, level, cx + cw - lw - 2, bar - 12, lw, MdtWidgets.alpha(PRIORITY_COLORS[prio], 0xE0));
        if (!str(call, "assignee").isEmpty()) {
            String no = str(call, "assigneeNo");
            String unitState = "";
            for (JsonElement e : arr(data, "units")) {
                JsonObject u = e.getAsJsonObject();
                if (str(u, "id").equals(str(call, "assigneeId"))) unitState = "  -  " + MdtStatus.shortLabel(str(u, "status"));
            }
            long at = call.get("assignedAt").getAsLong();
            small(g, "Assegnata a " + str(call, "assignee") + (no.isEmpty() ? "" : " (" + no + ")") + unitState
                    + (at > 0 ? "  -  dalle " + HOUR.format(Instant.ofEpochMilli(at)) : ""), x, bar - 12, w - lw - 8,
                    MdtWidgets.alpha(GREEN, 0xC0));
        } else {
            small(g, "Nessuna unità assegnata", x, bar - 12, w - lw - 8, MdtWidgets.alpha(ORANGE, 0xC0));
        }
    }

    @Override
    public void onEnter() {
        assignPicker = false;
        showLog = false;
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
