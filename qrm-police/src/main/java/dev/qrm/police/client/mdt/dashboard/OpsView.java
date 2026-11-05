package dev.qrm.police.client.mdt.dashboard;

import static dev.qrm.police.client.mdt.dispatch.CallFormat.distance;
import static dev.qrm.police.client.mdt.dispatch.CallFormat.prioOf;
import static dev.qrm.police.client.mdt.dispatch.CallFormat.stateColor;
import static dev.qrm.police.client.mdt.dispatch.CallFormat.stateOf;
import static dev.qrm.police.client.mdt.ui.MdtStatus.PRIORITY_COLORS;
import static dev.qrm.police.client.mdt.ui.MdtText.DATE;
import static dev.qrm.police.client.mdt.ui.MdtText.HOUR;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.BLUE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.GREEN;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.LINE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.ORANGE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.PANEL;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT_SOFT;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.qrm.police.client.ClientPolice;
import dev.qrm.police.client.mdt.MdtForm;
import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.MdtPage;
import dev.qrm.police.client.mdt.ui.MdtStatus;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Btn;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import dev.qrm.police.logic.CallType;
import dev.qrm.police.logic.StatoChiamata;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * La Dashboard operativa: quando l'agente ha una chiamata in carico, in alto ne mostra le informazioni, in basso a
 * sinistra una griglia di azioni rapide e a destra la cronologia e le unità più vicine. Sta sotto l'intestazione
 * (stato e panico) della {@link DashboardPage}.
 */
final class OpsView extends MdtPage {
    OpsView(MdtHost host) {
        super(host);
    }

    private static final int TOP = 36;
    private static final int INFO_H = 50;
    private static final StatoChiamata[] STEPS = {StatoChiamata.EN_ROUTE, StatoChiamata.ON_SCENE, StatoChiamata.INTERVENING};
    private static final String[] STEP_NAMES = {"In viaggio", "Sul posto", "In intervento"};

    /** La chiamata aperta che l'agente ha in carico, o null. */
    JsonObject mine() {
        String me = myId();
        if (me.isEmpty()) return null;
        for (JsonObject c : list("calls")) {
            if (str(c, "assigneeId").equals(me) && !stateOf(c).terminal()) return c;
        }
        return null;
    }

    private int gridW() {
        return cw * 45 / 100;
    }

    @Override
    public void build() {
        JsonObject call = mine();
        if (call == null) return;
        String id = str(call, "id");
        StatoChiamata state = stateOf(call);
        int current = -1;
        for (int i = 0; i < STEPS.length; i++) if (STEPS[i] == state) current = i;
        boolean last = current == STEPS.length - 1;
        String advance = last ? STEP_NAMES[current] : "> " + STEP_NAMES[current + 1];
        final int next = current + 1;

        int gx = cx;
        int gy = cy + TOP + INFO_H + 6 + 11;
        int bw = (gridW() - 3) / 2;
        int bh = 20;
        int pitch = bh + 4;
        Btn adv = addRenderableWidget(smallButton(gx, gy, bw, bh, advance, BLUE, () -> {
            if (!last) send("call_state", id, STEPS[next].name());
        }));
        adv.active = !last;
        addRenderableWidget(smallButton(gx + bw + 3, gy, bw, bh, "Termina", GREEN, () -> {
            host.setForm(new MdtForm("Termina intervento #" + id, new String[] {"Esito dell'intervento"},
                    new String[] {""}, v -> send("call_close", id, v[0])));
            rebuildWidgets();
        }));
        Btn backup = addRenderableWidget(smallButton(gx, gy + pitch, bw, bh, "Rinforzi", ORANGE,
                () -> send("call_backup", id)));
        backup.active = state != StatoChiamata.BACKUP_REQUESTED;
        addRenderableWidget(smallButton(gx + bw + 3, gy + pitch, bw, bh, "Waypoint", DIM, () -> {
            ClientPolice.setWaypoint(new ClientPolice.Waypoint(id, str(call, "desc"), call.get("x").getAsInt(),
                    call.get("y").getAsInt(), call.get("z").getAsInt()));
            rebuildWidgets();
        }));
    }

    @Override
    public void render(GuiGraphicsExtractor g) {
        JsonObject call = mine();
        if (call == null) return;
        StatoChiamata state = stateOf(call);
        int prio = prioOf(call);
        CallType type = CallType.parse(str(call, "type"));
        int t = cy + TOP;

        // Informazioni della chiamata.
        MdtWidgets.box(g, cx, t, cw, INFO_H, LINE, PANEL);
        g.fill(cx, t + 2, cx + 2, t + INFO_H - 2, MdtWidgets.alpha(PRIORITY_COLORS[prio], 0xA0));
        int tx = cx + 8;
        String stateText = state.label();
        int sw = (int) (font.width(stateText) * 0.8f) + 1;
        g.text(font, MdtWidgets.fit(font, type.label(), cw - sw - 24), tx, t + 4, TEXT, false);
        small(g, stateText, cx + cw - sw - 6, t + 6, sw, MdtWidgets.alpha(stateColor(state), 0xC0));
        small(g, "#" + str(call, "id") + "  -  " + DATE.format(Instant.ofEpochMilli(call.get("time").getAsLong()))
                + "  -  " + distance(call), tx, t + 17, cw - 16, DIM);
        List<String> desc = wrap(str(call, "desc"), (int) ((cw - 40) / 0.8f));
        for (int i = 0; i < 2 && i < desc.size(); i++) {
            small(g, i == 1 && desc.size() > 2 ? desc.get(i) + "..." : desc.get(i), tx, t + 28 + i * 10, cw - 40,
                    TEXT_SOFT);
        }
        String level = "P" + prio;
        int lw = (int) (font.width(level) * 0.8f) + 1;
        small(g, level, cx + cw - lw - 6, t + INFO_H - 12, lw, MdtWidgets.alpha(PRIORITY_COLORS[prio], 0xE0));

        // Griglia di azioni (i pulsanti sono widget).
        int by = t + INFO_H + 6;
        small(g, "OPERAZIONI", cx, by, gridW(), DIM);

        // A destra: cronologia della chiamata e unità vicine.
        int rx = cx + gridW() + 10;
        int rw = cw - gridW() - 10;
        small(g, "CRONOLOGIA", rx, by, rw, DIM);
        JsonArray events = arr(call, "events");
        for (int i = 0; i < 4 && i < events.size(); i++) {
            JsonObject ev = events.get(events.size() - 1 - i).getAsJsonObject();
            small(g, "[" + HOUR.format(Instant.ofEpochMilli(ev.get("time").getAsLong())) + "] " + str(ev, "text"), rx,
                    by + 11 + i * 9, rw, i == 0 ? TEXT_SOFT : DIM);
        }
        if (events.size() == 0) small(g, "Nessun evento.", rx, by + 11, rw, DIM);

        int uy = by + 11 + 4 * 9 + 6;
        small(g, "UNITÀ VICINE", rx, uy, rw, DIM);
        List<JsonObject> near = nearUnits(call);
        for (int i = 0; i < near.size() && i < 4; i++) {
            JsonObject u = near.get(i);
            int y = uy + 11 + i * 10;
            g.fill(rx, y + 1, rx + 4, y + 5, MdtStatus.color(str(u, "status")));
            String d = metersTo(call, u) + " m";
            int dw = (int) (font.width(d) * 0.8f) + 1;
            small(g, str(u, "name"), rx + 8, y, rw - dw - 12, TEXT_SOFT);
            small(g, d, rx + rw - dw, y, dw, DIM);
        }
        if (near.isEmpty()) small(g, "Nessuna unità vicina.", rx, uy + 11, rw, DIM);
    }

    private static int metersTo(JsonObject call, JsonObject u) {
        double dx = u.get("x").getAsInt() - call.get("x").getAsInt();
        double dz = u.get("z").getAsInt() - call.get("z").getAsInt();
        return (int) Math.sqrt(dx * dx + dz * dz);
    }

    /** Agenti in servizio nello stesso mondo della chiamata, escluso chi la ha in carico, dal più vicino. */
    private List<JsonObject> nearUnits(JsonObject call) {
        List<JsonObject> out = new ArrayList<>();
        String me = myId();
        for (JsonObject u : list("units")) {
            if (str(u, "status").equals("OFF") || str(u, "id").equals(me)) continue;
            if (!str(u, "dim").equals(str(call, "dim"))) continue;
            out.add(u);
        }
        out.sort((a, b) -> Integer.compare(metersTo(call, a), metersTo(call, b)));
        return out;
    }
}
