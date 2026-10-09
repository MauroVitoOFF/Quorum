package dev.qrm.neoforge.client;

import dev.qrm.client.api.hud.HudLine;
import dev.qrm.client.api.hud.HudRow;
import dev.qrm.client.api.hud.HudRows;
import dev.qrm.client.api.theme.QrmTheme;
import dev.qrm.client.api.theme.ThemeManifest;
import dev.qrm.client.api.theme.ThemeRenderer;
import dev.qrm.client.api.widget.TextFit;
import dev.qrm.neoforge.net.PlayerStatus;
import dev.qrm.neoforge.net.UiConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HUD del personaggio: pannello in alto a sinistra con le righe che il server ha configurato, nell'ordine indicato
 * (nome, saldo principale, lavoro e righe dei moduli).
 */
public final class HudOverlay {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("qrm", "hud");
    private static final Logger LOG = LoggerFactory.getLogger("qrm");
    private static final int DOT = 5;
    private static final int DOT_GAP = 4;
    /** Righe di modulo che hanno lanciato un'eccezione: segnalate una volta sola. */
    private static final Set<String> FAILED_ROWS = new HashSet<>();

    private record Line(String text, String colorKey, String dotKey) {}

    /** Bordo inferiore del pannello HUD all'ultimo frame (0 se non è disegnato): il pannello staff si impila sotto. */
    private static volatile int bottom;

    private HudOverlay() {}

    public static int bottom() {
        return bottom;
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ID, HudOverlay::render);
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        bottom = 0;
        Minecraft mc = Minecraft.getInstance();
        PlayerStatus s = ClientState.status();
        UiConfig ui = s.ui();
        boolean uiHidden = mc.gui.hud.isHidden() || mc.getDebugOverlay().showDebugScreen();
        if (!HudVisibility.visible(mc.gui.screen() == null, uiHidden, ClientConfig.HUD_VISIBLE.get(),
                s.hasCharacter() && !ui.hudElements().isEmpty())) {
            return;
        }
        ThemeManifest theme = QrmTheme.current();
        Font font = mc.font;
        int pad = Math.max(0, (int) Math.round(theme.metric("padding")));
        int maxW = Math.max(1, (int) Math.round(theme.metric("hudMaxWidth")));
        int inner = Math.max(1, maxW - 2 * pad);
        int lh = font.lineHeight;

        List<Line> lines = new ArrayList<>();
        for (String element : ui.hudElements()) {
            Line line = switch (element) {
                case UiConfig.HUD_NAME -> plain(font, s.characterName(), "accent", inner);
                case UiConfig.HUD_BALANCE -> plain(font, s.primaryBalance().map(s::format).orElse(""), "text", inner);
                case UiConfig.HUD_JOB -> jobLine(font, s, inner);
                default -> moduleLine(font, element, inner);
            };
            if (line != null) lines.add(line);
        }
        if (lines.isEmpty()) return;

        int contentW = 0;
        for (Line l : lines) contentW = Math.max(contentW, font.width(l.text()) + indent(l));
        int step = lh + 2;
        int panelW = Math.min(maxW, contentW + 2 * pad);
        int panelH = 2 * pad + lines.size() * lh + (lines.size() - 1) * 2;
        ThemeRenderer.draw(g, "hud.panel", pad, pad, panelW, panelH);
        bottom = pad + panelH;

        int x = 2 * pad;
        int y = 2 * pad;
        for (Line l : lines) {
            if (l.dotKey() != null) {
                int dotY = y + (lh - DOT) / 2;
                g.fill(x, dotY, x + DOT, dotY + DOT, color(l.dotKey()));
            }
            g.text(font, l.text(), x + indent(l), y, color(l.colorKey()));
            y += step;
        }
    }

    private static int indent(Line l) {
        return l.dotKey() != null ? DOT + DOT_GAP : 0;
    }

    private static Line plain(Font font, String text, String colorKey, int inner) {
        String fitted = TextFit.fit(font, text, inner);
        return fitted.isEmpty() ? null : new Line(fitted, colorKey, null);
    }

    private static Line jobLine(Font font, PlayerStatus s, int inner) {
        Optional<PlayerStatus.JobInfo> job = s.job();
        if (job.isEmpty()) {
            return plain(font, Component.translatable("qrm.hud.unemployed").getString(), "muted", inner);
        }
        PlayerStatus.JobInfo j = job.get();
        String text = TextFit.fit(font, Component.translatable("qrm.hud.job", j.jobLabel(), j.gradeLabel()).getString(),
                inner - DOT - DOT_GAP);
        return text.isEmpty() ? null : new Line(text, "text", j.onDuty() ? "statusOn" : "statusOff");
    }

    /** Riga di un modulo: un'eccezione la salta (e viene segnalata una volta sola) senza rompere l'HUD. */
    private static Line moduleLine(Font font, String id, int inner) {
        try {
            Optional<HudLine> line = HudRows.find(id).flatMap(HudRow::line);
            if (line.isEmpty()) return null;
            HudLine l = line.get();
            int indent = l.dotColorKey() != null ? DOT + DOT_GAP : 0;
            String text = TextFit.fit(font, l.text(), inner - indent);
            return text.isEmpty() ? null : new Line(text, l.colorKey(), l.dotColorKey());
        } catch (RuntimeException e) {
            if (FAILED_ROWS.add(id)) LOG.warn("QRM: HUD row {} failed and is skipped", id, e);
            return null;
        }
    }

    /** Colore del tema; una chiave sconosciuta (ad es. di un modulo) ricade su {@code text}. */
    private static int color(String key) {
        try {
            return ThemeRenderer.color(key);
        } catch (IllegalArgumentException e) {
            return ThemeRenderer.color("text");
        }
    }
}
