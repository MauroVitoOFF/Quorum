package dev.qrm.police.client.mdt.dispatch;

import static dev.qrm.police.client.mdt.ui.MdtStatus.PRIORITY_COLORS;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.BLUE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.GREEN;

import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Btn;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Field;
import dev.qrm.police.logic.CallType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** La creazione di una chiamata da parte degli operatori di centrale. */
final class NewCallView extends DispatchView {
    NewCallView(MdtHost host, DispatchPage owner) {
        super(host, owner);
    }

    private static final CallType[] CREATABLE = {CallType.MEDICAL, CallType.ROBBERY, CallType.SHOOTING,
            CallType.ASSAULT, CallType.KIDNAPPING, CallType.THEFT, CallType.BURGLARY, CallType.SUSPECT,
            CallType.DISTURBANCE, CallType.BACKUP, CallType.ESCORT, CallType.UNIT_SUPPORT, CallType.REPORT,
            CallType.ROUTINE, CallType.OTHER};

    private boolean newCallMenu;
    private int ncType;
    private int ncPrio;
    private String ncDesc = "", ncX = "0", ncY = "64", ncZ = "0";

    void start() {
        var p = Minecraft.getInstance().player;
        ncType = 0;
        ncPrio = 0;
        ncDesc = "";
        ncX = p == null ? "0" : String.valueOf(p.blockPosition().getX());
        ncY = p == null ? "64" : String.valueOf(p.blockPosition().getY());
        ncZ = p == null ? "0" : String.valueOf(p.blockPosition().getZ());
        newCallMenu = false;
    }

    @Override
    public void build() {
        backButton("< Annulla", () -> {
            newCallMenu = false;
            owner.finishCreating();
        });
        if (newCallMenu) {
            int half = (cw - 4) / 2;
            for (int i = 0; i < CREATABLE.length; i++) {
                final int idx = i;
                addRenderableWidget(smallButton(cx + (i % 2) * (half + 4), cy + 20 + (i / 2) * 20, half, 18,
                        CREATABLE[i].label(), i == ncType ? BLUE : DIM, () -> {
                            ncType = idx;
                            newCallMenu = false;
                            rebuildWidgets();
                        }));
            }
            return;
        }
        int half = (cw - 4) / 2;
        addRenderableWidget(dropdownButton(cx, cy + 29, half, 16, "Tipo: " + CREATABLE[ncType].label(), BLUE, () -> {
            newCallMenu = true;
            rebuildWidgets();
        }));
        int prio = ncPrio == 0 ? CREATABLE[ncType].defaultPriority() : ncPrio;
        addRenderableWidget(smallButton(cx + half + 4, cy + 29, half, 16, "Priorità P" + prio
                + (ncPrio == 0 ? " (predefinita)" : ""), PRIORITY_COLORS[prio], () -> {
                    ncPrio = (ncPrio + 1) % 5;
                    rebuildWidgets();
                }));
        Field desc = draftField(cx, cy + 59, cw, "Descrizione", 120, ncDesc, v -> ncDesc = v);
        if (ncDesc.isEmpty()) setFocused(desc);
        int third = (cw - 8) / 3;
        draftField(cx, cy + 98, third, "X", 9, ncX, v -> ncX = v);
        draftField(cx + third + 4, cy + 98, third, "Y", 9, ncY, v -> ncY = v);
        draftField(cx + 2 * (third + 4), cy + 98, cw - 2 * (third + 4), "Z", 9, ncZ, v -> ncZ = v);
        addRenderableWidget(new Btn(cx + cw - 120, cy + 128, 120, 16, Component.literal("Crea chiamata"), GREEN, () -> {
            if (ncDesc.isBlank()) return;
            send("call_create", CREATABLE[ncType].name(), String.valueOf(ncPrio), ncDesc, ncX, ncY, ncZ);
            owner.finishCreating();
        }));
    }

    @Override
    public void render(GuiGraphicsExtractor g) {
        small(g, "NUOVA CHIAMATA", cx + 86, cy + 4, cw - 86, BLUE);
        if (newCallMenu) {
            small(g, "Tipo di chiamata", cx + 86, cy + 11, cw - 86, DIM);
            return;
        }
        small(g, "Tipo e priorità", cx, cy + 20, cw, DIM);
        small(g, "Descrizione", cx, cy + 50, cw, DIM);
        small(g, "Posizione (di default la tua)", cx, cy + 80, cw, DIM);
        int third = (cw - 8) / 3;
        small(g, "X", cx, cy + 89, third, DIM);
        small(g, "Y", cx + third + 4, cy + 89, third, DIM);
        small(g, "Z", cx + 2 * (third + 4), cy + 89, third, DIM);
    }
}
