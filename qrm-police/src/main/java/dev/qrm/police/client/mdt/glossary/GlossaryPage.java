package dev.qrm.police.client.mdt.glossary;

import static dev.qrm.police.client.mdt.ui.MdtText.money;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.AMBER;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.BLUE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.LINE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT;

import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.MdtPage;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import dev.qrm.police.logic.PenalCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Il glossario dei reati: categorie, multe e mesi di carcere, con ricerca. */
public final class GlossaryPage extends MdtPage {
    public GlossaryPage(MdtHost host) {
        super(host);
    }

    /** Le righe del glossario: ogni categoria con i suoi reati. Una String è un'intestazione di categoria. */
    private List<Object> glossaryLines() {
        List<Object> out = new ArrayList<>();
        String last = null;
        for (PenalCode.Offense o : PenalCode.filter(query)) {
            if (!o.category().equals(last)) {
                out.add(o.category());
                last = o.category();
            }
            out.add(o);
        }
        return out;
    }

    @Override
    public void build() {
        searchField(cx, cy, cw, "Cerca reato o categoria...");
    }

    @Override
    public void render(GuiGraphicsExtractor g) {
        int top = cy + 22;
        MdtWidgets.inset(g, cx, top, cw, ch - 22);
        List<Object> lines = glossaryLines();
        if (lines.isEmpty()) {
            g.text(font, "Nessun reato trovato.", cx + 8, top + 8, DIM, false);
            return;
        }
        int rowH = 14;
        int rows = (ch - 22 - 6) / rowH;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - rows)));
        for (int i = 0; i < rows && scroll + i < lines.size(); i++) {
            int y = top + 3 + i * rowH;
            Object line = lines.get(scroll + i);
            if (line instanceof String category) {
                small(g, category.toUpperCase(Locale.ROOT), cx + 6, y + 3, cw - 12, BLUE);
                g.fill(cx + 4, y + rowH - 2, cx + cw - 4, y + rowH - 1, LINE);
            } else if (line instanceof PenalCode.Offense o) {
                String s = money(o.fine()) + " - " + (o.jailMonths() == 0 ? "no carcere" : o.jailMonths() + " mesi");
                int sw = font.width(s);
                g.text(font, MdtWidgets.fit(font, o.name(), cw - sw - 28), cx + 12, y + 3, TEXT, false);
                g.text(font, s, cx + cw - 8 - sw, y + 3, AMBER, false);
            }
        }
    }
}
