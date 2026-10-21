package dev.qrm.police.weapon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.qrm.police.mc.PoliceItems;
import dev.qrm.police.weapon.logic.CommitPoint;
import dev.qrm.police.weapon.logic.Hint;
import dev.qrm.police.weapon.logic.HintPresenter;
import dev.qrm.police.weapon.logic.MagazineState;
import dev.qrm.police.weapon.logic.PistolAction;
import dev.qrm.police.weapon.logic.PistolRules;
import dev.qrm.police.weapon.logic.WeaponState;
import dev.qrm.police.weapon.logic.anim.AnimEvent;
import dev.qrm.police.weapon.mc.WeaponComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Suggerimenti scritti accanto alla pistola in prima persona (nessun livello dell'HUD) e risultato del controllo
 * munizioni. Cosa scrivere e con che opacità lo decide {@link HintPresenter}; qui si raccolgono gli ingressi e si
 * disegna.
 *
 * <p>Ingressi:
 * <ul>
 *   <li>stato: la copia del client del {@link WeaponState} della pistola in mano e, per {@code hasLoadedSpare}, una
 *       scansione dell'inventario principale del giocatore locale (caricatore con almeno un colpo), come fa il server
 *       per scegliere il caricatore di scorta;</li>
 *   <li>clic a vuoto: {@link PistolInput} chiama {@link #onDryFire()} quando la previsione dello sparo avvia
 *       {@code dry_fire}. Scelto al posto del suono {@code pistol.dry_fire} perché i suoni delle clip non passano da
 *       {@link PistolClientEvents} (li suona direttamente l'animatore) e perché così non dipende dalla clip;</li>
 *   <li>controllo munizioni: l'evento {@code commit check} della clip {@code ammo_check}. Finché quella clip non c'è
 *       (Parte F) il commit non arriva mai: allora vale la fine non annullata dell'azione {@code AMMO_CHECK}
 *       annunciata dal server ({@link #onActionCompleted});</li>
 *   <li>azione che blocca: la clip d'azione in corso o l'azione annunciata dal server (ricariche, carrello), così i
 *       suggerimenti si nascondono anche finché le clip del carrello non sono esportate.</li>
 * </ul>
 *
 * <p>Disegno: ancorato al perno del bone {@code slide}, spostato a destra nello spazio della vista, rivolto alla
 * camera (solo traslazione, nessuna rotazione), piccolo, a piena luce, con ombra e uno sfondo tenue.
 */
public final class PistolHints {
    private static final String SLIDE = "slide";
    private static final String AMMO_CHECK_CLIP = PistolAction.AMMO_CHECK.clip();

    /** Scostamento dal perno del carrello nello spazio della vista, in blocchi (x destra, y su, z verso la camera). */
    private static final float OFFSET_X = 0.06f;
    private static final float OFFSET_Y = 0.02f;
    private static final float OFFSET_Z = 0.02f;
    /** Blocchi per pixel del font: 9 pixel ≈ 0,018 blocchi, a circa 0,9 blocchi dalla camera. */
    private static final float TEXT_SCALE = 0.002f;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int TEXT_RGB = 0xFFFFFF;
    /** Opacità massima dello sfondo dietro al testo. */
    private static final float BACKGROUND_ALPHA = 0.25f;
    /** Sotto questa opacità non si disegna: lo shader del testo scarta comunque i pixel quasi trasparenti. */
    private static final float MIN_ALPHA = 0.05f;

    private static final HintPresenter PRESENTER = new HintPresenter();
    private static boolean listening;

    private PistolHints() {}

    /** Da chiamare una volta all'avvio del client: ascolta i commit delle clip. */
    public static void init() {
        if (listening) return;
        listening = true;
        PistolClientEvents.addListener(PistolHints::onAnimEvent);
    }

    // ------------------------------------------------------------------ ingressi

    /** Da registrare sul bus di NeoForge, dopo {@link PistolClientTick#onClientTick}. */
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        ItemStack main = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        WeaponState state = main.is(PoliceItems.PISTOL.get()) ? main.get(WeaponComponents.PISTOL_STATE.get()) : null;
        if (state == null) {
            PRESENTER.tick(Hint.NONE, false, false);
            return;
        }
        boolean blocking = HintPresenter.blocksClip(PistolAnimator.local().actionClip())
                || HintPresenter.blocks(PistolInput.runningAction());
        PRESENTER.tick(PistolRules.hint(state, hasLoadedSpare(player)), true, blocking);
    }

    /** Clic a vuoto previsto dal client. */
    static void onDryFire() {
        PRESENTER.dryFire();
    }

    /** L'azione temporizzata del giocatore locale è finita senza essere annullata. */
    static void onActionCompleted(@Nullable PistolAction action) {
        if (action == PistolAction.AMMO_CHECK && !PistolAssets.clips().containsKey(AMMO_CHECK_CLIP)) showCheck();
    }

    /** Uscita dal mondo o rinascita: niente di residuo. */
    public static void reset() {
        PRESENTER.tick(Hint.NONE, false, false);
    }

    private static void onAnimEvent(AnimEvent e) {
        if ("commit".equals(e.type()) && !e.args().isEmpty() && CommitPoint.CHECK.arg().equals(e.args().get(0))) {
            showCheck();
        }
    }

    private static void showCheck() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack main = player.getMainHandItem();
        if (!main.is(PoliceItems.PISTOL.get())) return;
        WeaponState state = main.get(WeaponComponents.PISTOL_STATE.get());
        if (state == null) return;
        PRESENTER.ammoCheck(state.magazine().orElse(null), state.chambered());
    }

    private static boolean hasLoadedSpare(LocalPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.is(PoliceItems.MAGAZINE.get())) continue;
            MagazineState mag = stack.get(WeaponComponents.MAGAZINE.get());
            if (mag != null && mag.rounds() > 0) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ disegno

    /**
     * Disegna la riga corrente nella passata della prima persona.
     *
     * @param modelPose la PoseStack già portata nello spazio del modello della pistola
     * @param slidePivot cornice del perno del bone {@code slide} nello spazio del modello, o {@code null} se manca
     */
    static void render(PoseStack modelPose, @Nullable Matrix4f slidePivot, SubmitNodeCollector collector,
            float partialTick) {
        HintPresenter.Line line = PRESENTER.line();
        if (line == null || slidePivot == null) return;
        float alpha = PRESENTER.alpha(partialTick);
        if (alpha < MIN_ALPHA) return;

        Vector3f anchor = new Matrix4f(modelPose.last().pose()).mul(slidePivot).transformPosition(new Vector3f());
        Font font = Minecraft.getInstance().font;
        Component text = text(line);

        PoseStack pose = new PoseStack();
        pose.translate(anchor.x + OFFSET_X, anchor.y + OFFSET_Y, anchor.z + OFFSET_Z);
        // Come le targhette dei nomi: nello spazio della vista basta capovolgere Y (il font scrive verso il basso).
        pose.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        int a = Math.round(alpha * 255f);
        int background = Math.round(alpha * BACKGROUND_ALPHA * 255f) << 24;
        collector.submitText(pose, 0f, -font.lineHeight / 2f, text.getVisualOrderText(), true,
                Font.DisplayMode.NORMAL, FULL_BRIGHT, (a << 24) | TEXT_RGB, background, 0);
    }

    private static Component text(HintPresenter.Line line) {
        List<Object> args = new ArrayList<>();
        switch (line.binding()) {
            case INTERACT -> args.add(PistolKeys.INTERACT.getTranslatedKeyMessage());
            case RELOAD -> args.add(PistolKeys.RELOAD.getTranslatedKeyMessage());
            case NONE -> {}
        }
        for (String key : line.args()) args.add(Component.translatable(key));
        return Component.translatable(line.key(), args.toArray());
    }
}
