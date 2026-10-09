package dev.qrm.neoforge.client;

import dev.qrm.api.staff.StaffNodes;
import dev.qrm.client.api.staff.StaffEntries;
import dev.qrm.client.api.staff.StaffPanelModel;
import dev.qrm.client.api.staff.StaffPlayerActions;
import dev.qrm.client.api.widget.TextFit;
import dev.qrm.neoforge.net.PanelRequestPayload;
import dev.qrm.neoforge.net.PlayerInfoLine;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pannello staff HUD: menu a sezioni, lista giocatori e scheda giocatore con le azioni, in alto a sinistra sotto l'HUD del
 * personaggio. Non apre mai una {@code Screen}: frecce/Invio si osservano (non sono legati nel gameplay) e il cursore si
 * libera solo tenendo premuto {@link KeyBindings#STAFF_CURSOR}. Il server ricontrolla il nodo di ogni azione.
 *
 * <p>Aspetto: cornice scura con doppio bordo dorato, righe a placca con una gemma colorata per sezione, testa della skin
 * nella scheda giocatore, testo con ombra e un suono di clic quando si naviga (texture {@code staffpanel.*} del tema).
 */
public final class StaffPanel {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("qrm", "staff_panel");
    private static final Logger LOG = LoggerFactory.getLogger("qrm");
    private static final int MAX_ROWS = 10;
    private static final int MIN_ROWS = 3;
    private static final int SNAPSHOT_TICKS = 100;

    private static final int WIDTH = 176;
    private static final int FRAME = 12;           // bordo della cornice (slice della texture)
    /** Altezza di una placca: circa il triplo del testo, per dare aria alle voci. */
    private static final int PLATE_H = 26;
    /** Spazio fra due placche. */
    private static final int ROW_GAP = 4;
    private static final int ROW_H = PLATE_H + ROW_GAP;
    /** Margine sinistro della gemma e distanza fra gemma e testo. */
    private static final int GEM_INSET = 7;
    private static final int GEM_GAP = 8;
    private static final int GEM = 14;
    private static final int HEAD = 32;

    private static final Set<String> FAILED = new HashSet<>();
    private static final StaffPanelModel MODEL = new StaffPanelModel(new StaffPanelSource(), StaffPanel::reportFailure);

    private static boolean visible;
    /** Vero se siamo stati noi a liberare il cursore (e quindi dobbiamo riprenderlo). */
    private static boolean cursorFree;
    private static StaffPanelLayout layout;
    private static int snapshotTicks;
    /** Righe visibili all'ultimo frame (dipende dall'altezza della finestra). */
    private static int rowsVisible = 8;

    private StaffPanel() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ID, StaffPanel::render);
    }

    public static void reset() {
        visible = false;
        cursorFree = false;
        layout = null;
        FAILED.clear();
    }

    private static void reportFailure(String id) {
        if (FAILED.add(id)) LOG.warn("QRM: staff entry {} failed its availability check and is skipped", id);
    }

    private static boolean canShow(Minecraft mc) {
        return visible && mc.gui.screen() == null && !mc.gui.hud.isHidden() && !mc.getDebugOverlay().showDebugScreen()
                && ClientState.serverHasQrm() && ClientStaffAccess.allows(StaffNodes.PANEL);
    }

    private static void click(Minecraft mc, float pitch) {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    // ---- stato e tick --------------------------------------------------------------------------------------

    public static void toggle(Minecraft mc) {
        if (!ClientState.serverHasQrm() || !ClientStaffAccess.allows(StaffNodes.PANEL)) return;
        visible = !visible;
        click(mc, visible ? 1.1F : 0.9F);
        if (visible) {
            requestSnapshot();
        } else {
            restoreMouse(mc);
        }
    }

    private static void requestSnapshot() {
        snapshotTicks = 0;
        ClientPacketDistributor.sendToServer(new PanelRequestPayload());
    }

    public static void tick(Minecraft mc) {
        if (!canShow(mc)) {
            restoreMouse(mc);
            layout = null;
            return;
        }
        if (++snapshotTicks >= SNAPSHOT_TICKS) requestSnapshot();
        MODEL.refresh();
        MODEL.currentPlayer().ifPresent(p -> StaffTarget.choose(p.id(), p.name()));
        if (KeyBindings.STAFF_CURSOR.isDown()) {
            if (!cursorFree && mc.mouseHandler.isMouseGrabbed()) {
                mc.mouseHandler.releaseMouse();
                cursorFree = true;
            }
        } else {
            restoreMouse(mc);
        }
    }

    private static void restoreMouse(Minecraft mc) {
        if (!cursorFree) return;
        cursorFree = false;
        if (mc.gui.screen() == null && !mc.mouseHandler.isMouseGrabbed()) mc.mouseHandler.grabMouse();
    }

    // ---- input ---------------------------------------------------------------------------------------------

    public static void onKey(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (!canShow(mc) || event.getAction() == GLFW.GLFW_RELEASE) return;
        switch (event.getKey()) {
            case GLFW.GLFW_KEY_UP -> {
                MODEL.move(-1);
                click(mc, 1.5F);
            }
            case GLFW.GLFW_KEY_DOWN -> {
                MODEL.move(1);
                click(mc, 1.5F);
            }
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> activate(mc);
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_BACKSPACE -> goBack(mc);
            default -> { }
        }
    }

    public static void onMouse(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!canShow(mc) || !cursorFree) return;
        event.setCanceled(true);                       // altrimenti un clic riprenderebbe il mouse
        if (event.getAction() != GLFW.GLFW_PRESS || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        StaffPanelLayout l = layout;
        if (l == null) return;
        int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        if (l.isTitle(mx, my)) {
            goBack(mc);
            return;
        }
        int row = l.rowAt(mx, my);
        if (row >= 0) {
            MODEL.select(row);
            activate(mc);
        }
    }

    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!canShow(mc) || !cursorFree) return;
        event.setCanceled(true);
        MODEL.scrollBy(event.getScrollDeltaY() > 0 ? -1 : 1, rowsVisible);
    }

    private static void goBack(Minecraft mc) {
        if (MODEL.back()) {
            click(mc, 0.9F);
        } else {
            visible = false;
            click(mc, 0.9F);
            restoreMouse(mc);
        }
    }

    private static void activate(Minecraft mc) {
        int depth = MODEL.depth();
        StaffPanelModel.Outcome outcome = MODEL.enter();
        if (MODEL.depth() != depth || !(outcome instanceof StaffPanelModel.Outcome.None)) click(mc, 1.0F);
        switch (outcome) {
            case StaffPanelModel.Outcome.Open open -> {
                Optional<StaffTarget.Target> target = StaffTarget.effective(mc);
                StaffEntries.Context ctx = new StaffEntries.Context(target.map(StaffTarget.Target::id),
                        target.map(StaffTarget.Target::name).orElse(""), () -> mc.setScreenAndShow(null));
                guarded(open.entry().id(), () -> open.entry().open().accept(ctx));
            }
            case StaffPanelModel.Outcome.Run run -> {
                StaffPlayerActions.Context ctx = new StaffPlayerActions.Context(run.player().id(), run.player().name(),
                        () -> mc.setScreenAndShow(null), run.arg());
                guarded(run.action().id(), () -> run.action().run().ifPresent(r -> r.accept(ctx)));
            }
            case StaffPanelModel.Outcome.Input input -> {
                StaffPlayerActions.Action action = input.action();
                StaffPanelModel.PlayerInfo player = input.player();
                mc.setScreenAndShow(new StaffInputScreen(Component.translatable(action.labelKey()), player.name(),
                        action.fields(), action.danger(), values -> {
                            StaffPlayerActions.Context ctx = new StaffPlayerActions.Context(player.id(), player.name(),
                                    () -> mc.setScreenAndShow(null), String.join("\n", values));
                            guarded(action.id(), () -> action.run().ifPresent(r -> r.accept(ctx)));
                        }));
            }
            case StaffPanelModel.Outcome.Confirm confirm -> { }
            case StaffPanelModel.Outcome.None none -> { }
        }
    }

    /** Apre una pagina informativa nel pannello (es. dall'esito di "View permissions"). */
    static void showInfo(String titleKey, List<StaffPanelModel.InfoLine> lines) {
        MODEL.pushInfo(titleKey, lines);
    }

    private static void guarded(String id, Runnable r) {
        try {
            r.run();
        } catch (RuntimeException e) {
            LOG.warn("QRM: staff panel item {} failed", id, e);
        }
    }

    // ---- disegno -------------------------------------------------------------------------------------------

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    /** Testo con ombra, come nei menu di Minecraft. */
    private static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) {
        g.text(font, s, x, y, color, true);
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!canShow(mc)) {
            layout = null;
            return;
        }
        Font font = mc.font;
        int lh = font.lineHeight;
        int pad = 8;
        int x = pad;
        int y = HudOverlay.bottom() > 0 ? HudOverlay.bottom() + 4 : pad;
        int cx = x + FRAME;
        int cw = WIDTH - 2 * FRAME;
        int gold = StaffPanelSkin.gold();
        int cream = StaffPanelSkin.cream();
        int muted = StaffPanelSkin.muted();

        // misure verticali
        int titleH = FRAME + lh + 8;                              // dalla cima della cornice al filo ornamentale
        List<StaffPanelModel.HeaderLine> info = MODEL.header();
        int infoH = info.isEmpty() ? 0 : Math.max(HEAD + 8, info.size() * (lh + 2) + 8);
        int headerH = titleH + (infoH == 0 ? 0 : infoH + 4) + 3;
        int hintH = lh + 6;
        int below = 3 + hintH + FRAME;                             // spazio sotto le righe
        int guiH = mc.getWindow().getGuiScaledHeight();
        rowsVisible = StaffPanelLayout.fitRows(guiH, y, headerH + below + 4, ROW_H, MIN_ROWS, MAX_ROWS);
        List<StaffPanelModel.Row> rows = MODEL.rows();
        int start = MODEL.windowStart(rowsVisible);
        int shown = Math.max(0, Math.min(rowsVisible, rows.size() - start));
        int bodyH = Math.max(ROW_H, shown * ROW_H);
        int totalH = headerH + bodyH + below;

        StaffPanelSkin.drawFrame(g, x, y, WIDTH, totalH);
        layout = new StaffPanelLayout(x, y, WIDTH, titleH, headerH, ROW_H, rowsVisible, rows.size(), start);

        // titolo con rombi ai lati e filo ornamentale
        StaffPanelModel.Title title = MODEL.title();
        String head = title.literal() != null ? title.literal() : tr(title.key());
        String fitted = TextFit.fit(font, head, cw - 28);
        int tw = font.width(fitted);
        int tx = cx + (cw - tw) / 2;
        int ty = y + FRAME + 1;
        text(g, font, fitted, tx, ty, gold);
        StaffPanelSkin.diamond(g, tx - 8, ty + lh / 2, 2, gold);
        StaffPanelSkin.diamond(g, tx + tw + 8, ty + lh / 2, 2, gold);
        if (MODEL.depth() > 0) text(g, font, "‹", cx + 1, ty, StaffPanelSkin.goldDark());
        StaffPanelSkin.ornamentRule(g, cx + 2, cx + cw - 2, y + titleH - 2);

        // scheda: testa della skin e righe informative dentro un riquadro incassato
        int ry0 = y + titleH + 2;
        if (!info.isEmpty()) {
            StaffPanelSkin.draw(g, "staffpanel.inset", cx, ry0, cw, infoH, 0xFF150E22);
            int textX = cx + 6;
            Optional<StaffPanelModel.PlayerInfo> pl = MODEL.currentPlayer();
            PlayerInfo net = pl.map(p -> playerInfo(mc, p.id())).orElse(null);
            if (net != null) {
                PlayerFaceExtractor.extractRenderState(g, net.getSkin(), cx + 5, ry0 + (infoH - HEAD) / 2, HEAD);
                textX = cx + 5 + HEAD + 7;
            }
            int labelW = 0;
            for (StaffPanelModel.HeaderLine h : info) labelW = Math.max(labelW, font.width(tr(h.labelKey()) + ":"));
            int iy = ry0 + (infoH - (info.size() * (lh + 2) - 2)) / 2;
            for (StaffPanelModel.HeaderLine h : info) {
                text(g, font, tr(h.labelKey()) + ":", textX, iy, gold);
                String value = h.translateValue() ? tr(h.value()) : h.value();
                text(g, font, TextFit.fit(font, value, cx + cw - 6 - (textX + labelW + 5)), textX + labelW + 5, iy, cream);
                iy += lh + 2;
            }
        }

        // righe a placca
        int rowY = y + headerH;
        if (rows.isEmpty()) {
            text(g, font, TextFit.fit(font, tr("qrm.staff.panel.empty"), cw), cx + 4, rowY + 4, muted);
        }
        boolean home = MODEL.depth() == 0;
        for (int i = 0; i < shown; i++) {
            int idx = start + i;
            StaffPanelModel.Row row = rows.get(idx);
            int ry = rowY + i * ROW_H;
            boolean selected = idx == MODEL.selected();
            boolean armed = row.id().equals(MODEL.armedId());
            boolean soon = row.state() == StaffPanelModel.State.SOON;
            boolean off = row.state() != StaffPanelModel.State.ENABLED;
            String plate = selected || armed ? "staffpanel.row.selected" : soon ? "staffpanel.row.soon"
                    : row.danger() ? "staffpanel.row.danger" : "staffpanel.row";
            StaffPanelSkin.draw(g, plate, cx, ry, cw, PLATE_H, selected ? 0xFF45306A : 0xFF2B1F40);

            if (row.info()) {
                String detailText = row.detail() == null ? "" : row.detail();
                if (row.labelKey().charAt(0) == PlayerInfoLine.LITERAL) {
                    // riga lunga (registro): data e staff sopra, azione sotto
                    text(g, font, TextFit.fit(font, row.labelKey().substring(1), cw - 16), cx + 8, ry + 4, gold);
                    text(g, font, TextFit.fit(font, detailText, cw - 16), cx + 8, ry + 5 + lh, cream);
                } else {
                    int labelW = Math.min(cw * 2 / 5, font.width(tr(row.labelKey())) + 2);
                    text(g, font, TextFit.fit(font, tr(row.labelKey()), labelW), cx + 8, ry + (PLATE_H - lh) / 2 + 1, gold);
                    text(g, font, TextFit.fit(font, detailText, cw - labelW - 18),
                            cx + 12 + labelW, ry + (PLATE_H - lh) / 2 + 1, cream);
                }
                continue;
            }
            int labelX = cx + 8;
            if (home && StaffPanelSkin.gem(g, row.id(), cx + GEM_INSET, ry + (PLATE_H - GEM) / 2)) {
                labelX = cx + GEM_INSET + GEM + GEM_GAP;
            }
            String label = armed ? tr("qrm.staff.confirm") : row.label() != null ? row.label() : tr(row.labelKey());
            int color = armed || selected && !off ? gold : off ? muted : row.danger() ? StaffPanelSkin.danger() : cream;

            int right = cx + cw - 9;
            if (soon) {
                String chip = tr("qrm.staff.panel.soon");
                int chipW = font.width(chip) + 10;
                StaffPanelSkin.draw(g, "staffpanel.chip", cx + cw - chipW - 5, ry + (PLATE_H - 12) / 2, chipW, 12, 0xFF3A2D52);
                text(g, font, chip, cx + cw - chipW, ry + (PLATE_H - lh) / 2 + 1, muted);
                right = cx + cw - chipW - 10;
            } else {
                String detail = row.detail() != null ? row.detail() : row.deeper() ? "›" : "";
                if (!detail.isEmpty()) {
                    int dw = font.width(detail);
                    text(g, font, detail, right - dw, ry + (PLATE_H - lh) / 2 + 1, selected ? gold : muted);
                    right -= dw + 6;
                }
            }
            text(g, font, TextFit.fit(font, label, right - labelX), labelX, ry + (PLATE_H - lh) / 2 + 1, color);
        }

        // suggerimento in una vaschetta in basso
        int hy = y + headerH + bodyH + 3;
        StaffPanelSkin.draw(g, "staffpanel.inset", cx, hy, cw, hintH, 0xFF150E22);
        String hint = tr(cursorFree ? "qrm.staff.panel.hint.mouse" : "qrm.staff.panel.hint.keys");
        text(g, font, TextFit.fit(font, hint, cw - 8), cx + 4, hy + (hintH - lh) / 2 + 1, muted);
    }

    private static PlayerInfo playerInfo(Minecraft mc, UUID id) {
        return mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(id);
    }
}
