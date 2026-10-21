package dev.qrm.police.weapon.client;

import dev.qrm.police.weapon.net.PistolPayloads;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Ponte dal canale di rete al client della pistola: caricata solo quando arriva davvero un pacchetto o dal codice
 * client (mai sul server dedicato). I gestori girano sul thread principale del client.
 *
 * <p>Per il giocatore locale avvia e ferma le clip; per gli altri ricorda solo chi sta mirando (per la posa in terza
 * persona). Colpi e identità ({@link #onShot}, {@link #onIdentity}) li gestiscono gli effetti e l'HUD.
 */
public final class PistolClientBridge {
    /** Id delle entità (altri giocatori) che stanno mirando; si svuota a ogni uscita o cambio di livello. */
    private static final Set<Integer> REMOTE_AIM = new HashSet<>();

    private PistolClientBridge() {}

    public static void onActionStarted(PistolPayloads.ActionStarted p) {
        // Le clip di sparo non sono azioni temporizzate: la previsione le ha già avviate, il server non le annuncia.
        if (isLocal(p.entityId())) PistolInput.onActionStarted(p.action());
    }

    public static void onActionEnded(PistolPayloads.ActionEnded p) {
        if (isLocal(p.entityId())) PistolInput.onActionEnded(p.cancelled());
    }

    public static void onShot(PistolPayloads.Shot p) {}

    public static void onAim(PistolPayloads.Aim p) {
        if (isLocal(p.entityId())) return;
        if (p.aiming()) REMOTE_AIM.add(p.entityId());
        else REMOTE_AIM.remove(p.entityId());
    }

    public static void onRejected(PistolPayloads.Rejected p) {
        if (p.kind() == PistolPayloads.Input.Kind.FIRE) PistolInput.onFireRejected();
    }

    public static void onIdentity(PistolPayloads.Identity p) {}

    /** Vero se l'entità sta mirando con la pistola: il flag locale per sé, l'ultimo {@code Aim} per gli altri. */
    public static boolean isAiming(LivingEntity entity) {
        if (entity == Minecraft.getInstance().player) return PistolInput.isLocalAiming();
        return REMOTE_AIM.contains(entity.getId());
    }

    /** Dimentica la mira degli altri (uscita dal mondo, cambio di livello: gli id delle entità cambiano). */
    public static void clear() {
        REMOTE_AIM.clear();
    }

    private static boolean isLocal(int entityId) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.getId() == entityId;
    }
}
