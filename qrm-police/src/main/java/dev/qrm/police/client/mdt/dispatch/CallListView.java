package dev.qrm.police.client.mdt.dispatch;

import static dev.qrm.police.client.mdt.dispatch.CallFormat.*;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.BLUE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.RED;

import com.google.gson.JsonObject;
import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Le chiamate aperte, in ordine di priorità. */
final class CallListView extends DispatchView {
    CallListView(MdtHost host, DispatchPage owner) {
        super(host, owner);
    }

    @Override
    public void build() {
        addRenderableWidget(smallButton(cx + cw - 84, cy, 84, 13, "Richiedi supporto", RED, () -> send("support")));
        if (canOperate()) {
            addRenderableWidget(smallButton(cx + cw - 84 - 4 - 54, cy, 54, 13, "+ Chiamata", BLUE, owner::startCreating));
        }
        List<JsonObject> calls = sorted(list("calls"));
        int listTop = cy + 32;
        int rows = Math.max(1, (cy + ch - 3 - listTop) / CALL_ROW_H);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, calls.size() - rows)));
        for (int i = 0; i < rows && scroll + i < calls.size(); i++) {
            CallRow.add(this, calls.get(scroll + i), cx + 3, listTop + i * CALL_ROW_H, cw - 6, CALL_ROW_H - 2);
        }
    }

    @Override
    public void render(GuiGraphicsExtractor g) {
        MdtWidgets.inset(g, cx, cy + 31, cw, ch - 31);
        if (list("calls").isEmpty()) g.text(font, "Nessuna chiamata in corso.", cx + 8, cy + 40, DIM, false);
    }

}
