package dev.qrm.police.client.mdt.dispatch;

import static dev.qrm.police.client.mdt.dispatch.CallFormat.*;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.BLUE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.GREEN;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.LINE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.PANEL;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.PANEL_HI;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT;

import com.google.gson.JsonObject;
import dev.qrm.police.client.ClientPolice;
import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.ui.MdtStatus;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Btn;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import dev.qrm.police.logic.CallType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Le unità collegate: stato, posizione, distanza e intervento in carico. */
final class UnitsView extends DispatchView {
    UnitsView(MdtHost host, DispatchPage owner) {
        super(host, owner);
    }

    private static final int UNIT_ROW_H = 38;
    private List<JsonObject> unitsLive;
    private long unitsPollAt;

    private List<JsonObject> unitList() {
        List<JsonObject> out = new ArrayList<>(unitsLive != null ? unitsLive : list("units"));
        out.sort((a, b) -> {
            int byState = Integer.compare(MdtStatus.order(str(a, "status")),
                    MdtStatus.order(str(b, "status")));
            return byState != 0 ? byState : str(a, "name").compareToIgnoreCase(str(b, "name"));
        });
        return out;
    }

    private JsonObject activeCallOfUnit(String unitId) {
        for (JsonObject c : list("calls")) {
            if (str(c, "assigneeId").equals(unitId) && !stateOf(c).terminal()) return c;
        }
        return null;
    }

    @Override
    public void build() {
        List<JsonObject> units = unitList();
        int listTop = cy + 34;
        int rows = Math.max(1, (cy + ch - 3 - listTop) / UNIT_ROW_H);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, units.size() - rows)));
        for (int i = 0; i < rows && scroll + i < units.size(); i++) {
            addUnitRow(units.get(scroll + i), cx + 3, listTop + i * UNIT_ROW_H, cw - 6, UNIT_ROW_H - 2);
        }
    }

    private void addUnitRow(JsonObject u, int x, int y, int w, int h) {
        String st = str(u, "status");
        int color = MdtStatus.color(st);
        String id = str(u, "id");
        boolean me = id.equals(myId());
        JsonObject call = activeCallOfUnit(id);
        String no = str(u, "no");
        String rank = str(u, "rank");
        String right = (no.isEmpty() ? "" : no) + (rank.isEmpty() ? "" : (no.isEmpty() ? "" : " - ") + rank);
        int rightW = (int) (font.width(right) * 0.8f) + 1;
        addRenderableWidget(new Btn(x, y, w, h, (g, bx, by, bw, bh, hot, active) -> {
            MdtWidgets.box(g, bx, by, bw, bh, hot && call != null ? BLUE : LINE, hot && call != null ? PANEL_HI : PANEL);
            g.fill(bx, by, bx + 3, by + bh, color);
            g.fill(bx + 7, by + 5, bx + 12, by + 10, color);
            g.text(font, MdtWidgets.fit(font, str(u, "name") + (me ? " (tu)" : ""), bw - 22 - rightW), bx + 16,
                    by + 3, TEXT, false);
            small(g, right, bx + bw - rightW - 4, by + 4, rightW, DIM);
            small(g, MdtStatus.shortLabel(st) + " - X " + u.get("x").getAsInt() + " Y " + u.get("y").getAsInt() + " Z "
                    + u.get("z").getAsInt() + " - " + shortDim(str(u, "dim")) + " - " + (me ? "tu" : distance(u)), bx + 7,
                    by + 14, bw - 12, color);
            small(g, call == null ? "Nessun intervento in carico" : "#" + str(call, "id") + " "
                    + CallType.parse(str(call, "type")).label() + " - " + stateOf(call).label(), bx + 7, by + 24,
                    bw - 130, call == null ? DIM : TEXT);
        }, () -> {
            if (call != null) owner.showCall(str(call, "id"));
        }));
        if (!me) {
            addRenderableWidget(smallButton(x + w - 56, y + h - 12, 52, 10, "Posizione", BLUE, () -> {
                ClientPolice.setWaypoint(new ClientPolice.Waypoint("unit:" + id, "Unità " + str(u, "name"),
                        u.get("x").getAsInt(), u.get("y").getAsInt(), u.get("z").getAsInt()));
                rebuildWidgets();
            }));
        }
        if (call != null) {
            addRenderableWidget(smallButton(x + w - 56 - 2 - 52, y + h - 12, 52, 10, "Intervento", GREEN,
                    () -> owner.showCall(str(call, "id"))));
        }
    }

    @Override
    public void onEnter() {
        unitsLive = null;
        unitsPollAt = 0;
        scroll = 0;
    }

    @Override
    public void tick() {
        long now = System.currentTimeMillis();
        if (now - unitsPollAt >= 2000) {
            unitsPollAt = now;
            send("units_poll");
        }
    }

    @Override
    public boolean onReply(String kind, List<JsonObject> rows) {
        if (!kind.equals("units")) return false;
        unitsLive = rows;
        return true;
    }

    @Override
    public void render(GuiGraphicsExtractor g) {
        MdtWidgets.inset(g, cx, cy + 31, cw, ch - 31);
        if (unitList().isEmpty()) g.text(font, "Nessuna unità collegata.", cx + 8, cy + 40, DIM, false);
    }

}
