package dev.qrm.police.weapon.client;

import dev.qrm.police.mc.PoliceItems;
import dev.qrm.police.weapon.logic.PistolAction;
import dev.qrm.police.weapon.logic.PistolTimings;
import dev.qrm.police.weapon.logic.WeaponState;
import dev.qrm.police.weapon.logic.anim.AnimEvent;
import dev.qrm.police.weapon.logic.anim.AnimMixer;
import dev.qrm.police.weapon.logic.anim.BoneTransform;
import dev.qrm.police.weapon.mc.PistolSounds;
import dev.qrm.police.weapon.mc.WeaponComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Animatore della pistola del giocatore locale (uno solo, lato client): avvolge {@link AnimMixer} con le clip di
 * {@link PistolAssets} e lo stato dell'arma che il giocatore ha in mano.
 *
 * <p>Avanza a ogni tick del client finché il giocatore impugna la pistola; quando la ripone o cambia arma torna a
 * riposo. La corsa segue {@code isSprinting()} del giocatore a ogni tick; mira e azioni le comandano gli altri task
 * del client (input, previsione dello sparo, pacchetti {@code ActionStarted}).
 *
 * <p>Eventi (regola R26): i {@code sound <id>} delle clip del giocatore locale suonano subito in locale, alla sua
 * posizione; tutti gli altri ({@code muzzle_flash}, {@code eject_shell}, {@code camera_kick}, {@code commit},
 * {@code arm}) restano in coda per {@link #drainEvents()}.
 */
public final class PistolAnimator {

    /**
     * Spostamento di {@code root} in mira piena, in unità Blockbench, usato finché il file delle animazioni non ha
     * una clip {@code aim} (arriverà con la Parte F). Calcolato perché, con la posa vanilla della mano e il
     * {@code firstperson_righthand} del modello (traslazione 1,5/5/-3, scala 0,7), la tacca di mira (x 8, y ~11,5)
     * finisca al centro dello schermo: x = -(0,56 + 1,5/16) / 0,7 · 16 ≈ -14,9; y ≈ +1,25; z +3 per avvicinarla.
     * Provvisorio: da rifinire in gioco.
     */
    static final float[] STATIC_AIM_OFFSET = {-14.9f, 1.25f, 3f};

    private static final PistolAnimator LOCAL = new PistolAnimator();

    private final AnimMixer mixer = new AnimMixer(PistolTimings.AIM_TICKS, STATIC_AIM_OFFSET);
    private final List<AnimEvent> later = new ArrayList<>();
    private UUID heldId;
    private boolean holding;

    private PistolAnimator() {}

    /** L'animatore del giocatore locale. */
    public static PistolAnimator local() {
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
        mixer.play(clip, PistolAssets.clips());
        dispatch();
    }

    public void stop() {
        mixer.stop();
    }

    public void setAiming(boolean aiming) {
        mixer.setAiming(aiming);
    }

    public void setSprinting(boolean sprinting) {
        mixer.setSprinting(sprinting);
    }

    /** Torna a riposo e scarta gli eventi in coda (cambio d'arma, uscita dal mondo). */
    public void reset() {
        mixer.reset();
        later.clear();
    }

    /** Posa dei bone all'istante {@code tick + partialTick}, con lo stato dell'arma in mano sovrapposto. */
    public Map<String, BoneTransform> pose(float partialTick) {
        return mixer.pose(PistolAssets.clips(), partialTick, heldState());
    }

    /** Eventi non sonori raccolti dall'ultima chiamata, in ordine. */
    public List<AnimEvent> drainEvents() {
        if (later.isEmpty()) return List.of();
        List<AnimEvent> out = List.copyOf(later);
        later.clear();
        return out;
    }

    /** Clip d'azione in corso, o {@code null}. */
    public String actionClip() {
        return mixer.actionClip();
    }

    // ------------------------------------------------------------------ interni

    private void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        ItemStack stack = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        if (!stack.is(PoliceItems.PISTOL.get())) {
            if (holding) reset();
            holding = false;
            heldId = null;
            return;
        }
        WeaponState state = stack.get(WeaponComponents.PISTOL_STATE.get());
        UUID id = state == null ? null : state.id();
        if (holding && heldId != null && id != null && !heldId.equals(id)) reset();
        holding = true;
        heldId = id;

        mixer.setSprinting(player.isSprinting());
        mixer.tick(PistolAssets.clips());
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

    private static AnimMixer.State heldState() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return AnimMixer.State.NONE;
        ItemStack stack = player.getMainHandItem();
        WeaponState state = stack.is(PoliceItems.PISTOL.get()) ? stack.get(WeaponComponents.PISTOL_STATE.get()) : null;
        if (state == null) return AnimMixer.State.NONE;
        return new AnimMixer.State(state.slideLocked(), state.magazine().isPresent());
    }
}
