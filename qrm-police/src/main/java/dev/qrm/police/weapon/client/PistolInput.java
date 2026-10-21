package dev.qrm.police.weapon.client;

import dev.qrm.police.mc.PoliceItems;
import dev.qrm.police.weapon.logic.PistolAction;
import dev.qrm.police.weapon.logic.PistolTimings;
import dev.qrm.police.weapon.logic.WeaponState;
import dev.qrm.police.weapon.logic.input.EdgeLatch;
import dev.qrm.police.weapon.logic.input.FireGate;
import dev.qrm.police.weapon.logic.input.PressClassifier;
import dev.qrm.police.weapon.mc.WeaponComponents;
import dev.qrm.police.weapon.net.PistolPayloads;
import dev.qrm.police.weapon.net.PistolPayloads.Input.Kind;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Comandi della pistola del giocatore locale. Il client manda solo intenzioni ({@link PistolPayloads.Input}); il
 * server decide munizioni, colpi e danno.
 *
 * <ul>
 *   <li><b>Sparo</b> ("attacca"): a ogni pressione, se {@link FireGate} lo consente (5 tick, nessuna azione che blocca
 *       lo sparo), avvia subito la clip prevista ({@code fire}, {@code fire_last} o {@code dry_fire}) e manda
 *       {@code FIRE}. Le pressioni si contano svuotando {@code keyAttack.consumeClick()} all'inizio del tick, prima
 *       che lo faccia vanilla: così ogni clic vale una volta sola (semiautomatica) e vanilla non avvia né fendente né
 *       rottura del blocco. {@link InputEvent.InteractionKeyMappingTriggered} di "attacca" viene comunque annullato,
 *       senza oscillazione della mano: vanilla lo emette a ogni tick tenendo premuto su un blocco.</li>
 *   <li><b>Mira</b> ("usa"): {@code keyUse.isDown()} letto a ogni tick; i cambi mandano {@code AIM_ON}/{@code AIM_OFF}
 *       e comandano l'animatore. Gli eventi "usa" con la pistola in mano vengono annullati, così non succede
 *       nient'altro (porte, oggetti nell'altra mano).</li>
 *   <li><b>Ricarica</b> ({@link PistolKeys#RELOAD}): {@code RELOAD}.</li>
 *   <li><b>Interazione</b> ({@link PistolKeys#INTERACT}): rilasciato prima di
 *       {@link PistolTimings#HOLD_THRESHOLD_TICKS} tick → {@code INTERACT_TAP}; tenuto fino alla soglia →
 *       {@code INTERACT_HOLD}, una volta. Con un caricatore in mano vale solo la pressione breve (ispezione).</li>
 * </ul>
 *
 * <p>Scelta sul clic a vuoto: anche quando la previsione dice {@code dry_fire} si manda {@code FIRE}. Il server non
 * spara né risponde se la camera è davvero vuota, ma interrompe un controllo o un'ispezione in corso come per uno
 * sparo vero (il client fa lo stesso) e, se lo stack del client era indietro (per esempio una ricarica appena
 * confermata), il colpo parte comunque dove conta.
 */
public final class PistolInput {
    private static final UUID NO_ID = new UUID(0L, 0L);
    private static final FireGate GATE = new FireGate();
    private static final PressClassifier INTERACT_PRESS = new PressClassifier(PistolTimings.HOLD_THRESHOLD_TICKS);
    private static final EdgeLatch AIM = new EdgeLatch();

    /** Contatore di tick del client, solo crescente. */
    private static long now;
    /** Cosa c'era in mano al tick precedente: 0 niente di utile, 1 pistola, 2 caricatore. */
    private static int lastHeld;

    private PistolInput() {}

    /** Vero se il giocatore locale sta mirando con la pistola. */
    public static boolean isLocalAiming() {
        return AIM.value();
    }

    // ------------------------------------------------------------------ eventi di NeoForge

    /** Da registrare sul bus di NeoForge: legge i tasti prima che lo faccia vanilla. */
    public static void onClientTick(ClientTickEvent.Pre event) {
        now++;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ItemStack main = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        boolean pistol = main.is(PoliceItems.PISTOL.get());
        boolean magazine = !pistol && main.is(PoliceItems.MAGAZINE.get());
        boolean inWorld = player != null && mc.gui.screen() == null && !player.isSpectator();

        int held = pistol ? 1 : magazine ? 2 : 0;
        if (held != lastHeld) {
            // La pressione di V vale per l'oggetto che c'era quando è iniziata: cambiato oggetto, annullata.
            INTERACT_PRESS.cancel();
            if (lastHeld == 1) {
                // Pistola riposta: il server annulla l'azione in corso; l'intervallo tra i colpi invece resta.
                GATE.actionEnded();
                GATE.rejected();
            }
            lastHeld = held;
        }

        // Sparo: i clic si prendono solo con la pistola in mano, altrimenti restano a vanilla.
        if (pistol && inWorld && !player.isUsingItem()) {
            boolean clicked = false;
            while (mc.options.keyAttack.consumeClick()) clicked = true;
            if (clicked) pull(main);
        }

        setAim(pistol && inWorld && mc.options.keyUse.isDown(), player != null);

        boolean reload = false;
        while (PistolKeys.RELOAD.consumeClick()) reload = true;
        if (reload && pistol && inWorld) send(Kind.RELOAD);

        boolean interactClicked = false;
        while (PistolKeys.INTERACT.consumeClick()) interactClicked = true;
        // Il classificatore vede sempre il tasto (anche senza oggetto utile), così i rilasci non si perdono.
        PressClassifier.Press press = INTERACT_PRESS.tick(PistolKeys.INTERACT.isDown(), interactClicked);
        if (held != 0 && inWorld) {
            switch (press) {
                case TAP -> send(Kind.INTERACT_TAP);
                case HOLD -> {
                    if (pistol) send(Kind.INTERACT_HOLD);
                }
                case NONE -> {}
            }
        }
    }

    /** Da registrare sul bus di NeoForge: con la pistola in mano "attacca" e "usa" non fanno nulla di vanilla. */
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.getMainHandItem().is(PoliceItems.PISTOL.get())) return;
        if (event.isAttack() || event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    // ------------------------------------------------------------------ dal ponte di rete

    /** Il server ha avviato un'azione per il giocatore locale. */
    static void onActionStarted(PistolAction action) {
        GATE.actionStarted(action, now);
        PistolAnimator.local().play(action);
    }

    /**
     * L'azione del giocatore locale è finita. Se annullata, si ferma la clip solo se è ancora quella dell'azione: uno
     * sparo che ha interrotto un controllo ha già avviato la sua clip, che deve restare.
     */
    static void onActionEnded(boolean cancelled) {
        PistolAction running = GATE.running(now);
        GATE.actionEnded();
        PistolAnimator animator = PistolAnimator.local();
        if (cancelled && running != null && running.clip().equals(animator.actionClip())) animator.stop();
    }

    /** Il server ha rifiutato lo sparo: via la previsione e la clip prevista, se è ancora quella in corso. */
    static void onFireRejected() {
        GATE.rejected();
        PistolAnimator animator = PistolAnimator.local();
        if (FireGate.Outcome.isPredictedClip(animator.actionClip())) animator.stop();
    }

    /**
     * Uscita dal mondo ({@code connected = false}) o rinascita/cambio di dimensione ({@code connected = true}). La
     * mira passa dal normale rilascio: l'animatore esce dalla posa di mira e, se la connessione c'è ancora, il server
     * riceve {@code AIM_OFF} (lo spegne anche da sé, ma così i due lati non dipendono l'uno dall'altro).
     */
    public static void reset(boolean connected) {
        setAim(false, connected);
        GATE.reset();
        INTERACT_PRESS.reset();
        lastHeld = 0;
    }

    // ------------------------------------------------------------------ interni

    /** Unico punto in cui cambia la mira locale: animatore e, se {@code send}, server. */
    private static void setAim(boolean aim, boolean send) {
        if (!AIM.update(aim)) return;
        PistolAnimator.local().setAiming(aim);
        if (send) send(aim ? Kind.AIM_ON : Kind.AIM_OFF);
    }

    private static void pull(ItemStack pistol) {
        WeaponState state = pistol.get(WeaponComponents.PISTOL_STATE.get());
        if (state == null) state = WeaponState.empty(NO_ID);
        FireGate.Decision decision = GATE.pull(state, now);
        if (decision.outcome() == FireGate.Outcome.BLOCKED) return;
        PistolAnimator.local().playClip(decision.outcome().clip());
        send(Kind.FIRE);
    }

    private static void send(Kind kind) {
        ClientPacketDistributor.sendToServer(new PistolPayloads.Input(kind));
    }
}
