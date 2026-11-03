package dev.qrm.police.client.mdt.dashboard;

import static dev.qrm.police.client.mdt.ui.MdtWidgets.BLUE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.GREEN;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.ORANGE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.PANEL;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.RED;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.TEXT_SOFT;

import com.google.gson.JsonObject;
import dev.qrm.police.client.ClientPolice;
import dev.qrm.police.client.mdt.MdtForm;
import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.MdtPage;
import dev.qrm.police.client.mdt.ui.MdtStatus;
import dev.qrm.police.client.mdt.ui.MdtWidgets.Btn;
import dev.qrm.police.client.mdt.ui.MdtWidgets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/** Prima pagina: stato dell'agente e panico, bacheca, agenti attivi e note rapide. */
public final class DashboardPage extends MdtPage {
    private final OpsView ops;
    /** L'agente ha scelto la Dashboard classica pur avendo una chiamata in carico. */
    private boolean classic;

    public DashboardPage(MdtHost host) {
        super(host);
        ops = new OpsView(host);
    }

    @Override
    public void sync() {
        super.sync();
        ops.sync();
    }

    /** Con una chiamata in carico (e se non si è scelta la vista classica) il corpo è la Dashboard operativa. */
    private boolean opsMode() {
        return !classic && ops.mine() != null;
    }

    // Panico: la prima pressione arma, la seconda invia. Poi il pulsante resta bloccato quanto il ritardo del server.
    private static final long PANIC_ARM_MS = 3000;
    private static final long PANIC_SENDING_MS = 700;
    private static final long PANIC_LOCK_MS = 10_000;
    private long panicArmedUntil;
    private long panicSentAt;
    private long notesSavedAt;
    private MultiLineEditBox notesBox;
    private static final int DASH_UNIT_H = 16;
    private static final int DASH_FACE = 12;
    private static final int DASH_BOARD_LINES = 5;
    private static final int DASH_BOARD_BOX_H = 56;
    // Sotto la bacheca (rispetto a y0): a sinistra gli agenti attivi, a destra le note rapide.
    private static final int DASH_LOWER_Y = 12 + DASH_BOARD_BOX_H + 6;

    private int unitsW() {
        return cw * 3 / 5;
    }

    private int notesX() {
        return cx + unitsW() + 6;
    }

    /** Quante righe di agenti stanno nel riquadro. */
    private int unitRows() {
        return Math.max(1, (ch - 22 - DASH_LOWER_Y - 10 - 4) / DASH_UNIT_H);
    }

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
            // Strisce affiancate: lo stato scelto è pieno di colore con la linea sotto, gli altri sono neutri.
            addRenderableWidget(new Btn(cx + i * (sw + 2), cy, sw, 16, (g, bx, by, bw, bh, hot, active) -> {
                g.fill(bx, by, bx + bw, by + bh, selected ? MdtWidgets.alpha(color, 0x38)
                        : hot ? MdtWidgets.PANEL_HI : MdtWidgets.PANEL);
                if (selected) g.fill(bx, by + bh - 2, bx + bw, by + bh, MdtWidgets.alpha(color, 0xE0));
                int dot = bx + 6;
                int dy = by + (bh - 4) / 2 - (selected ? 1 : 0);
                g.fill(dot, dy, dot + 4, dy + 4, MdtWidgets.alpha(color, selected ? 0xFF : hot ? 0xB0 : 0x70));
                small(g, label, bx + 14, by + (bh - 7) / 2 + (selected ? 0 : 1), bw - 18,
                        selected ? TEXT : hot ? TEXT_SOFT : DIM);
            }, () -> send("status", key)));
        }
        int panicX = cx + 3 * (sw + 2) + 6;
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

        int y0 = cy + 22;
        if (ops.mine() != null) {
            // Interruttore tra la Dashboard operativa e quella classica.
            boolean opsNow = opsMode();
            int tw = 76;
            int tx = opsNow || !data.has("canBoard") || !data.get("canBoard").getAsBoolean() ? cx + cw - tw
                    : cx + cw - 46 - 4 - tw;
            addRenderableWidget(smallButton(tx, y0, tw, 11, opsNow ? "Vista classica" : "Vista operativa", BLUE, () -> {
                classic = !classic;
                rebuildWidgets();
            }));
        }
        if (opsMode()) {
            notesBox = null;
            ops.build();
            return;
        }
        if (data.has("canBoard") && data.get("canBoard").getAsBoolean()) {
            addRenderableWidget(smallButton(cx + cw - 46, y0, 46, 11, "Modifica", BLUE, () -> {
                host.setForm(new MdtForm("Bacheca del dipartimento", new String[] {"Comunicazione di turno"},
                        new String[] {str(data, "board")}, v -> send("board_set", v[0])));
                rebuildWidgets();
            }));
        }

        int nx = notesX();
        int notesTop = y0 + DASH_LOWER_Y + 11;
        MultiLineEditBox notes = MultiLineEditBox.builder().setX(nx + 2).setY(notesTop)
                .setPlaceholder(Component.literal("Appunti...")).setTextColor(TEXT).setTextShadow(false)
                .setShowBackground(false)
                .build(font, cx + cw - nx - 4, cy + ch - notesTop - 1, Component.literal("Note rapide"));
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
        if (opsMode()) {
            small(g, "CHIAMATA IN CARICO", cx, cy + 25, cw - 80, DIM);
            ops.render(g);
            return;
        }
        int y0 = cy + 22;
        int rx = cx;
        int rw = cw;
        // Bacheca: la comunicazione di turno, in cima e con un riquadro in rilievo.
        g.text(font, "BACHECA", rx, y0, TEXT, false);
        MdtWidgets.box(g, rx, y0 + 12, rw, DASH_BOARD_BOX_H, BLUE, PANEL);
        g.fill(rx, y0 + 12, rx + 3, y0 + 12 + DASH_BOARD_BOX_H, BLUE);
        String board = str(data, "board");
        if (board.isEmpty()) {
            small(g, "Nessuna comunicazione.", rx + 8, y0 + 18, rw - 14, DIM);
        } else {
            List<String> lines = wrap(board, (int) ((rw - 14) / 0.8f));
            for (int i = 0; i < DASH_BOARD_LINES && i < lines.size(); i++) {
                boolean cut = i == DASH_BOARD_LINES - 1 && lines.size() > DASH_BOARD_LINES;
                small(g, cut ? lines.get(i) + "..." : lines.get(i), rx + 8, y0 + 18 + i * 10, rw - 14, TEXT);
            }
        }

        List<JsonObject> active = new ArrayList<>();
        for (JsonObject u : list("units")) if (!str(u, "status").equals("OFF")) active.add(u);
        int ly = y0 + DASH_LOWER_Y;
        int uw = unitsW();
        int rows = unitRows();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, active.size() - rows)));
        small(g, "AGENTI ATTIVI (" + active.size() + ")", rx, ly, uw, DIM);
        int boxH = cy + ch - (ly + 10);
        MdtWidgets.inset(g, rx, ly + 10, uw, boxH);
        if (active.isEmpty()) small(g, "Nessun agente in servizio.", rx + 6, ly + 16, uw - 12, DIM);
        boolean bar = active.size() > rows;
        int textW = uw - DASH_FACE - 20 - (bar ? 4 : 0);
        for (int i = 0; i < rows && scroll + i < active.size(); i++) {
            JsonObject u = active.get(scroll + i);
            int uy = ly + 12 + i * DASH_UNIT_H;
            face(g, u, rx + 5, uy + 1);
            small(g, str(u, "name"), rx + DASH_FACE + 12, uy + 0, textW, TEXT);
            String rank = str(u, "rank");
            small(g, rank.isEmpty() ? "Grado non assegnato" : rank, rx + DASH_FACE + 12, uy + 8, textW, DIM);
            int dot = uw - 10 - (bar ? 4 : 0);
            g.fill(rx + dot, uy + 4, rx + dot + 4, uy + 8, MdtStatus.color(str(u, "status")));
        }
        if (bar) {
            // Barra di scorrimento: la tacca è in proporzione a quanti agenti si vedono.
            int trackY = ly + 12;
            int trackH = rows * DASH_UNIT_H;
            int thumbH = Math.max(6, trackH * rows / active.size());
            int thumbY = trackY + (trackH - thumbH) * scroll / (active.size() - rows);
            g.fill(rx + uw - 5, trackY, rx + uw - 3, trackY + trackH, MdtWidgets.INSET_LINE);
            g.fill(rx + uw - 5, thumbY, rx + uw - 3, thumbY + thumbH, MdtWidgets.DIM);
        }
        int nx = notesX();
        int nw = cx + cw - nx;
        small(g, "NOTE RAPIDE", nx, ly, nw, DIM);
        long now = System.currentTimeMillis();
        boolean justSaved = notesSavedAt != 0 && now - notesSavedAt < 1500;
        boolean focused = notesBox != null && notesBox.isFocused();
        int len = notesBox != null ? notesBox.getValue().length() : 0;
        String tag = len + "/" + ClientPolice.NOTE_MAX;
        int tagColor = len >= ClientPolice.NOTE_MAX ? RED : len > ClientPolice.NOTE_MAX * 8 / 10 ? ORANGE : DIM;
        int tagW = (int) (font.width(tag) * 0.8f) + 1;
        small(g, tag, nx + nw - tagW, ly, tagW, tagColor);
        MdtWidgets.box(g, nx, ly + 10, nw, cy + ch - (ly + 10),
                justSaved ? MdtWidgets.alpha(GREEN, 0xA0) : focused ? BLUE : MdtWidgets.INSET_LINE, MdtWidgets.INSET_FILL);
    }

    /** La faccia della skin dell'agente se è online; altrimenti un quadrato con le iniziali. */
    private void face(GuiGraphicsExtractor g, JsonObject unit, int x, int y) {
        MdtWidgets.outline(g, x - 1, y - 1, DASH_FACE + 2, DASH_FACE + 2, MdtWidgets.LINE);
        UUID id;
        try {
            id = UUID.fromString(str(unit, "id"));
        } catch (IllegalArgumentException e) {
            id = new UUID(0, 0);
        }
        var conn = Minecraft.getInstance().getConnection();
        PlayerInfo info = conn == null ? null : conn.getPlayerInfo(id);
        if (info != null) {
            PlayerFaceExtractor.extractRenderState(g, info.getSkin(), x, y, DASH_FACE);
            return;
        }
        g.fill(x, y, x + DASH_FACE, y + DASH_FACE, MdtWidgets.PANEL_HI);
        String name = str(unit, "name");
        String ini = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
        g.text(font, ini, x + (DASH_FACE - font.width(ini)) / 2, y + 2, TEXT, false);
    }

    @Override
    public boolean onScroll(double mx, double my, int step) {
        // La casella delle note ha la sua rotella.
        if (opsMode()) return false;
        if (mx >= notesX() && my >= cy + 22 + DASH_LOWER_Y + 10) return false;
        return super.onScroll(mx, my, step);
    }
}
