package dev.qrm.police.weapon.client;

import dev.qrm.police.weapon.logic.PistolAction;
import dev.qrm.police.weapon.logic.PistolTimings;
import dev.qrm.police.weapon.logic.WeaponDef;
import dev.qrm.police.weapon.logic.WeaponState;
import dev.qrm.police.weapon.logic.anim.AnimClip;
import dev.qrm.police.weapon.logic.anim.AnimEvent;
import dev.qrm.police.weapon.logic.anim.AnimMixer;
import dev.qrm.police.weapon.logic.anim.BoneTransform;
import dev.qrm.police.weapon.logic.anim.RefillSequence;
import dev.qrm.police.weapon.mc.PistolSounds;
import dev.qrm.police.weapon.mc.WeaponComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Animatore dell'arma in mano del giocatore locale (uno solo, lato client): avvolge {@link AnimMixer} con le clip di
 * {@link WeaponAssets} e lo stato dell'arma che il giocatore ha in mano.
 *
 * <p>Avanza a ogni tick del client finché il giocatore impugna la pistola; quando la ripone o cambia arma torna a
 * riposo. Correre non cambia la posa dell'arma; mira e azioni le comandano gli altri task
 * del client (input, previsione dello sparo, pacchetti {@code ActionStarted}).
 *
 * <p>Eventi (regola R26): i {@code sound <id>} delle clip del giocatore locale suonano subito in locale, alla sua
 * posizione; tutti gli altri ({@code muzzle_flash}, {@code eject_shell}, {@code camera_kick}, {@code commit},
 * {@code arm}) restano in coda per {@link #drainEvents()}.
 */
public final class WeaponAnimator {

    /**
     * Spostamento di {@code root} in mira piena, in unità Blockbench, usato finché il file delle animazioni non ha
     * una clip {@code aim} (arriverà con la Parte F). Calcolato perché, con la posa vanilla della mano e il
     * {@code firstperson_righthand} del modello (traslazione 3/3,5/-6, scala 0,82), la tacca di mira (x 8, y ~11,5)
     * finisca al centro dello schermo: x = -(0,56 + 3/16) / 0,82 · 16 ≈ -14,58; y ≈ +2,38; z +6,22 (stessa profondità di prima).
     * Con {@code /pistolpose} il valore si ricalcola da {@link WeaponPose}; questo è solo il valore iniziale.
     * Provvisorio: da rifinire in gioco.
     */
    static final float[] STATIC_AIM_OFFSET = {-14.58f, 2.38f, 6.22f};

    private final RefillSequence refill = new RefillSequence();
    private static final WeaponAnimator LOCAL = new WeaponAnimator();

    private final AnimMixer mixer = new AnimMixer(PistolTimings.AIM_TICKS, STATIC_AIM_OFFSET);
    private final List<AnimEvent> later = new ArrayList<>();
    /** L'arma in mano (l'ultima, se ora non se ne impugna una): da lei vengono le clip. */
    private WeaponDef def = WeaponDef.pistol();
    private UUID heldId;
    private boolean holding;

    private WeaponAnimator() {}

    /** L'animatore del giocatore locale. */
    public static WeaponAnimator local() {
        return LOCAL;
    }

    /** Da registrare sul bus di NeoForge, lato client. */
    public static void onClientTick(ClientTickEvent.Post event) {
        LOCAL.tick();
    }

    // ------------------------------------------------------------------ comandi per gli altri task

    /** Avvia la clip dell'azione temporizzata. */
    public void play(PistolAction action) {
        playClip(action.clip());
    }

    /** Avvia una clip per nome (per esempio {@code fire}, {@code fire_last}, {@code dry_fire}). */
    public void playClip(String clip) {
        mixer.play(clip, clips());
        dispatch();
    }

    /**
     * Proiettili spostati in un caricatore dell'inventario: avvia o prolunga la sequenza di riempimento
     * ({@link RefillSequence}: preparazione, un giro per colpo, chiusura). Parte solo con l'arma in mano e se non c'è
     * un'altra azione. Non dipende dalla GUI: l'animatore avanza a ogni tick del client, con o senza schermate
     * aperte, e nessuna apertura o chiusura dell'inventario lo azzera.
     *
     */
    public void playRefill(int rounds) {
        if (!holding) return;
        if (!refill.active() && mixer.actionClip() != null) return;
        String clip = refill.request(rounds, false);
        if (clip == null) return;
        if (!clips().containsKey(clip)) {
            refill.cancel();
            return;
        }
        playClip(clip);
    }

    /** Vero se la sequenza di riempimento è in corso. */
    public boolean refillActive() {
        return refill.active();
    }

    /** Il server ha finito o interrotto il riempimento: si chiude dopo il giro in corso. */
    public void stopRefill() {
        refill.finish();
    }

    private void tickRefill() {
        if (!refill.active()) return;
        String current = mixer.actionClip();
        AnimClip clip = current == null ? null : clips().get(current);
        switch (refill.tick(current, mixer.actionSeconds(0f), clip == null ? 0.0 : clip.length())) {
            case PLAY_LOOP -> playClip(RefillSequence.LOOP);
            case PLAY_OUT -> playClip(RefillSequence.OUT);
            case STOP -> mixer.stop();
            case NONE -> {}
        }
    }

    public void stop() {
        mixer.stop();
    }

    public void setAiming(boolean aiming) {
        mixer.setAiming(aiming);
    }

    /** Torna a riposo e scarta gli eventi in coda (cambio d'arma, uscita dal mondo). */
    public void reset() {
        refill.cancel();
        mixer.reset();
        later.clear();
    }

    /** Posa dei bone all'istante {@code tick + partialTick}, con lo stato dell'arma in mano sovrapposto. */
    public Map<String, BoneTransform> pose(float partialTick) {
        WeaponPose.select(def);
        mixer.setStaticAimOffset(WeaponPose.aimOffset());
        return mixer.pose(clips(), partialTick, heldState());
    }

    /** Eventi non sonori raccolti dall'ultima chiamata, in ordine. */
    public List<AnimEvent> drainEvents() {
        if (later.isEmpty()) return List.of();
        List<AnimEvent> out = List.copyOf(later);
        later.clear();
        return out;
    }

    /** Clip d'azione in corso, o {@code null}. */
    /** Secondi dall'inizio dell'azione corrente (0 se non ce n'è una). */
    public double actionSeconds(float partialTick) {
        return mixer.actionSeconds(partialTick);
    }

    public String actionClip() {
        return mixer.actionClip();
    }

    // ------------------------------------------------------------------ interni

    private void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        ItemStack stack = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        Optional<WeaponDef> held = HeldWeapon.of(stack);
        if (held.isEmpty()) {
            if (holding) reset();
            holding = false;
            heldId = null;
            return;
        }
        WeaponState state = stack.get(WeaponComponents.PISTOL_STATE.get());
        UUID id = state == null ? null : state.id();
        if (holding && ((heldId != null && id != null && !heldId.equals(id)) || def != held.get())) reset();
        def = held.get();
        holding = true;
        heldId = id;

        mixer.tick(clips());
        tickRefill();
        dispatch();
    }

    private void dispatch() {
        for (AnimEvent e : mixer.drainEvents()) {
            if ("sound".equals(e.type())) {
                if (!e.args().isEmpty()) playLocal(e.args().get(0));
            } else {
                later.add(e);
            }
        }
    }

    /** Suona in locale, alla posizione del giocatore locale, il suono della pistola con quell'id (se esiste). */
    static void playLocal(String id) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) return;
        PistolSounds.byId(id).ifPresent(sound -> level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                sound.value(), SoundSource.PLAYERS, 1f, 1f, false));
    }

    private Map<String, AnimClip> clips() {
        return WeaponAssets.clips(def);
    }

    private static AnimMixer.State heldState() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return AnimMixer.State.NONE;
        ItemStack stack = player.getMainHandItem();
        WeaponState state = HeldWeapon.of(stack).isPresent() ? stack.get(WeaponComponents.PISTOL_STATE.get()) : null;
        if (state == null) return AnimMixer.State.NONE;
        return new AnimMixer.State(state.slideLocked(), state.magazine().isPresent());
    }
}
