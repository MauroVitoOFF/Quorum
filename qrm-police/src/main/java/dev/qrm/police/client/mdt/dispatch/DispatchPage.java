package dev.qrm.police.client.mdt.dispatch;

import com.google.gson.JsonObject;
import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.MdtPage;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Il Dispatch: la centrale operativa. È un instradatore: decide se mostrare l'elenco delle chiamate, la pagina di una
 * chiamata o la creazione di una nuova.
 */
public final class DispatchPage extends MdtPage {
    private final CallListView calls;
    private final CallDetailView detail;
    private final NewCallView creator;
    private String openCallId;
    private boolean creating;

    public DispatchPage(MdtHost host) {
        super(host);
        calls = new CallListView(host, this);
        detail = new CallDetailView(host, this);
        creator = new NewCallView(host, this);
    }

    @Override
    public void sync() {
        super.sync();
        calls.sync();
        detail.sync();
        creator.sync();
    }

    // ---- navigazione ----

    /** Apre la pagina di una chiamata; se la pagina non è quella visibile, ci passa. */
    public void showCall(String id) {
        openCallId = id;
        creating = false;
        detail.onEnter();
        host.setForm(null);
        host.open(DispatchPage.class);
    }

    void backToList() {
        openCallId = null;
        calls.resetScroll();
        rebuildWidgets();
    }

    void startCreating() {
        creator.start();
        openCallId = null;
        creating = true;
        rebuildWidgets();
    }

    void finishCreating() {
        creating = false;
        rebuildWidgets();
    }

    /** La chiamata aperta; null se non c'è più (chiusa o annullata): si torna all'elenco. */
    JsonObject openCall() {
        if (openCallId == null) return null;
        for (JsonObject c : list("calls")) if (str(c, "id").equals(openCallId)) return c;
        openCallId = null;
        detail.onEnter();
        return null;
    }

    private MdtPage active() {
        if (creating) return creator;
        if (openCallId != null && openCall() != null) return detail;
        return calls;
    }

    @Override
    public void onEnter() {
        active().resetScroll();
    }

    /** Un secondo clic su "Dispatch" riporta all'elenco. */
    @Override
    public void onReselect() {
        openCallId = null;
        creating = false;
        detail.onEnter();
        creator.onEnter();
        calls.resetScroll();
    }

    @Override
    public boolean onScroll(double mx, double my, int step) {
        return active().onScroll(mx, my, step);
    }

    @Override
    public void build() {
        active().build();
    }

    @Override
    public void render(GuiGraphicsExtractor g) {
        active().render(g);
    }
}
