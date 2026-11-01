package dev.qrm.police.client.mdt.dashboard;

import static dev.qrm.police.client.mdt.ui.MdtWidgets.AMBER;
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
import com.google.gson.JsonObject;
import dev.qrm.police.client.ClientPolice;
import dev.qrm.police.client.mdt.MdtForm;
import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.MdtPage;
import dev.qrm.police.client.mdt.citizens.CitizensPage;
import dev.qrm.police.client.mdt.dispatch.CallFormat;
import dev.qrm.police.client.mdt.dispatch.CallRow;
import dev.qrm.police.client.mdt.ui.MdtStatus;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Btn;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;

/** Prima pagina: stato dell'agente e panico, chiamate da gestire, contatori, bacheca e note rapide. */
public final class DashboardPage extends MdtPage {
    public DashboardPage(MdtHost host) {
        super(host);
    }

    // Panico: la prima pressione arma, la seconda invia. Poi il pulsante resta bloccato quanto il ritardo del server.
    private static final long PANIC_ARM_MS = 3000;
    private static final long PANIC_SENDING_MS = 700;
    private static final long PANIC_LOCK_MS = 10_000;
    private long panicArmedUntil;
    private long panicSentAt;
    private long notesSavedAt;
    private MultiLineEditBox notesBox;
    private static final int DASH_LEFT_W = 166;
    private static final int DASH_ROW_H = 48;
    private static final int DASH_UNIT_ROWS = 3;
    private static final int DASH_BOARD_LINES = 3;

    /** 0 pronto, 1 in attesa di conferma, 2 invio in corso, 3 inviato (bloccato per poco). */
    private int panicState(long now) {
        if (panicSentAt != 0 && now - panicSentAt < PANIC_SENDING_MS) return 2;
        if (panicSentAt != 0 && now - panicSentAt < PANIC_LOCK_MS) return 3;
        return now < panicArmedUntil ? 1 : 0;
    }

    private void pressPanic() {
        long now = System.currentTimeMillis();
        int state = panicState(now);
        if (state == 0) {
            panicArmedUntil = now + PANIC_ARM_MS;
        } else if (state == 1) {
            panicArmedUntil = 0;
            panicSentAt = now;
            send("panic");
        }
    }

    /** Dalla dashboard alla lista dei cittadini, già ristretta ai ricercati. */
    private void openWantedList() {
        host.open(CitizensPage.class).showWanted();
    }

    @Override
    public void build() {
        // Intestazione: stato dell'agente e, ben separato, il panico.
        String me = myStatus();
        int sw = 66;
        for (int i = 0; i < MdtStatus.CHOICES.length; i++) {
            final String key = MdtStatus.CHOICES[i];
            final String label = MdtStatus.CHOICE_LABELS[i];
            final int color = MdtStatus.CHOICE_COLORS[i];
            final boolean selected = key.equals(me);
            addRenderableWidget(new Btn(cx + i * (sw + 3), cy, sw, 16, (g, bx, by, bw, bh, hot, active) -> {
                MdtWidgets.box(g, bx, by, bw, bh, selected || hot ? color : MdtWidgets.alpha(color, 0xA0),
                        MdtWidgets.alpha(color, selected ? 0x70 : hot ? 0x40 : 0x1C));
                small(g, label, bx + 4, by + (bh - 7) / 2 + 1, bw - 8, selected ? TEXT : DIM);
            }, () -> send("status", key)));
        }
        int panicX = cx + 3 * (sw + 3) + 3;
        addRenderableWidget(new Btn(panicX, cy, cx + cw - panicX, 16, (g, bx, by, bw, bh, hot, active) -> {
            long now = System.currentTimeMillis();
            int state = panicState(now);
            boolean blink = (now / 250) % 2 == 0;
            int color = state >= 2 ? GREEN : RED;
            MdtWidgets.box(g, bx, by, bw, bh, hot || state == 1 ? color : MdtWidgets.alpha(color, 0xC0),
                    MdtWidgets.alpha(color, state == 1 && blink ? 0x80 : hot ? 0x60 : 0x38));
            String label = switch (state) {
                case 1 -> "CONFERMA?";
                case 2 -> "INVIO...";
                case 3 -> "INVIATO";
                default -> "! PANICO";
            };
            small(g, label, bx + (bw - (int) (font.width(label) * 0.8f)) / 2, by + (bh - 7) / 2 + 1, bw - 4,
                    state == 0 && !hot ? RED : TEXT);
        }, this::pressPanic));

        // Centrale operativa.
        int y0 = cy + 22;
        List<JsonObject> calls = CallFormat.sorted(list("calls"));
        int listTop = y0 + 12;
        int rows = Math.max(1, (cy + ch - 3 - listTop) / DASH_ROW_H);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, calls.size() - rows)));
        for (int i = 0; i < rows && scroll + i < calls.size(); i++) {
            CallRow.add(this, calls.get(scroll + i), cx + 3, listTop + i * DASH_ROW_H, DASH_LEFT_W - 6, DASH_ROW_H - 2);
        }

        // Colonna laterale.
        int rx = cx + DASH_LEFT_W + 6;
        int rw = cw - DASH_LEFT_W - 6;
        int wanted = wantedCount();
        addRenderableWidget(new Btn(rx, y0, rw, 13, (g, x, y, w, h, hot, active) -> {
            MdtWidgets.box(g, x, y, w, h, hot ? AMBER : LINE, hot ? PANEL_HI : PANEL);
            g.fill(x, y, x + 3, y + h, AMBER);
            small(g, "Ricercati attivi", x + 7, y + 3, w - 30, hot ? TEXT : DIM);
            String n = String.valueOf(wanted);
            g.text(font, n, x + w - 5 - font.width(n), y + 3, AMBER, false);
        }, this::openWantedList));

        int boardY = y0 + 71;
        if (data.has("canBoard") && data.get("canBoard").getAsBoolean()) {
            addRenderableWidget(smallButton(rx + rw - 46, boardY - 2, 46, 11, "Modifica", BLUE, () -> {
                host.setForm(new MdtForm("Bacheca del dipartimento", new String[] {"Comunicazione di turno"},
                        new String[] {str(data, "board")}, v -> send("board_set", v[0])));
                rebuildWidgets();
            }));
        }

        MultiLineEditBox notes = MultiLineEditBox.builder().setX(rx + 2).setY(y0 + 130)
                .setPlaceholder(Component.literal("Appunti...")).setTextColor(TEXT).setTextShadow(false)
                .setShowBackground(false).build(font, rw - 4, cy + ch - (y0 + 130) - 1, Component.literal("Note rapide"));
        notes.setValue(ClientPolice.notes());
        // Il limite si applica qui e non sulla casella, che altrimenti disegna da sola un contatore sotto il riquadro.
        notes.setValueListener(v -> {
            if (v.length() > ClientPolice.NOTE_MAX) {
                notes.setValue(v.substring(0, ClientPolice.NOTE_MAX));
                return;
            }
            ClientPolice.saveNotes(v);
            notesSavedAt = System.currentTimeMillis();
        });
        notesBox = notes;
        addRenderableWidget(notes);
    }

    @Override
    public void render(GuiGraphicsExtractor g) {
        int y0 = cy + 22;
        int n = arr(data, "calls").size();
        g.text(font, "CHIAMATE ATTIVE (" + n + ")", cx, y0, n > 0 ? RED : DIM, false);
        MdtWidgets.inset(g, cx, y0 + 10, DASH_LEFT_W, ch - 32);
        if (n == 0) {
            // Un piccolo segno di spunta in pixel, discreto.
            int[][] check = {{0, 4}, {1, 5}, {2, 6}, {3, 5}, {4, 4}, {5, 3}, {6, 2}, {7, 1}};
            for (int[] px : check) {
                g.fill(cx + 8 + px[0], y0 + 19 + px[1], cx + 9 + px[0], y0 + 20 + px[1], MdtWidgets.alpha(GREEN, 0xB0));
            }
            g.text(font, "Nessuna chiamata in corso.", cx + 20, y0 + 18, DIM, false);
        }

        int rx = cx + DASH_LEFT_W + 6;
        int rw = cw - DASH_LEFT_W - 6;
        MdtWidgets.box(g, rx, y0 + 15, rw, 13, LINE, PANEL);
        g.fill(rx, y0 + 15, rx + 3, y0 + 28, BLUE);
        small(g, "Totale schedati", rx + 7, y0 + 18, rw - 30, DIM);
        String total = String.valueOf(arr(data, "citizens").size());
        g.text(font, total, rx + rw - 5 - font.width(total), y0 + 18, BLUE, false);

        JsonArray units = arr(data, "units");
        small(g, "UNITÀ IN SERVIZIO (" + units.size() + ")", rx, y0 + 32, rw, DIM);
        for (int i = 0; i < units.size() && i < DASH_UNIT_ROWS; i++) {
            JsonObject u = units.get(i).getAsJsonObject();
            int uy = y0 + 42 + i * 9;
            boolean more = i == DASH_UNIT_ROWS - 1 && units.size() > DASH_UNIT_ROWS;
            if (more) {
                small(g, "+ altri " + (units.size() - DASH_UNIT_ROWS + 1), rx + 8, uy, rw - 8, DIM);
            } else {
                String st = str(u, "status");
                g.fill(rx + 1, uy + 1, rx + 6, uy + 6, MdtStatus.color(st));
                String lab = MdtStatus.shortLabel(st);
                int lw = (int) (font.width(lab) * 0.8f) + 1;
                small(g, lab, rx + rw - lw - 3, uy, lw, DIM);
                small(g, str(u, "name"), rx + 10, uy, rw - 10 - lw - 6, st.equals("OFF") ? DIM : TEXT);
            }
        }

        int boardY = y0 + 71;
        small(g, "BACHECA", rx, boardY, rw, DIM);
        MdtWidgets.inset(g, rx, boardY + 10, rw, 34);
        String board = str(data, "board");
        if (board.isEmpty()) {
            small(g, "Nessuna comunicazione.", rx + 5, boardY + 15, rw - 10, DIM);
        } else {
            List<String> lines = wrap(board, (int) ((rw - 10) / 0.8f));
            for (int i = 0; i < DASH_BOARD_LINES && i < lines.size(); i++) {
                boolean cut = i == DASH_BOARD_LINES - 1 && lines.size() > DASH_BOARD_LINES;
                small(g, cut ? lines.get(i) + "..." : lines.get(i), rx + 5, boardY + 15 + i * 10, rw - 10, TEXT);
            }
        }
        small(g, "NOTE RAPIDE", rx, y0 + 119, rw, DIM);
        long now = System.currentTimeMillis();
        boolean justSaved = notesSavedAt != 0 && now - notesSavedAt < 1500;
        boolean focused = notesBox != null && notesBox.isFocused();
        int len = notesBox != null ? notesBox.getValue().length() : 0;
        String tag = len + "/" + ClientPolice.NOTE_MAX;
        int tagColor = len >= ClientPolice.NOTE_MAX ? RED : len > ClientPolice.NOTE_MAX * 8 / 10 ? ORANGE : DIM;
        int tagW = (int) (font.width(tag) * 0.8f) + 1;
        small(g, tag, rx + rw - tagW, y0 + 119, tagW, tagColor);
        MdtWidgets.box(g, rx, y0 + 129, rw, cy + ch - (y0 + 129),
                justSaved ? MdtWidgets.alpha(GREEN, 0xA0) : focused ? BLUE : 0xFF162238, 0xFF080D16);
    }

    @Override
    public boolean onScroll(double mx, double my, int step) {
        // La casella delle note ha la sua rotella.
        if (mx >= cx + DASH_LEFT_W + 6 && my >= cy + 22 + 129) return false;
        return super.onScroll(mx, my, step);
    }
}
