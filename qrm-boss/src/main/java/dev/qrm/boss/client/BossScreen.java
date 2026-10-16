package dev.qrm.boss.client;

import dev.qrm.boss.logic.BossAction;
import dev.qrm.boss.logic.BossPerks;
import dev.qrm.boss.logic.BossReply;
import dev.qrm.boss.logic.BossRequest;
import dev.qrm.boss.logic.BossResult;
import dev.qrm.boss.logic.BossSnapshot;
import dev.qrm.boss.net.BossActionPayload;
import dev.qrm.client.api.screen.QrmScreen;
import dev.qrm.client.api.theme.ThemeRenderer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Menu del boss aziendale: Panoramica (con la bacheca) e Membri (ruolo, titolo e bonus). Mostra l'ultimo snapshot del
 * server e manda richieste: non applica mai modifiche in locale. Lo stato vive nei campi, così
 * {@link #rebuildWidgets()} ricostruisce tutto.
 */
final class BossScreen extends QrmScreen {
    enum Section { OVERVIEW, MEMBERS }

    /** I blocchi di azioni della scheda di un dipendente. */
    enum Block {
        RANK("qrm_boss.mode.rank"), TITLE("qrm_boss.mode.title"), BONUS("qrm_boss.mode.bonus");

        private final String key;
        Block(String key) {
            this.key = key;
        }
    }

    private static final int FRAME_MAX_W = 316;
    private static final int FRAME_MAX_H = 260;
    private static final int MARGIN = 10;
    private static final int TITLE_H = 20;
    private static final int ROW_H = 20;
    private static final int BUTTON_H = 16;
    private static final int GAP = 4;
    private static final int PROFILE_H = 34;
    private static final int BLOCK_H = 94;
    private static final int CHIP_H = 14;
    private static final int KPI_H = 36;
    private static final int STATUS_H = 14;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());

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
    private String noticeText = "";
    private String titleText = "";
    private String bonusAmount = "";
    private String bonusReason = "";
    private BossAction lastAction = BossAction.REFRESH;
    private long feedbackAt;
    private boolean feedbackOk;

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
            if (result == BossResult.OK) {
                switch (lastAction) {
                    case SET_NOTICE -> noticeText = "";
                    case SET_TITLE -> titleText = "";
                    case GIVE_BONUS -> {
                        bonusAmount = "";
                        bonusReason = "";
                    }
                    case ADD_MEMBER -> addMode = false;
                    default -> {}
                }
            }
        } else if (result.closesScreen()) {
            snapshot = null;
            closed = true;
        }
        if (!(result == BossResult.OK && lastAction == BossAction.REFRESH)) {
            status = Component.translatable(result.translationKey());
            statusColor = ThemeRenderer.color(result == BossResult.OK ? "qrm_boss.positive" : "qrm_boss.negative");
            feedbackAt = System.currentTimeMillis();
            feedbackOk = result == BossResult.OK;
        }
        lastAction = BossAction.REFRESH;
        // Nel costruttore la schermata non è ancora inizializzata (width 0): ricostruire adesso lascerebbe widget
        // fantasma, perché init() non svuota la lista. La prima init() la fa Minecraft.
        if (this.width > 0 && this.height > 0) rebuildWidgets();
    }

    private boolean available(Section s) {
        return s == Section.OVERVIEW || snapshot.showMembers();
    }

    private void send(BossAction action, Optional<UUID> target, String rankId, String amount, String text) {
        if (snapshot == null) return;
        lastAction = action;
        ClientPacketDistributor.sendToServer(new BossActionPayload(new BossRequest(action, snapshot.orgId(),
                snapshot.character(), target, rankId, amount, text, UUID.randomUUID())));
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

    // ---- layout ----------------------------------------------------------------------------------------

    @Override
    protected void init() {
        fw = Math.min(FRAME_MAX_W, this.width - 20);
        fh = Math.max(170, Math.min(FRAME_MAX_H, this.height - 20));
        x0 = Math.max(10, (this.width - fw) / 2);
        y0 = Math.max(4, (this.height - fh) / 2);
        cx = x0 + MARGIN;
        cw = fw - 2 * MARGIN;
        top = y0 + 8 + TITLE_H + 8;
        statusY = y0 + fh - MARGIN - STATUS_H;
        bottom = statusY - 6;
        if (snapshot == null) return;

        // schede nella barra del titolo, a destra
        List<Section> sections = new ArrayList<>(List.of(Section.OVERVIEW));
        if (snapshot.showMembers()) sections.add(Section.MEMBERS);
        int total = 0;
        for (Section sec : sections) total += BossButton.tabWidth(tabLabel(sec));
        int tx = x0 + fw - MARGIN - 4 - total;
        for (Section sec : sections) {
            BossButton tab = addRenderableWidget(BossButton.tab(tx, y0 + 8, TITLE_H,
                    Component.literal(tabLabel(sec)), section == sec, () -> setSection(sec)));
            tx += tab.getWidth();
        }

        switch (section) {
            case OVERVIEW -> initOverview();
            case MEMBERS -> initMembers();
        }
    }

    private static String tabLabel(Section s) {
        return tr(s == Section.OVERVIEW ? "qrm_boss.section.overview" : "qrm_boss.section.members");
    }

    // ---- sezione Panoramica ------------------------------------------------------------------------------

    private int noticeTop() {
        return top + KPI_H + 8;
    }

    private void initOverview() {
        if (!snapshot.canNotice()) return;
        int by = bottom - BUTTON_H - 8;
        int fy = by - BUTTON_H - GAP;
        BossField field = addRenderableWidget(new BossField(this.font, cx + 8, fy, cw - 16, BUTTON_H,
                Component.translatable("qrm_boss.notice.hint")));
        field.setMaxLength(BossPerks.MAX_NOTICE);
        field.setHint(Component.translatable("qrm_boss.notice.hint"));
        field.setValue(noticeText);
        field.setResponder(t -> noticeText = t);
        int bw = (cw - 16 - GAP) / 2;
        BossButton publish = addRenderableWidget(BossButton.button(cx + 8, by, bw, BUTTON_H,
                Component.translatable("qrm_boss.notice.publish"), "qrm_boss.icon_add", BossButton.Style.CONFIRM,
                () -> send(BossAction.SET_NOTICE, Optional.empty(), "", "", noticeText)));
        publish.active = !noticeText.isBlank();
        BossButton clear = addRenderableWidget(BossButton.button(cx + 8 + bw + GAP, by, cw - 16 - bw - GAP, BUTTON_H,
                Component.translatable("qrm_boss.notice.clear"), "qrm_boss.icon_fire", BossButton.Style.DANGER,
                () -> send(BossAction.SET_NOTICE, Optional.empty(), "", "", "")));
        clear.active = !snapshot.notice().isEmpty();
    }

    // ---- sezione Membri ----------------------------------------------------------------------------------

    private int listTop() {
        return top + 20;
    }

    /** Sotto l'elenco dei candidati c'è la barra con il pulsante per aggiungere. */
    private int footer() {
        return addMode ? BUTTON_H + GAP + 2 : 0;
    }

    private int rows() {
        return Math.max(1, (bottom - listTop() - 4 - footer()) / ROW_H);
    }

    private BossSnapshot.Member selected() {
        return selectedMember == null ? null : snapshot.members().stream()
                .filter(x -> x.character().equals(selectedMember)).findFirst().orElse(null);
    }

    private List<Block> blocksOf(BossSnapshot.Member m) {
        List<Block> blocks = new ArrayList<>();
        if (m == null) return blocks;
        if (m.editable()) blocks.add(Block.RANK);
        if (m.canTitle()) blocks.add(Block.TITLE);
        if (m.canBonus()) blocks.add(Block.BONUS);
        return blocks;
    }

    private void initMembers() {
        BossSnapshot.Member m = addMode ? null : selected();
        if (m != null) {
            initSheet(m);
            return;
        }
        BossButton toggleButton = addRenderableWidget(BossButton.button(cx + cw - 84, top, 84, BUTTON_H,
                Component.literal(tr(addMode ? "qrm_boss.members.back" : "qrm_boss.members.add")), "qrm_boss.icon_add",
                BossButton.Style.DEFAULT, () -> {
                    addMode = !addMode;
                    scroll = 0;
                    fireArmed = false;
                    rebuildWidgets();
                }));
        toggleButton.active = snapshot.canManage() && snapshot.level() >= 0;
        toggleButton.visible = snapshot.canManage();

        List<?> items = addMode ? snapshot.candidates() : snapshot.members();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, items.size() - rows())));
        for (int i = 0; i < rows() && scroll + i < items.size(); i++) {
            int ry = listTop() + 2 + i * ROW_H;
            if (addMode) {
                BossSnapshot.Candidate c = snapshot.candidates().get(scroll + i);
                addRenderableWidget(new BossRow(cx + 2, ry, cw - 10, ROW_H, c.name(), "", "", null, true,
                        c.character().equals(selectedCandidate), () -> {
                            selectedCandidate = c.character();
                            rebuildWidgets();
                        }));
            } else {
                BossSnapshot.Member row = snapshot.members().get(scroll + i);
                addRenderableWidget(new BossRow(cx + 2, ry, cw - 10, ROW_H, row.name(),
                        row.title().isEmpty() ? presenceLabel(row.presence()) : row.title(), row.rankLabel(),
                        row.player(), row.active(), false, () -> {
                            selectedMember = row.character();
                            fireArmed = false;
                            rebuildWidgets();
                        }));
            }
        }
        if (addMode) {
            String lowest = snapshot.ranks().isEmpty() ? "" : snapshot.ranks().get(0).label();
            BossButton add = addRenderableWidget(BossButton.button(cx, bottom - BUTTON_H, cw, BUTTON_H,
                    Component.translatable("qrm_boss.members.add_confirm", lowest), "qrm_boss.icon_add",
                    BossButton.Style.CONFIRM,
                    () -> send(BossAction.ADD_MEMBER, Optional.of(selectedCandidate), "", "", "")));
            add.active = snapshot.canManage() && selectedCandidate != null && !snapshot.ranks().isEmpty();
        }
    }

    private int blockTop() {
        return listTop() + PROFILE_H + GAP;
    }

    private int blockH() {
        return Math.min(BLOCK_H, bottom - blockTop());
    }

    private int logTop() {
        return blockTop() + blockH() + GAP;
    }

    private boolean showLog() {
        return bottom - logTop() >= 30;
    }

    private String presenceLabel(int presence) {
        return tr(switch (presence) {
            case 3 -> "qrm_boss.presence.duty";
            case 2 -> "qrm_boss.presence.online";
            case 1 -> "qrm_boss.presence.offduty";
            default -> "qrm_boss.presence.offline";
        });
    }

    private int blockW(int n) {
        return (cw - (n - 1) * GAP) / n;
    }

    /** La scheda del dipendente: un pulsante per tornare all'elenco e un blocco per ogni cosa che si può fare. */
    private void initSheet(BossSnapshot.Member m) {
        addRenderableWidget(BossButton.button(cx + cw - 84, top, 84, BUTTON_H, Component.translatable("qrm_boss.members.back"),
                null, BossButton.Style.DEFAULT, () -> {
                    selectedMember = null;
                    fireArmed = false;
                    rebuildWidgets();
                }));
        List<Block> blocks = blocksOf(m);
        int n = blocks.size();
        if (n == 0) return;
        int bw = blockW(n);
        for (int i = 0; i < n; i++) {
            int bx = cx + i * (bw + GAP) + 6;
            int by = blockTop() + 18;
            int w = bw - 12;
            switch (blocks.get(i)) {
                case RANK -> initRank(m, bx, by, w);
                case TITLE -> initTitle(m, bx, by, w);
                case BONUS -> initBonus(bx, by, w);
            }
        }
    }

    private void initRank(BossSnapshot.Member m, int bx, int cy, int bw) {
        BossSnapshot.RankOption up = snapshot.ranks().stream().filter(r -> r.level() > m.level()).findFirst().orElse(null);
        BossSnapshot.RankOption down = lastBelow(m.level());
        BossButton promote = addRenderableWidget(BossButton.button(bx, cy, bw, BUTTON_H,
                Component.translatable("qrm_boss.members.promote"), "qrm_boss.icon_promote", BossButton.Style.DEFAULT,
                () -> send(BossAction.SET_RANK, Optional.of(selectedMember), up.id(), "", "")));
        promote.active = m.editable() && up != null;
        BossButton demote = addRenderableWidget(BossButton.button(bx, cy + BUTTON_H + GAP, bw, BUTTON_H,
                Component.translatable("qrm_boss.members.demote"), null, BossButton.Style.DEFAULT,
                () -> send(BossAction.SET_RANK, Optional.of(selectedMember), down.id(), "", "")));
        demote.active = m.editable() && down != null;
        BossButton fire = addRenderableWidget(BossButton.button(bx, cy + 2 * (BUTTON_H + GAP), bw, BUTTON_H,
                Component.translatable(fireArmed ? "qrm_boss.members.fire_confirm" : "qrm_boss.members.fire"),
                "qrm_boss.icon_fire", BossButton.Style.DANGER, () -> {
                    if (!fireArmed) {
                        fireArmed = true;
                        rebuildWidgets();
                    } else {
                        fireArmed = false;
                        send(BossAction.REMOVE_MEMBER, Optional.of(selectedMember), "", "", "");
                    }
                }));
        fire.active = m.editable();
    }

    private void initTitle(BossSnapshot.Member m, int bx, int cy, int bw) {
        BossField field = addRenderableWidget(new BossField(this.font, bx, cy, bw, BUTTON_H,
                Component.translatable("qrm_boss.title.hint")));
        field.setMaxLength(BossPerks.MAX_TITLE);
        field.setHint(Component.translatable("qrm_boss.title.hint"));
        field.setValue(titleText);
        field.setResponder(t -> titleText = t);
        BossButton assign = addRenderableWidget(BossButton.button(bx, cy + BUTTON_H + GAP, bw, BUTTON_H,
                Component.translatable("qrm_boss.title.assign"), "qrm_boss.icon_promote", BossButton.Style.CONFIRM,
                () -> send(BossAction.SET_TITLE, Optional.of(selectedMember), "", "", titleText)));
        assign.active = !titleText.isBlank();
        BossButton remove = addRenderableWidget(BossButton.button(bx, cy + 2 * (BUTTON_H + GAP), bw, BUTTON_H,
                Component.translatable("qrm_boss.title.remove"), null, BossButton.Style.DEFAULT,
                () -> send(BossAction.SET_TITLE, Optional.of(selectedMember), "", "", "")));
        remove.active = !m.title().isEmpty();
    }

    private static final String[] BONUS_CHIPS = {"100", "250", "500"};

    private void initBonus(int bx, int cy, int bw) {
        int chipW = (bw - 2 * 3) / BONUS_CHIPS.length;
        for (int i = 0; i < BONUS_CHIPS.length; i++) {
            String chip = BONUS_CHIPS[i];
            addRenderableWidget(BossButton.button(bx + i * (chipW + 3), cy, chipW, CHIP_H, Component.literal(chip), null,
                    BossButton.Style.DEFAULT, () -> {
                        bonusAmount = chip;
                        rebuildWidgets();
                    }).selected(chip.equals(bonusAmount.strip())));
        }
        int fy = cy + CHIP_H + GAP;
        BossField amount = addRenderableWidget(new BossField(this.font, bx, fy, bw, BUTTON_H,
                Component.translatable("qrm_boss.bonus.amount")));
        amount.setMaxLength(BossRequest.MAX_AMOUNT);
        amount.setHint(Component.translatable("qrm_boss.bonus.amount"));
        amount.setValue(bonusAmount);
        amount.setResponder(t -> bonusAmount = t);
        BossField reason = addRenderableWidget(new BossField(this.font, bx, fy + BUTTON_H + GAP, bw, BUTTON_H,
                Component.translatable("qrm_boss.bonus.reason")));
        reason.setMaxLength(BossPerks.MAX_REASON);
        reason.setHint(Component.translatable("qrm_boss.bonus.reason"));
        reason.setValue(bonusReason);
        reason.setResponder(t -> bonusReason = t);
        BossButton give = addRenderableWidget(BossButton.button(bx, fy + 2 * (BUTTON_H + GAP), bw, BUTTON_H,
                Component.translatable("qrm_boss.bonus.give"), "qrm_boss.icon_deposit", BossButton.Style.CONFIRM,
                () -> send(BossAction.GIVE_BONUS, Optional.of(selectedMember), "", bonusAmount.strip(), bonusReason)));
        give.active = !bonusAmount.isBlank();
    }

    private BossSnapshot.RankOption lastBelow(int level) {
        BossSnapshot.RankOption found = null;
        for (BossSnapshot.RankOption r : snapshot.ranks()) if (r.level() < level) found = r;
        return found;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (snapshot != null && section == Section.MEMBERS && (addMode || selectedMember == null) && scrollY != 0) {
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
        BossStyle.box(g, x0, y0, fw, fh, BossStyle.LINE, BossStyle.BG);
        BossStyle.panel(g, x0 + MARGIN, y0 + 8, fw - 2 * MARGIN, TITLE_H);
        if (snapshot == null) {
            BossStyle.centered(g, this.font, BossStyle.fit(this.font, status.getString(), cw), this.width / 2,
                    y0 + fh / 2, statusColor);
            return;
        }
        int tabsW = 0;
        for (Section sec : Section.values()) if (available(sec)) tabsW += BossButton.tabWidth(tabLabel(sec));
        BossStyle.text(g, this.font, BossStyle.fit(this.font, snapshot.orgLabel(), fw - 2 * MARGIN - 16 - tabsW),
                x0 + MARGIN + 8, y0 + 8 + (TITLE_H - BossStyle.TEXT_H) / 2 + 1, BossStyle.color("qrm_boss.title"));
        switch (section) {
            case OVERVIEW -> drawOverview(g);
            case MEMBERS -> drawMembers(g);
        }
        drawStatus(g);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    /** Barra di stato in fondo, come quella dell'ATM: un lampo verde se l'azione riesce, rosso e un tremolio se no. */
    private void drawStatus(GuiGraphicsExtractor g) {
        BossStyle.panel(g, cx, statusY, cw, STATUS_H);
        long age = System.currentTimeMillis() - feedbackAt;
        int dx = 0;
        if (feedbackAt > 0 && age < 700) {
            int a = (int) ((1 - age / 700f) * 0x50);
            g.fill(cx + 2, statusY + 2, cx + cw - 2, statusY + STATUS_H - 2,
                    (a << 24) | (feedbackOk ? 0x3CE07A : 0x5A0F0F));
            if (!feedbackOk && age < 300) dx = (int) Math.round(Math.sin(age / 22.0) * 2);
        }
        BossStyle.text(g, this.font, BossStyle.fit(this.font, status.getString(), cw - 12), cx + 6 + dx,
                statusY + (STATUS_H - BossStyle.TEXT_H) / 2 + 1, statusColor);
    }

    private void label(GuiGraphicsExtractor g, String text, int x, int y) {
        BossStyle.text(g, this.font, text, x, y, BossStyle.color("qrm_boss.label"));
    }

    private void kpi(GuiGraphicsExtractor g, int x, int w, String label, String value, int color) {
        BossStyle.panel(g, x, top, w, KPI_H);
        label(g, BossStyle.fit(this.font, label, w - 12), x + 6, top + 7);
        BossStyle.text(g, this.font, BossStyle.fit(this.font, value, w - 12), x + 6, top + 21, color);
    }

    private void drawOverview(GuiGraphicsExtractor g) {
        List<String[]> kpis = new ArrayList<>();
        int gold = BossStyle.color("qrm_boss.gold");
        int live = BossStyle.color("qrm_boss.positive");
        kpis.add(new String[] {tr("qrm_boss.overview.members"), Integer.toString(snapshot.memberCount()), "g"});
        kpis.add(new String[] {tr("qrm_boss.overview.online"), Integer.toString(snapshot.online()), "l"});
        if (snapshot.onDuty() >= 0) {
            kpis.add(new String[] {tr("qrm_boss.overview.duty"), Integer.toString(snapshot.onDuty()), "l"});
        }
        kpis.add(new String[] {tr("qrm_boss.overview.rank"),
                snapshot.level() < 0 ? tr("qrm_boss.overview.rank_job") : snapshot.rankLabel(), "g"});
        int n = kpis.size();
        int w = (cw - (n - 1) * GAP) / n;
        for (int i = 0; i < n; i++) {
            String[] k = kpis.get(i);
            boolean lit = k[2].equals("l") && !k[1].equals("0");
            kpi(g, cx + i * (w + GAP), i == n - 1 ? cw - i * (w + GAP) : w, k[0], k[1], lit ? live : gold);
        }

        int ny = noticeTop();
        int nh = bottom - ny;
        BossStyle.panel(g, cx, ny, cw, nh);
        g.fill(cx, ny, cx + 2, ny + nh, BossStyle.GOLD);
        BossStyle.text(g, this.font, tr("qrm_boss.notice.title"), cx + 8, ny + 7, BossStyle.color("qrm_boss.cream"));
        if (snapshot.notice().isEmpty()) {
            String invite = tr(snapshot.canNotice() ? "qrm_boss.notice.empty_boss" : "qrm_boss.notice.empty");
            int y = ny + 22;
            for (String line : BossStyle.wrap(this.font, invite, cw - 20)) {
                label(g, line, cx + 8, y);
                y += BossStyle.TEXT_H + 3;
            }
            return;
        }
        String who = snapshot.noticeAuthor();
        if (snapshot.noticeAt() > 0) {
            String date = DATE.format(Instant.ofEpochMilli(snapshot.noticeAt()));
            who = who.isEmpty() ? date : who + " · " + date;
        }
        BossStyle.right(g, this.font, BossStyle.fit(this.font, who, cw / 2), cx + cw - 6, ny + 7, gold);
        int textBottom = ny + nh - (snapshot.canNotice() ? 2 * BUTTON_H + GAP + 14 : 6);
        int y = ny + 22;
        for (String line : BossStyle.wrap(this.font, snapshot.notice(), cw - 20)) {
            if (y + BossStyle.TEXT_H > textBottom) break;
            BossStyle.text(g, this.font, line, cx + 8, y, BossStyle.color("qrm_boss.ink"));
            y += BossStyle.TEXT_H + 3;
        }
    }

    private void drawMembers(GuiGraphicsExtractor g) {
        BossSnapshot.Member m = addMode ? null : selected();
        if (m != null) {
            drawSheet(g, m);
            return;
        }
        String header = addMode ? tr("qrm_boss.members.candidates")
                : Component.translatable("qrm_boss.members.header", snapshot.memberCount()).getString();
        BossStyle.text(g, this.font, header, cx + 2, top + (BUTTON_H - BossStyle.TEXT_H) / 2 + 1,
                BossStyle.color("qrm_boss.cream"));
        int h = rows() * ROW_H + 4;
        BossStyle.panel(g, cx, listTop(), cw, h);

        List<?> items = addMode ? snapshot.candidates() : snapshot.members();
        if (items.isEmpty()) {
            label(g, BossStyle.fit(this.font, tr(addMode ? "qrm_boss.members.no_candidates"
                    : "qrm_boss.members.empty"), cw - 16), cx + 6, listTop() + 8);
        } else if (items.size() > rows()) {
            int trackH = h - 8;
            int thumbH = Math.max(8, trackH * rows() / items.size());
            int range = items.size() - rows();
            int thumbY = listTop() + 4 + (trackH - thumbH) * Math.min(scroll, range) / range;
            g.fill(cx + cw - 5, listTop() + 4, cx + cw - 2, listTop() + 4 + trackH, 0x33FFFFFF);
            g.fill(cx + cw - 5, thumbY, cx + cw - 2, thumbY + thumbH, 0xFFB88420);
        }
    }

    /** La scheda del dipendente: profilo con la testa, poi un riquadro per ogni azione e in fondo il fascicolo. */
    private void drawSheet(GuiGraphicsExtractor g, BossSnapshot.Member m) {
        BossStyle.text(g, this.font, tr("qrm_boss.members.sheet"), cx + 2, top + (BUTTON_H - BossStyle.TEXT_H) / 2 + 1,
                BossStyle.color("qrm_boss.cream"));
        BossStyle.panel(g, cx, listTop(), cw, PROFILE_H);
        BossStyle.face(g, m.player(), m.active(), cx + 6, listTop() + 5, 24);
        int tx = cx + 38;
        String presence = presenceLabel(m.presence());
        int presenceW = BossStyle.width(this.font, presence);
        BossStyle.right(g, this.font, presence, cx + cw - 8, listTop() + 7,
                BossStyle.color(m.active() ? "qrm_boss.positive" : "qrm_boss.label"));
        BossStyle.text(g, this.font, BossStyle.fit(this.font, m.name(), cw - 52 - presenceW), tx, listTop() + 7,
                BossStyle.color("qrm_boss.ink"));
        String line = m.title().isEmpty() ? m.rankLabel() : m.rankLabel() + " · " + m.title();
        BossStyle.text(g, this.font, BossStyle.fit(this.font, line, cw - 52), tx, listTop() + 19,
                BossStyle.color("qrm_boss.ink_soft"));

        List<Block> blocks = blocksOf(m);
        if (blocks.isEmpty()) {
            label(g, BossStyle.fit(this.font, tr("qrm_boss.members.read_only"), cw - 12), cx + 6, blockTop() + 8);
        } else {
            int bw = blockW(blocks.size());
            for (int i = 0; i < blocks.size(); i++) {
                int bx = cx + i * (bw + GAP);
                BossStyle.panel(g, bx, blockTop(), bw, blockH());
                BossStyle.text(g, this.font, tr(blocks.get(i).key), bx + 6, blockTop() + 7,
                        BossStyle.color("qrm_boss.title"));
            }
        }

        if (!showLog()) return;
        BossStyle.panel(g, cx, logTop(), cw, bottom - logTop());
        BossStyle.text(g, this.font, tr("qrm_boss.log.title"), cx + 6, logTop() + 6, BossStyle.color("qrm_boss.cream"));
        if (m.log().isEmpty()) {
            label(g, tr("qrm_boss.log.empty"), cx + 6, logTop() + 18);
            return;
        }
        int y = logTop() + 17;
        for (BossSnapshot.LogEntry e : m.log()) {
            if (y + BossStyle.TEXT_H > bottom - 3) break;
            String date = DATE.format(Instant.ofEpochMilli(e.at()));
            String what = e.kind() == BossSnapshot.LogEntry.BONUS
                    ? tr("qrm_boss.log.bonus") + " " + e.text()
                    : e.text().isEmpty() ? tr("qrm_boss.log.title_removed") : tr("qrm_boss.log.title_set") + " " + e.text();
            BossStyle.text(g, this.font, date, cx + 6, y, BossStyle.color("qrm_boss.label"));
            BossStyle.text(g, this.font, BossStyle.fit(this.font, what, cw - 70), cx + 56, y,
                    BossStyle.color(e.kind() == BossSnapshot.LogEntry.BONUS ? "qrm_boss.gold" : "qrm_boss.ink"));
            y += BossStyle.TEXT_H + 3;
        }
    }
}
