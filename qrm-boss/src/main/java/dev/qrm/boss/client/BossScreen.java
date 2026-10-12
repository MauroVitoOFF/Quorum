package dev.qrm.boss.client;

import dev.qrm.boss.logic.BossAction;
import dev.qrm.boss.logic.BossReply;
import dev.qrm.boss.logic.BossRequest;
import dev.qrm.boss.logic.BossResult;
import dev.qrm.boss.logic.BossSnapshot;
import dev.qrm.boss.net.BossActionPayload;
import dev.qrm.client.api.screen.QrmScreen;
import dev.qrm.client.api.theme.QrmTheme;
import dev.qrm.client.api.theme.ThemeRenderer;
import dev.qrm.client.api.widget.TextFit;
import dev.qrm.client.api.widget.ThemedField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Menu del boss aziendale: Panoramica, Membri e Conto. Mostra l'ultimo snapshot del server e manda richieste: non
 * applica mai modifiche in locale. Lo stato vive nei campi, così {@link #rebuildWidgets()} ricostruisce tutto.
 */
final class BossScreen extends QrmScreen {
    enum Section { OVERVIEW, MEMBERS, ACCOUNT }

    private static final int FRAME_MAX_W = 340;
    private static final int FRAME_MAX_H = 230;
    private static final int BORDER = 12;
    private static final int ROW_H = 16;
    private static final int BUTTON_H = 20;
    private static final int GAP = 4;
    private static final int PLATE_H = 36;
    private static final int MASCOT_W = 32;
    private static final int MASCOT_H = 40;
    private static final int MASCOT_FRAMES = 8;
    private static final long MASCOT_FRAME_MS = 140;
    private static final String[] PRESETS = {"500", "25%", "50%", "all"};

    private BossSnapshot snapshot;
    private Section section = Section.OVERVIEW;
    private Component status = Component.empty();
    private int statusColor;
    private boolean closed;

    private boolean addMode;
    private UUID selectedMember;
    private UUID selectedCandidate;
    private int scroll;
    private boolean fireArmed;
    private String preset = PRESETS[0];
    private String amountText = "";
    private BossAction lastAction = BossAction.REFRESH;

    private int x0, y0, fw, fh, cx, cw, top, bottom, statusY;

    BossScreen(BossReply first) {
        super(Component.translatable("qrm_boss.title"));
        onState(first);
    }

    String orgId() {
        return snapshot == null ? "" : snapshot.orgId();
    }

    // ---- stato dal server ----------------------------------------------------------------------------

    void onState(BossReply reply) {
        BossResult result = reply.result();
        if (reply.snapshot().isPresent()) {
            snapshot = reply.snapshot().get();
            closed = false;
            if (!available(section)) section = Section.OVERVIEW;
            if (selectedMember != null && snapshot.members().stream().noneMatch(m -> m.character().equals(selectedMember))) {
                selectedMember = null;
            }
            if (selectedCandidate != null
                    && snapshot.candidates().stream().noneMatch(c -> c.character().equals(selectedCandidate))) {
                selectedCandidate = null;
            }
            fireArmed = false;
            if (result == BossResult.OK && (lastAction == BossAction.DEPOSIT || lastAction == BossAction.WITHDRAW)) {
                amountText = "";
            }
            if (result == BossResult.OK && lastAction == BossAction.ADD_MEMBER) addMode = false;
        } else if (result.closesScreen()) {
            snapshot = null;
            closed = true;
        }
        if (!(result == BossResult.OK && lastAction == BossAction.REFRESH)) {
            status = Component.translatable(result.translationKey());
            statusColor = ThemeRenderer.color(result == BossResult.OK ? "qrm_boss.ok" : "qrm_boss.err");
        }
        lastAction = BossAction.REFRESH;
        if (this.minecraft != null) rebuildWidgets();
    }

    private boolean available(Section s) {
        return switch (s) {
            case OVERVIEW -> true;
            case MEMBERS -> snapshot.showMembers();
            case ACCOUNT -> snapshot.showAccount();
        };
    }

    private void send(BossAction action, Optional<UUID> target, String rankId, String amount) {
        if (snapshot == null) return;
        lastAction = action;
        ClientPacketDistributor.sendToServer(new BossActionPayload(new BossRequest(action, snapshot.orgId(),
                snapshot.character(), target, rankId, amount, UUID.randomUUID())));
    }

    private void setSection(Section s) {
        section = s;
        scroll = 0;
        fireArmed = false;
        rebuildWidgets();
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private int lineH() {
        return this.font.lineHeight;
    }

    // ---- layout ----------------------------------------------------------------------------------------

    @Override
    protected void init() {
        fw = Math.min(FRAME_MAX_W, this.width - 40);
        fh = Math.max(150, Math.min(FRAME_MAX_H, this.height - 50));
        x0 = Math.max(30, (this.width - fw) / 2);
        y0 = Math.max(MASCOT_H, (this.height - fh) / 2);
        cx = x0 + BORDER;
        cw = fw - 2 * BORDER;
        top = y0 + 16;
        statusY = y0 + fh - BORDER - lineH() + 2;
        bottom = statusY - 4;
        if (snapshot == null) return;

        // linguette a sinistra della cornice
        int ty = y0 + 14;
        ty = addTab(ty, Section.OVERVIEW, "qrm_boss.section.overview", "qrm_boss.icon_overview");
        if (snapshot.showMembers()) ty = addTab(ty, Section.MEMBERS, "qrm_boss.section.members", "qrm_boss.icon_members");
        if (snapshot.showAccount()) addTab(ty, Section.ACCOUNT, "qrm_boss.section.account", "qrm_boss.icon_account");

        switch (section) {
            case OVERVIEW -> {}
            case MEMBERS -> initMembers();
            case ACCOUNT -> initAccount();
        }
    }

    private int addTab(int y, Section s, String labelKey, String icon) {
        addRenderableWidget(BossButton.tab(x0 - 26, y, Component.translatable(labelKey), icon, section == s,
                () -> setSection(s)));
        return y + 22;
    }

    // ---- sezione Membri ----------------------------------------------------------------------------------

    private int listX() {
        return cx;
    }

    private int listW() {
        return (int) (cw * 0.58);
    }

    private int panelX() {
        return cx + listW() + GAP;
    }

    private int panelW() {
        return cw - listW() - GAP;
    }

    private int listTop() {
        return top + lineH() + 6;
    }

    private int rows() {
        return Math.max(1, (bottom - listTop() - 4) / ROW_H);
    }

    private void initMembers() {
        boolean canEdit = snapshot.level() >= 0;
        int bw = panelW() - 12;
        int bx = panelX() + 6;
        int by = listTop() + 4;

        String toggle = tr(addMode ? "qrm_boss.members.back" : "qrm_boss.members.add");
        BossButton toggleButton = addRenderableWidget(BossButton.button(cx + cw - 90, top - 2, 90, BUTTON_H - 2,
                Component.literal(toggle), "qrm_boss.icon_add", BossButton.Style.DEFAULT, () -> {
                    addMode = !addMode;
                    scroll = 0;
                    fireArmed = false;
                    rebuildWidgets();
                }));
        toggleButton.active = canEdit;

        List<?> items = addMode ? snapshot.candidates() : snapshot.members();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, items.size() - rows())));
        for (int i = 0; i < rows() && scroll + i < items.size(); i++) {
            int ry = listTop() + 2 + i * ROW_H;
            if (addMode) {
                BossSnapshot.Candidate c = snapshot.candidates().get(scroll + i);
                addRenderableWidget(new BossRow(listX() + 2, ry, listW() - 4, ROW_H, c.name(), "",
                        c.character().equals(selectedCandidate), () -> {
                            selectedCandidate = c.character();
                            rebuildWidgets();
                        }));
            } else {
                BossSnapshot.Member m = snapshot.members().get(scroll + i);
                addRenderableWidget(new BossRow(listX() + 2, ry, listW() - 4, ROW_H, m.name(), m.rankLabel(),
                        m.character().equals(selectedMember), () -> {
                            selectedMember = m.character();
                            fireArmed = false;
                            rebuildWidgets();
                        }));
            }
        }

        if (addMode) {
            String lowest = snapshot.ranks().isEmpty() ? "" : snapshot.ranks().get(0).label();
            BossButton add = addRenderableWidget(BossButton.button(bx, by + 24, bw, BUTTON_H,
                    Component.translatable("qrm_boss.members.add_confirm", lowest), "qrm_boss.icon_add",
                    BossButton.Style.CONFIRM,
                    () -> send(BossAction.ADD_MEMBER, Optional.of(selectedCandidate), "", "")));
            add.active = canEdit && selectedCandidate != null && !snapshot.ranks().isEmpty();
            return;
        }

        BossSnapshot.Member m = selectedMember == null ? null : snapshot.members().stream()
                .filter(x -> x.character().equals(selectedMember)).findFirst().orElse(null);
        boolean editable = m != null && m.editable();
        BossSnapshot.RankOption up = m == null ? null : snapshot.ranks().stream()
                .filter(r -> r.level() > m.level()).findFirst().orElse(null);
        BossSnapshot.RankOption down = m == null ? null : lastBelow(m.level());
        BossButton promote = addRenderableWidget(BossButton.button(bx, by + 36, bw, BUTTON_H,
                Component.translatable("qrm_boss.members.promote"), "qrm_boss.icon_promote", BossButton.Style.DEFAULT,
                () -> send(BossAction.SET_RANK, Optional.of(selectedMember), up.id(), "")));
        promote.active = editable && up != null;
        BossButton demote = addRenderableWidget(BossButton.button(bx, by + 36 + BUTTON_H + GAP, bw, BUTTON_H,
                Component.translatable("qrm_boss.members.demote"), null, BossButton.Style.DEFAULT,
                () -> send(BossAction.SET_RANK, Optional.of(selectedMember), down.id(), "")));
        demote.active = editable && down != null;
        BossButton fire = addRenderableWidget(BossButton.button(bx, by + 36 + 2 * (BUTTON_H + GAP), bw, BUTTON_H,
                Component.translatable(fireArmed ? "qrm_boss.members.fire_confirm" : "qrm_boss.members.fire"),
                "qrm_boss.icon_fire", BossButton.Style.DANGER, () -> {
                    if (!fireArmed) {
                        fireArmed = true;
                        rebuildWidgets();
                    } else {
                        fireArmed = false;
                        send(BossAction.REMOVE_MEMBER, Optional.of(selectedMember), "", "");
                    }
                }));
        fire.active = editable;
    }

    private BossSnapshot.RankOption lastBelow(int level) {
        BossSnapshot.RankOption found = null;
        for (BossSnapshot.RankOption r : snapshot.ranks()) if (r.level() < level) found = r;
        return found;
    }

    // ---- sezione Conto -----------------------------------------------------------------------------------

    private ThemedField amountField;

    private void initAccount() {
        int py = top + PLATE_H + 8 + lineH() + 4;
        int pw = (cw - 3 * GAP) / 4;
        for (int i = 0; i < PRESETS.length; i++) {
            String p = PRESETS[i];
            String label = p.equals("all") ? tr("qrm_boss.account.all") : p;
            addRenderableWidget(BossButton.button(cx + i * (pw + GAP), py, pw, BUTTON_H, Component.literal(label), null,
                    BossButton.Style.DEFAULT, () -> {
                        preset = p;
                        amountText = "";
                        rebuildWidgets();
                    }).selected(amountText.isBlank() && p.equals(preset)));
        }
        int fy = py + BUTTON_H + 8;
        amountField = addRenderableWidget(new ThemedField(this.font, cx, fy, cw, BUTTON_H,
                Component.translatable("qrm_boss.account.custom")));
        amountField.setMaxLength(BossRequest.MAX_AMOUNT);
        amountField.setHint(Component.translatable("qrm_boss.account.custom"));
        amountField.setValue(amountText);
        amountField.setResponder(t -> amountText = t);

        int by = fy + BUTTON_H + 8;
        int bw = (cw - GAP) / 2;
        BossButton deposit = addRenderableWidget(BossButton.button(cx, by, bw, BUTTON_H,
                Component.translatable("qrm_boss.account.deposit"), "qrm_boss.icon_deposit",
                BossButton.Style.CONFIRM, () -> send(BossAction.DEPOSIT, Optional.empty(), "", chosenAmount())));
        deposit.active = snapshot.canDeposit();
        BossButton withdraw = addRenderableWidget(BossButton.button(cx + bw + GAP, by, cw - bw - GAP, BUTTON_H,
                Component.translatable("qrm_boss.account.withdraw"), "qrm_boss.icon_withdraw",
                BossButton.Style.DEFAULT, () -> send(BossAction.WITHDRAW, Optional.empty(), "", chosenAmount())));
        withdraw.active = snapshot.canWithdraw();
    }

    private String chosenAmount() {
        return amountText.isBlank() ? preset : amountText.strip();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (snapshot != null && section == Section.MEMBERS && scrollY != 0) {
            int total = addMode ? snapshot.candidates().size() : snapshot.members().size();
            int next = Math.max(0, Math.min(scroll - (int) Math.signum(scrollY), Math.max(0, total - rows())));
            if (next != scroll) {
                scroll = next;
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    // ---- disegno ---------------------------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        ThemeRenderer.draw(g, "qrm_boss.frame", x0, y0, fw, fh);
        if (snapshot == null) {
            g.centeredText(this.font, TextFit.fit(this.font, status.getString(), cw), this.width / 2, y0 + fh / 2,
                    statusColor);
            return;
        }
        switch (section) {
            case OVERVIEW -> drawOverview(g);
            case MEMBERS -> drawMembers(g);
            case ACCOUNT -> drawAccount(g);
        }
        g.centeredText(this.font, TextFit.fit(this.font, status.getString(), cw), this.width / 2, statusY, statusColor);
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        int plateW = Math.min(fw - 90, Math.max(120, this.font.width(snapshot.orgLabel()) + 40));
        int px = x0 + (fw - plateW) / 2;
        int py = y0 - 8;
        ThemeRenderer.draw(g, "qrm_boss.title_plate", px, py, plateW, 16);
        String title = TextFit.fit(this.font, snapshot.orgLabel(), plateW - 16);
        g.centeredText(this.font, title, x0 + fw / 2, py + (16 - lineH()) / 2 + 1, ThemeRenderer.color("qrm_boss.gold"));

        drawMascot(g);

        for (var child : this.children()) {
            if (child instanceof BossButton b && b.hovered() && b.getWidth() == 28) {
                g.setTooltipForNextFrame(this.font, b.getMessage(), mouseX, mouseY);
            }
        }
    }

    /** La mascotte in piedi sul bordo superiore della cornice, a destra: un frame ogni 140 ms. */
    private void drawMascot(GuiGraphicsExtractor g) {
        var texture = QrmTheme.current().texture("qrm_boss.boss_mascot");
        int frame = (int) ((System.currentTimeMillis() / MASCOT_FRAME_MS) % MASCOT_FRAMES);
        g.blit(RenderPipelines.GUI_TEXTURED, Identifier.parse(texture.id()), x0 + fw - MASCOT_W - 4, y0 - MASCOT_H + 2,
                0F, (float) (frame * MASCOT_H), MASCOT_W, MASCOT_H, MASCOT_W, MASCOT_H, texture.width(), texture.height());
    }

    private void line(GuiGraphicsExtractor g, int y, String label, String value) {
        g.text(this.font, label, cx + 8, y, ThemeRenderer.color("qrm_boss.cream_soft"));
        String v = TextFit.fit(this.font, value, cw - 24 - this.font.width(label));
        g.text(this.font, v, cx + cw - 8 - this.font.width(v), y, ThemeRenderer.color("qrm_boss.cream"));
    }

    private void drawOverview(GuiGraphicsExtractor g) {
        int h = Math.min(bottom - top, 5 * (lineH() + 6) + 10);
        ThemeRenderer.draw(g, "qrm_boss.inset", cx, top, cw, h);
        int y = top + 8;
        int step = lineH() + 6;
        line(g, y, tr("qrm_boss.overview.company"), snapshot.orgLabel());
        line(g, y += step, tr("qrm_boss.overview.status"), tr("qrm_boss.status." + snapshot.status().toLowerCase(java.util.Locale.ROOT)));
        line(g, y += step, tr("qrm_boss.overview.rank"), snapshot.level() < 0 ? tr("qrm_boss.overview.rank_job")
                : Component.translatable("qrm_boss.overview.rank_value", snapshot.rankLabel(), snapshot.level()).getString());
        line(g, y += step, tr("qrm_boss.overview.members"), Integer.toString(snapshot.memberCount()));
        if (snapshot.hasBalance()) line(g, y + step, tr("qrm_boss.overview.balance"), snapshot.balance());
    }

    private void drawMembers(GuiGraphicsExtractor g) {
        String header = addMode ? tr("qrm_boss.members.candidates")
                : Component.translatable("qrm_boss.members.header", snapshot.memberCount()).getString();
        g.text(this.font, header, cx + 2, top + 1, ThemeRenderer.color("qrm_boss.ink"), false);
        int h = rows() * ROW_H + 4;
        ThemeRenderer.draw(g, "qrm_boss.inset", listX(), listTop(), listW(), h);
        ThemeRenderer.draw(g, "qrm_boss.inset", panelX(), listTop(), panelW(), h);
        List<?> items = addMode ? snapshot.candidates() : snapshot.members();
        if (items.isEmpty()) {
            g.text(this.font, TextFit.fit(this.font, tr(addMode ? "qrm_boss.members.no_candidates"
                    : "qrm_boss.members.empty"), listW() - 16), listX() + 8, listTop() + 8, ThemeRenderer.color("qrm_boss.cream_soft"));
        }
        int tx = panelX() + 8;
        int ty = listTop() + 6;
        if (snapshot.level() < 0) {
            g.text(this.font, TextFit.fit(this.font, tr("qrm_boss.members.read_only"), panelW() - 16), tx, ty,
                    ThemeRenderer.color("qrm_boss.cream_soft"));
        } else if (!addMode && selectedMember != null) {
            snapshot.members().stream().filter(m -> m.character().equals(selectedMember)).findFirst().ifPresent(m ->
                    g.text(this.font, TextFit.fit(this.font, m.name(), panelW() - 16), tx, ty, ThemeRenderer.color("qrm_boss.cream")));
        } else if (addMode) {
            g.text(this.font, TextFit.fit(this.font, tr("qrm_boss.members.add_hint"), panelW() - 16), tx, ty,
                    ThemeRenderer.color("qrm_boss.cream_soft"));
        } else {
            g.text(this.font, TextFit.fit(this.font, tr("qrm_boss.members.select_hint"), panelW() - 16), tx, ty,
                    ThemeRenderer.color("qrm_boss.cream_soft"));
        }
    }

    private void drawAccount(GuiGraphicsExtractor g) {
        ThemeRenderer.draw(g, "qrm_boss.balance_plate", cx, top, cw, PLATE_H);
        String text = snapshot.hasBalance() ? snapshot.balance() : tr("qrm_boss.account.hidden");
        g.centeredText(this.font, text, cx + cw / 2, top + (PLATE_H - lineH()) / 2 + 1,
                ThemeRenderer.color(snapshot.hasBalance() ? "qrm_boss.gold" : "qrm_boss.cream_soft"));
        g.text(this.font, tr("qrm_boss.account.amount"), cx + 2, top + PLATE_H + 8, ThemeRenderer.color("qrm_boss.ink"),
                false);
    }
}
